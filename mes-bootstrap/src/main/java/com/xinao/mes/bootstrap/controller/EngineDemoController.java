package com.xinao.mes.bootstrap.controller;

import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.auth.LoginUser;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.R;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.event.DomainEvent;
import com.xinao.mes.engine.event.MesEventPublisher;
import com.xinao.mes.engine.flow.FlowEngine;
import com.xinao.mes.engine.flow.entity.FlowTransition;
import com.xinao.mes.engine.meta.MetaEngine;
import com.xinao.mes.engine.meta.vo.TableVO;
import com.xinao.mes.engine.script.FormulaResult;
import com.xinao.mes.engine.script.FormulaService;
import com.xinao.mes.engine.script.entity.QcFormula;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * M1 引擎验收 demo 接口（需登录；M2 起正式业务接口挂按钮级权限）
 *
 * <p>code:  取号 / 100线程并发无重号
 * <p>flow:  按钮下发 / 迁移校验 / 加迁移配置不改代码
 * <p>meta:  动态列 / 指标纵表增删→列表自动多列
 * <p>script: 公式执行 / 死循环 500ms 被杀 / 编辑即生效
 * <p>event: 领域事件总线
 */
@Slf4j
@RestController
@RequestMapping("/api/engine")
@RequiredArgsConstructor
public class EngineDemoController {

    private final CodeEngine codeEngine;
    private final FlowEngine flowEngine;
    private final MetaEngine metaEngine;
    private final FormulaService formulaService;
    private final com.xinao.mes.engine.script.FormulaEngine formulaEngine;
    private final MesEventPublisher eventPublisher;

    // ==================== code 编码引擎 ====================

    /** 取一个号：GET/POST /api/engine/code/next/计划工艺单 */
    @GetMapping("/code/next/{codeName}")
    public R<String> nextCode(@PathVariable String codeName, @RequestParam(required = false) String bizRef) {
        return R.ok(codeEngine.next(codeName, bizRef));
    }

    /** 并发取号验收：threads 线程各取 loops 个号，duplicates 应为 0 */
    @GetMapping("/code/concurrent/{codeName}")
    public R<ConcurrentResult> concurrentNext(@PathVariable String codeName,
                                              @RequestParam(defaultValue = "100") int threads,
                                              @RequestParam(defaultValue = "1") int loops) throws Exception {
        Set<String> all = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        for (int j = 0; j < loops; j++) {
                            all.add(codeEngine.next(codeName, null));
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            if (!latch.await(60, TimeUnit.SECONDS)) {
                return R.fail("并发取号超时");
            }
            ConcurrentResult r = new ConcurrentResult();
            r.expected = threads * loops;
            r.unique = all.size();
            r.duplicates = r.expected - r.unique;
            r.sample = all.stream().sorted().limit(3).toList();
            return R.ok(r);
        } finally {
            pool.shutdownNow();
        }
    }

    @Data
    public static class ConcurrentResult {
        public int expected;
        public int unique;
        public int duplicates;
        public List<String> sample;
    }

    // ==================== flow 状态机 ====================

    /** 当前状态+登录用户角色 → 可见按钮（前端据此渲染） */
    @GetMapping("/flow/buttons")
    public R<List<com.xinao.mes.engine.flow.vo.ButtonVO>> buttons(
            @RequestParam String billType, @RequestParam String status) {
        return R.ok(flowEngine.buttons(billType, status, currentRoles()));
    }

    /** 迁移校验执行：返回目标状态 */
    @PostMapping("/flow/fire")
    public R<String> fire(@RequestBody FireDTO dto) {
        return R.ok(flowEngine.fire(dto.billType, dto.fromStatus, dto.action, currentRoles()));
    }

    /** 加一条迁移配置（即时生效，不改代码） */
    @PostMapping("/flow/transitions")
    @OperLog(title = "新增状态迁移配置")
    public R<Void> addTransition(@RequestBody FlowTransition transition) {
        flowEngine.addTransition(transition);
        return R.ok();
    }

    /** 手动重载 flow 配置（直改 DB 后调用） */
    @PostMapping("/flow/refresh")
    public R<String> refresh() {
        flowEngine.refresh();
        return R.ok("reloaded");
    }

