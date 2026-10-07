package com.xinao.mes.engine.script;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 公式执行结果
 */
@Data
public class FormulaResult {

    /** 执行是否成功 */
    private boolean success;

    /** 脚本返回值 */
    private Object value;

    /** $SET 写回的衍生指标：key → value */
    private Map<String, Object> derived = new LinkedHashMap<>();

    /** 错误信息（含行号提示） */
    private String error;

    /** 出错行号 */
    private Integer errorLine;

    /** 是否执行超时（死循环被杀） */
    private boolean timeout;

    public static FormulaResult ok(Object value, Map<String, Object> derived) {
        FormulaResult r = new FormulaResult();
        r.setSuccess(true);
        r.setValue(value);
        if (derived != null) {
            r.setDerived(derived);
        }
        return r;
    }

    public static FormulaResult fail(String error, Integer errorLine, boolean timeout) {
        FormulaResult r = new FormulaResult();
        r.setSuccess(false);
        r.setError(error);
        r.setErrorLine(errorLine);
        r.setTimeout(timeout);
        return r;
    }
}
