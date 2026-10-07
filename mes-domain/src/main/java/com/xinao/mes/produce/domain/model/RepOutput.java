package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 产量汇报（逐班报产；前纺/复精梳变体同构） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rep_output")
public class RepOutput extends BaseEntity {

    /** 编码:OR26092901064 */
    private String outputNo;
    /** 排程号 */
    private Long scheduleId;
    /** 生产任务ID */
    private Long taskId;
    /** 班组 */
    private Long teamId;
    /** 汇报人员 */
    private String reportUser;
    /** 生产人员(多选) */
    private String prodUsers;
    /** 批号 */
    private String batchNo;
    /** 工序 */
    private Long processId;
    /** 产品名称 */
    private String productName;
    /** 机台 */
    private Long equipmentId;
    /** 汇报产量 */
    private BigDecimal outputQty;
    /** 返工数量 */
    private BigDecimal reworkQty;
    /** 回毛数量 */
    private BigDecimal returnWoolQty;
    /** 正常回毛上报比例 */
    private BigDecimal returnWoolRate;
    /** 毛料数量 */
    private BigDecimal woolQty;
    /** 袋子颜色 */
    private String bagColor;
    /** 筒管颜色:网蓝 */
    private String tubeColor;
    /** 报工类型 */
    private String reportType;
    /** 单位 */
    private String unit;
    /** 检验产量 */
    private BigDecimal checkQty;
    /** 报产日期 */
    private LocalDate reportDate;
    /** 是否入库（0否/1是） */
    private Integer isWarehouse;
    /** 备注:倒毛自动报程 */
    private String remark;
}
