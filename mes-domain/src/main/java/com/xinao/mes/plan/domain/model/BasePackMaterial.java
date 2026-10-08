package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 包装材质（内销纸箱/内胆纸箱/外销纸箱等） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_pack_material")
public class BasePackMaterial extends BaseEntity {

    /** 材质编码 */
    private String materialCode;
    /** 材质名称：内销纸箱(标准)/内胆纸箱(实重) */
    private String materialName;
    /** 包装规格（长×宽×高） */
    private String spec;
    /** 纸箱重量 KG */
    private BigDecimal weight;
    /** 状态：1启用 0停用 */
    private Integer status;
}
