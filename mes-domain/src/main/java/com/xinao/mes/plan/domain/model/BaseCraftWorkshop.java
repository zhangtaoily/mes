package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 车间工艺库（工序>机型>工艺项，工艺配置引用源2） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_craft_workshop")
public class BaseCraftWorkshop extends BaseEntity {

    /** 车间工艺名称 */
    private String craftName;
    /** 车间ID */
    private Long workshopId;
    /** 车间产线：复二4组/纺十车间2路 */
    private String prodLine;
    /** 原料明细 */
    private String rawMaterial;
    /** 实纺支数 */
    private BigDecimal actualCount;
    /** 捻度 */
    private BigDecimal twist;
    /** 染色方式 */
    private String dyeMode;
    /** 工艺等级 */
    private String craftLevel;
    /** 小时产能 */
    private BigDecimal hourCapacity;
    /** 启用/停用 */
    private Integer enabled;
}
