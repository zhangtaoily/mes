package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 计划工艺单-生产信息 */
@Data
@TableName("plan_craft_prod")
public class PlanCraftProd {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划工艺单ID */
    private Long planCraftId;
    /** 产品编号:5.1.2.004.099 */
    private String productNo;
    /** 品名 */
    private String productName;
    /** 打印缩写:W100 */
    private String printAbbr;
    /** 英文品名 */
    private String nameEn;
    /** 产品规格:2/60 NM */
    private String productSpec;
    /** 辅助属性:90%防缩短毛条(阳离子) */
    private String auxAttr;
    /** 色号 */
    private String colorNo;
    /** 公司色号 */
    private String companyColor;
    /** 条染订版色号 */
    private String tdProofColor;
    /** 绞染订版色号 */
    private String jdProofColor;
    /** 实纺支数 */
    private String actualCount;
    /** 复精梳批号 */
    private String recombedBatchNo;
    /** 复精梳编号 */
    private String recombedNo;
    /** 细纱支数 */
    private String fineCount;
    /** 纺纱支数 */
    private String spinCount;
    /** 单位 */
    private String unit;
    /** 英文打印 */
    private String printEn;
    /** 中文打印 */
    private String printCn;
    /** 用途 */
    private String purpose;
    /** 客户名称 */
    private String customerName;
    /** 销售部门 */
    private String saleDept;
    /** 销售员 */
    private String saleUser;
    /** 是否荧光 */
    private Integer isFluorescent;
    /** 股数/支数:2/60 */
    private String plyCount;
}
