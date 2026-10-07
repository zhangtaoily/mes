package com.xinao.mes.engine.code;

import com.xinao.mes.engine.code.entity.SysAutoCode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 编码格式化纯函数（便于单测）
 */
public final class CodeFormatter {

    private CodeFormatter() {
    }

    /**
     * 计算当前流水期间
     *
     * @param resetType 1不重置（期间恒空） 2按日
     * @return 期间串，如 20260930；不重置时返回空串
     */
    public static String periodOf(String dateFormat, Integer resetType) {
        if (resetType == null || resetType == 1) {
            return "";
        }
        String pattern = (dateFormat == null || dateFormat.isBlank()) ? "yyyyMMdd" : dateFormat;
        return LocalDate.now().format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 拼装完整单号：前缀 + 期间 + 补零流水（流水超位数时不截断，避免重号）
     */
    public static String format(SysAutoCode rule, String period, long serial) {
        int len = (rule.getSerialLen() == null || rule.getSerialLen() <= 0) ? 6 : rule.getSerialLen();
        String serialStr = String.format("%0" + len + "d", serial);
        return (rule.getPrefix() == null ? "" : rule.getPrefix()) + period + serialStr;
    }
}
