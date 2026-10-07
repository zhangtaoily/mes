package com.xinao.mes.engine.code;

import com.xinao.mes.engine.code.entity.SysAutoCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 编码格式化纯函数
 */
class CodeFormatterTest {

    @Test
    void periodNoReset() {
        assertEquals("", CodeFormatter.periodOf("yyyyMMdd", 1));
        assertEquals("", CodeFormatter.periodOf("yyyyMMdd", null));
    }

    @Test
    void periodDaily() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertEquals(today, CodeFormatter.periodOf("yyyyMMdd", 2));
    }

    @Test
    void formatPadsSerial() {
        SysAutoCode rule = new SysAutoCode();
        rule.setPrefix("PI");
        rule.setSerialLen(6);
        assertEquals("PI20260930000001", CodeFormatter.format(rule, "20260930", 1));
        assertEquals("PI20260930012345", CodeFormatter.format(rule, "20260930", 12345));
    }

    @Test
    void serialOverflowKeepsFullDigits() {
        // 流水超出位数不截断，避免重号
        SysAutoCode rule = new SysAutoCode();
        rule.setPrefix("QC");
        rule.setSerialLen(5);
        assertEquals("QC20260930123456", CodeFormatter.format(rule, "20260930", 123456));
    }

    @Test
    void noPeriodNoReset() {
        SysAutoCode rule = new SysAutoCode();
        rule.setPrefix("SR");
        rule.setSerialLen(4);
        assertEquals("SR0001", CodeFormatter.format(rule, "", 1));
    }
}
