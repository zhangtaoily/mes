package com.xinao.mes.engine.script;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GraalVM JS 沙箱测试：$BQ/$SET 注入、死循环超时杀、禁宿主访问
 */
class GraalJsFormulaEngineTest {

    private static GraalJsFormulaEngine engine;

    @BeforeAll
    static void setUp() {
        engine = new GraalJsFormulaEngine();
        engine.init();
    }

    @AfterAll
    static void tearDown() {
        engine.destroy();
    }

    @Test
    void evalFormulaWithBqAndSet() {
        Map<Object, Object> values = new HashMap<>();
        values.put(1L, "10");
        values.put(2L, "20");

        String script = """
                var calculate = function() {
                    var a = Number($BQ(1));
                    var b = Number($BQ(2));
                    var sum = a + b;
                    $SET("SUM", sum);
                    return sum;
                };
                calculate();
                """;

        FormulaResult result = engine.eval(script, values);
        assertTrue(result.isSuccess());
        assertEquals(30.0, ((Number) result.getValue()).doubleValue());
        assertEquals(30.0, ((Number) result.getDerived().get("SUM")).doubleValue());
    }

    @Test
    void bqAcceptsStringKeyFromJson() {
        // JSON 反序列化后 key 是字符串，$BQ(1) 数字入参也要能命中
        Map<Object, Object> values = new HashMap<>();
        values.put("1", "10");

        FormulaResult result = engine.eval("Number($BQ(1)) + 5", values);
        assertTrue(result.isSuccess());
        assertEquals(15, ((Number) result.getValue()).intValue());
    }

    @Test
    void infiniteLoopKilledByTimeoutWithLineNumber() {
        String script = """
                var calculate = function() {
                    while (true) { }
                };
                calculate();
                """;
        FormulaResult result = engine.eval(script, Map.of(), 500);
        assertFalse(result.isSuccess());
        assertTrue(result.isTimeout());
        assertNotNull(result.getErrorLine());
        assertTrue(result.getError().contains("超时"), "错误信息应提示超时: " + result.getError());
    }

    @Test
    void scriptErrorReportsLineNumber() {
        FormulaResult result = engine.eval("var a = undefinedFn();\na.b.c;", Map.of());
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorLine());
        assertEquals(1, result.getErrorLine());
    }

    @Test
    void hostAccessBlocked() {
        // 禁 Java 类访问：Java.type 不可用
        FormulaResult result = engine.eval(
                "var Runtime = Java.type('java.lang.Runtime'); Runtime.getRuntime();", Map.of());
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
    }
}
