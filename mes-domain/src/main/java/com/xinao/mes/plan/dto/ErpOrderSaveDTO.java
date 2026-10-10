package com.xinao.mes.plan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** K3订单手工录入/编辑入参（M5 插件接入前过渡方案；orderNo 留空自动取号 SO） */
@Data
public class ErpOrderSaveDTO {

    /** 订单号（留空自动取号SO；编辑时不可改） */
    private String orderNo;

    /** 生产批号 */
    @NotBlank(message = "生产批号不能为空")
    private String batchNo;

    /** 订单数量 */
    @NotNull(message = "订单数量不能为空")
    @DecimalMin(value = "0.01", message = "订单数量必须大于0")
    private BigDecimal k3Qty;

    /** 单位（缺省KG） */
    private String unit;

    /** 订单日期（缺省今天） */
    private LocalDate orderDate;

    /** 计划开始日期 */
    private LocalDate planStartDate;

    /** 计划结束日期（交期） */
    private LocalDate planEndDate;

    /** 车间名称 */
    private String workshopName;

    /** 品名 */
    private String productName;

    /** 色号 */
    private String colorNo;

    /** 纺纱支数 */
    private String spinCount;

    /** 物料明细 */
    private String materialDetail;

    /** 物料规格 */
    private String materialSpec;

    /** 原料 */
    private String rawMaterial;

    /** 客户名称 */
    private String customerName;

    /** 染色类别:条染/绞纱/胚纱 */
    private String dyeType;
}
