package com.xinao.mes.plugin.scale;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 秤重读数 */
@Data
public class WeightReading {

    /** 重量 */
    private BigDecimal weight;
    /** 单位 */
    private String unit;
    /** 读数是否稳定 */
    private Boolean stable;
    /** 秤设备标识 */
    private String device;
    /** 读取时间 */
    private LocalDateTime readAt;
}
