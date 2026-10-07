package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** K3 订单（同步只读镜像，引入量回写 imported_qty） */
@Data
@TableName("erp_k3_order")
public class ErpK3Order {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单类型(调入) */
    private String orderType;
    /** 订单号:FBDC772348 */
    private String orderNo;
    /** 订单日期 */
    private LocalDate orderDate;
    /** 车间名称 */
    private String workshopName;
    /** 生产线 */
    private String prodLine;
    /** 批号:26091340 */
    private String batchNo;
    /** K3数量 */
    private BigDecimal k3Qty;
    /** 合并投料量 */
    private BigDecimal mergeFeedQty;
    /** 计划开始日期 */
    private LocalDate planStartDate;
    /** 计划结束日期 */
    private LocalDate planEndDate;
    /** 色号 */
    private String colorNo;
    /** 品名 */
    private String productName;
    /** 纺纱支数 */
    private String spinCount;
    /** 支数 */
    private String countNum;
    /** 股数 */
    private String plyNum;
    /** 折标率 */
    private BigDecimal convertRate;
    /** 已引入数量 */
    private BigDecimal importedQty;
    /** 染色类别:条染/绞纱/胚纱 */
    private String dyeType;
    /** 物料明细 */
    private String materialDetail;
    /** 物料号:4.1.1.001 */
    private String materialNo;
    /** 物料名称 */
    private String materialName;
    /** 物料规格 */
    private String materialSpec;
    /** 单位 */
    private String unit;
    /** 英文品名 */
    private String nameEn;
    /** 中文品名 */
    private String nameCn;
    /** 打印编号 */
    private String printCode;
    /** 公司色号 */
    private String companyColor;
    /** 纺纱下达日期 */
    private LocalDate issueDate;
    /** 客户名称 */
    private String customerName;
    /** 销售部门 */
    private String saleDept;
    /** 销售员 */
    private String saleUser;
    /** 用途:横机/梭织 */
    private String purpose;
    /** 纺纱用精梳批号 */
    private String combedBatchNo;
    /** 复精梳批号 */
    private String recombedBatchNo;
    /** 原料批号 */
    private String rawBatchNo;
    /** 原料样板编号(OCR不确定):001.030 */
    private String rawSampleNo;
    /** 原料 */
    private String rawMaterial;
    /** 客户色号 */
    private String customerColor;
    /** 缸号 */
    private String dyeVatNo;
    /** 打印缸号 */
    private String printVatNo;
    /** 加工单位 */
    private String processUnit;
    /** 工序名称 */
    private String processName;
    /** 版本名称 */
    private String versionName;
    /** 条染订版色号:HY-M63634 */
    private String tdProofColor;
    /** 绞染订版色号 */
    private String jdProofColor;
    /** 颜色:中灰 */
    private String color;
    /** 辅助属性:70% 澳毛条 HY-M63634-中灰 */
    private String auxAttr;
    /** BOM版本 */
    private String bomVersion;
    /** 是否荧光 */
    private Integer isFluorescent;
    /** 络筒工艺要求 */
    private String windingReq;
    /** 技术工艺纺纱要求 */
    private String techSpinReq;
    /** 计划工艺纺纱要求 */
    private String planSpinReq;
    /** 工艺及倍捻要求 */
    private String craftTwistReq;
    /** 成品要求 */
    private String productReq;
    /** 标签打印要求 */
    private String labelPrintReq;
    /** 包装要求 */
    private String packReq;
    /** 复精梳纺纱备注 */
    private String recombedRemark;
    /** 订单状态 */
    private String orderStatus;
    /** 审核人 */
    private String auditUser;
    /** 审核时间 */
    private LocalDateTime auditTime;
    /** 作废状态 */
    private Integer voidStatus;
    /** 作废时间 */
    private LocalDateTime voidTime;
    /** 删除标志 */
    private Integer delFlag;
    /** K3订单号(MES) */
    private String k3OrderNoMes;
    /** K3订单ID */
    private String k3OrderId;
    /** K3物料ID */
    private String k3MaterialId;
    /** K3车间ID */
    private String k3WorkshopId;
    /** K3生产线ID */
    private String k3LineId;
    /** K3批号ID */
    private String batchId;
    /** K3辅助属性ID */
    private String auxAttrId;
    /** K3客户ID */
    private String customerId;
    /** K3订单明细ID */
    private String orderDetailId;
    /** K3加工单位ID */
    private String processUnitId;
    /** K3货主ID */
    private String ownerId;
    /** K3打印模板ID */
    private String printTemplateId;
    /** K3计量单位ID */
    private String measureTypeId;
    /** MES创建时间 */
    private LocalDateTime mesCreateTime;
    /** MES更新时间 */
    private LocalDateTime mesUpdateTime;
    /** K3数据版本时间(增量同步) */
    private LocalDateTime k3VersionTime;
    /** 是否关闭(引入完毕) */
    private Integer isClosed;
    /** 创建人 */
    private String createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新人 */
    private String updateBy;
    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 剩余可引入 = K3数量 - 已引入数量 */
    public BigDecimal remainingQty() {
        BigDecimal imported = importedQty == null ? BigDecimal.ZERO : importedQty;
        return (k3Qty == null ? BigDecimal.ZERO : k3Qty).subtract(imported);
    }

    /** 引入量校验：已引入 + 本次引入 ≤ 订单数量 */
    public void checkImportable(BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.xinao.mes.common.exception.BizException("引入量必须大于0");
        }
        if (remainingQty().compareTo(qty) < 0) {
            throw new com.xinao.mes.common.exception.BizException(
                    "引入量超订单剩余可引入: 剩余" + remainingQty() + " " + unit + "，本次" + qty);
        }
    }
}
