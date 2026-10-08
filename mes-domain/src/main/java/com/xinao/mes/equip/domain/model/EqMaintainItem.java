package com.xinao.mes.equip.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 保养项目（设备基础字典） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_maintain_item")
public class EqMaintainItem extends BaseEntity {

    /** 项目编码 */
    private String itemCode;
    /** 项目名称 */
    private String itemName;
    /** 保养类型 */
    private String maintainType;
    /** 项目描述 */
    private String itemDesc;
    /** 状态 */
    private Integer status;
}