    @Data
    public static class FireDTO {
        public String billType;
        public String fromStatus;
        public String action;
    }

    private Set<String> currentRoles() {
        LoginUser user = AuthContext.get();
        return user == null ? Set.of() : user.getRoles();
    }

    // ==================== meta 元数据 ====================

    /** 表节点动态列（质检指标纵表增删 → 列表自动多列/少列） */
    @GetMapping("/meta/columns/{nodeCode}")
    public R<TableVO> columns(@PathVariable String nodeCode) {
        return R.ok(metaEngine.getTable(nodeCode));
    }

    /** 新增质检指标（演示动态多列） */
    @PostMapping("/meta/indicators")
    @OperLog(title = "新增质检指标")
    public R<Long> addIndicator(@RequestBody IndicatorDTO dto) {
        return R.ok(metaEngine.addIndicator(dto.indicatorCode, dto.indicatorName, dto.unit).getId());
    }

    /** 删除质检指标（演示动态少列） */
    @DeleteMapping("/meta/indicators/{id}")
    @OperLog(title = "删除质检指标")
    public R<Void> removeIndicator(@PathVariable Long id) {
        metaEngine.removeIndicator(id);
        return R.ok();
    }

    @Data
    public static class IndicatorDTO {
        public String indicatorCode;
        public String indicatorName;
        public String unit;
    }

    // ==================== script 脚本引擎 ====================

    /** 按公式ID执行（$BQ 取值 → 计算 → $SET 写回衍生指标） */
    @PostMapping("/script/eval")
    public R<FormulaResult> eval(@RequestBody EvalDTO dto) {
        return R.ok(formulaService.evalById(dto.formulaId, dto.values));
    }

    /** 直接执行脚本（死循环 500ms 被杀验收） */
    @PostMapping("/script/eval-raw")
    public R<FormulaResult> evalRaw(@RequestBody EvalRawDTO dto) {
        return R.ok(formulaEngine.eval(dto.script,
                dto.values == null ? Map.of() : dto.values,
                dto.timeoutMs == null ? 500 : dto.timeoutMs));
    }

    /** 查看公式脚本 */
    @GetMapping("/script/formulas/{id}")
    public R<QcFormula> formula(@PathVariable Long id) {
        return R.ok(formulaService.getById(id));
    }

    /** 编辑公式脚本（编辑即生效） */
    @PutMapping("/script/formulas/{id}/script")
    @OperLog(title = "编辑质检公式")
    public R<QcFormula> updateScript(@PathVariable Long id, @RequestBody EvalRawDTO dto) {
        return R.ok(formulaService.updateScript(id, dto.script));
    }

    @Data
    public static class EvalDTO {
        public Long formulaId;
        public Map<Object, Object> values;
    }

    @Data
    public static class EvalRawDTO {
        public String script;
        public Map<Object, Object> values;
        public Long timeoutMs;
    }

    // ==================== event 领域事件总线 ====================

    /** 发布 demo 事件，同步监听器回执 */
    @PostMapping("/event/publish")
    public R<String> publishEvent(@RequestBody(required = false) Map<String, Object> payload) {
        DemoEvent event = new DemoEvent(
                payload == null ? "demo" : String.valueOf(payload.getOrDefault("msg", "demo")));
        eventPublisher.publish(event);
        return R.ok(event.handledBy == null ? "no-listener" : event.handledBy);
    }

    /** demo 事件（M2 起替换为真实领域事件，如 BatchFinishedEvent） */
    public static class DemoEvent implements DomainEvent {
        public final String msg;
        public volatile String handledBy;
        private final LocalDateTime occurredOn = LocalDateTime.now();

        DemoEvent(String msg) {
            this.msg = msg;
        }

        @Override
        public LocalDateTime occurredOn() {
            return occurredOn;
        }
    }

    @org.springframework.stereotype.Component
    public static class DemoEventListener {
        @EventListener
        public void onDemo(DemoEvent event) {
            event.handledBy = "DemoEventListener@" + event.occurredOn();
            log.info("[领域事件] DemoEvent msg={} 已处理", event.msg);
        }
    }
}
