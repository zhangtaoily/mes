package com.xinao.mes.engine.script;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * GraalVM JS 沙箱实现
 *
 * <p>安全约束：禁 IO / 禁线程 / 禁进程 / 禁本地方法 / 禁 Java 类访问（HostClassLookup 全拒），
 * 宿主仅注入 $BQ/$SET 两个可执行代理（ProxyExecutable）；
 * 超时用 watchdog 线程 + Context.interrupt —— 死循环脚本到时被杀并报出行号。
 */
@Service
public class GraalJsFormulaEngine implements FormulaEngine {

    private static final long DEFAULT_TIMEOUT_MS = 500;

    /** 共享 Engine（复用解析器，Context 每次新建保证隔离） */
    private Engine sharedEngine;

    /** 看门狗：到时中断正在执行的脚本 Context */
    private ScheduledExecutorService watchdog;

    @PostConstruct
    public void init() {
        sharedEngine = Engine.newBuilder("js")
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        watchdog = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "script-watchdog");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    public void destroy() {
        if (watchdog != null) {
            watchdog.shutdownNow();
        }
        if (sharedEngine != null) {
            sharedEngine.close();
        }
    }

    @Override
    public FormulaResult eval(String script, Map<Object, Object> values) {
        return eval(script, values, DEFAULT_TIMEOUT_MS);
    }

    @Override
    public FormulaResult eval(String script, Map<Object, Object> values, long timeoutMs) {
        ScriptBridge bridge = new ScriptBridge(values);
        try (Context ctx = Context.newBuilder("js")
                .engine(sharedEngine)
                .allowHostAccess(HostAccess.EXPLICIT)
                .allowHostClassLookup(clazz -> false)
                .allowIO(IOAccess.NONE)
                .allowCreateProcess(false)
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .build()) {

            Value bindings = ctx.getBindings("js");
            bindings.putMember("$BQ", (ProxyExecutable) args -> {
                Object id = args.length > 0 ? args[0].as(Object.class) : null;
                return bridge.lookupValue(id);
            });
            bindings.putMember("$SET", (ProxyExecutable) args -> {
                String key = args.length > 0 ? args[0].asString() : null;
                Object value = args.length > 1 ? args[1].as(Object.class) : null;
                return bridge.derived.put(key, value);
            });

            // 看门狗：到时强制中断脚本执行（死循环被杀）
            ScheduledFuture<?> killer = watchdog.schedule(() -> {
                try {
                    ctx.interrupt(Duration.ofSeconds(2));
                } catch (Throwable ignore) {
                    // context 已关闭等情况
                }
            }, timeoutMs, TimeUnit.MILLISECONDS);
            try {
                Value result = ctx.eval("js", script);
                Object value = result == null ? null : result.as(Object.class);
                return FormulaResult.ok(value, bridge.derived);
            } finally {
                killer.cancel(false);
            }
        } catch (PolyglotException e) {
            Integer line = null;
            if (e.getSourceLocation() != null && e.getSourceLocation().getStartLine() > 0) {
                line = e.getSourceLocation().getStartLine();
            }
            String msg = e.getMessage() == null ? "" : e.getMessage();
            // watchdog 中断：isInterrupted / isCancelled
            if (e.isInterrupted() || e.isCancelled()) {
                return FormulaResult.fail(
                        "脚本执行超时(>" + timeoutMs + "ms)，已被强制终止，疑似死循环，行号:" + line,
                        line, true);
            }
            String detail = e.isGuestException() ? msg : "脚本执行异常: " + msg;
            return FormulaResult.fail(detail + " (行号:" + line + ")", line, false);
        } catch (Exception e) {
            return FormulaResult.fail("引擎异常: " + e.getMessage(), null, false);
        }
    }

    /**
     * 桥接数据：$BQ 按指标ID取质检值（兼容 number/string key），$SET 写回衍生指标。
     * 宿主值必须 JS 友好化：
     * <ul>
     *   <li>BigDecimal/BigInteger → double —— GraalJS 对宿主对象 ToNumber 得 NaN</li>
     *   <li>List → ProxyArray —— HostAccess.EXPLICIT 下宿主 List 无任何导出成员（a.length 为 undefined），
     *       ProxyArray 才支持 a[i]/a.length</li>
     * </ul>
     */
    static final class ScriptBridge {

        private final Map<Object, Object> values;
        final Map<String, Object> derived = new java.util.LinkedHashMap<>();

        ScriptBridge(Map<Object, Object> values) {
            this.values = values == null ? Map.of() : values;
        }

        Object lookupValue(Object id) {
            if (id == null) {
                return null;
            }
            return toJsFriendly(rawLookup(id));
        }

        private static Object toJsFriendly(Object v) {
            if (v instanceof BigDecimal d) {
                return d.doubleValue();
            }
            if (v instanceof BigInteger i) {
                return i.doubleValue();
            }
            if (v instanceof List<?> list) {
                return new ProxyArray() {
                    @Override
                    public Object get(long index) {
                        return toJsFriendly(list.get(Math.toIntExact(index)));
                    }

                    @Override
                    public void set(long index, Value value) {
                        throw new UnsupportedOperationException("质检值集合只读");
                    }

                    @Override
                    public long getSize() {
                        return list.size();
                    }
                };
            }
            return v;
        }

        private Object rawLookup(Object id) {
            if (values.containsKey(id)) {
                return values.get(id);
            }
            // key 字符串化 / 数字化兜底（JSON 入参 key 为 String，脚本传 number）
            if (id instanceof Number n) {
                Object v = values.get(n.longValue());
                if (v != null) {
                    return v;
                }
                return values.get(String.valueOf(n.longValue()));
            }
            try {
                return values.get(Long.parseLong(String.valueOf(id)));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
