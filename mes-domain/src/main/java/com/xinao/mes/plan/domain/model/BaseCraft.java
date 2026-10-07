package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 技术工艺库（基础工艺模板，工艺配置引用源1） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_craft")
public class BaseCraft extends BaseEntity {

    /** 工艺编号：CT000006 */
    private String craftCode;
    /** 工艺名称：倒丝等 */
    private String craftName;
    /** 成分 */
    private String component;
    /** 实纺支数 */
    private BigDecimal actualCount;
    /** 工艺等级 */
    private String craftLevel;
    /** 染色方式 */
    private String dyeMode;
    /** 捻度 */
    private BigDecimal twist;
    /** 启用/停用 */
    private Integer enabled;
    /** 备注 */
    private String remark;
}
