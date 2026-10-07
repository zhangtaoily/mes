package com.xinao.mes.pack.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 逐箱称重入参（五列重量：公重公式换算，恒等式校验毛=皮+净） */
@Data
public class BoxSaveDTO {

    private Long executeId;
    /** 颗数 */
    private Integer grainCount;
    private BigDecimal netWeight;
    private BigDecimal grossWeight;
    private BigDecimal tareWeight;
    private String packSpec;
    /** 扎绞/线颜色 */
    private String tieLineColor;
    /** 自动线/手动线 */
    private String lineType;
    /** 一体机编码 */
    private String machineCode;
    /** 上机/装箱人员 */
    private String packUser;
    /** 实际筒数 */
    private Integer actualTube;
}
