package com.xinao.mes.engine.script;

import java.util.Map;

/**
 * 公式脚本引擎接口（质检/报产公式外置）
 */
public interface FormulaEngine {

    /**
     * 沙箱执行 JS 脚本
     *
     * @param script   脚本内容，可用注入函数：$BQ(指标ID) 取质检值、$SET(key, val) 写回衍生指标
     * @param values   指标值输入：指标ID → 检验值（字符串/数值均可）
     * @return 执行结果（含 $SET 写回的衍生值；失败时含行号与错误信息）
     */
    FormulaResult eval(String script, Map<Object, Object> values);

    /**
     * 带超时参数的执行（默认 500ms）
     */
    FormulaResult eval(String script, Map<Object, Object> values, long timeoutMs);
}
