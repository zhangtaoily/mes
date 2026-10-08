package com.xinao.mes.equip.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 配件型号（设备基础字典） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_part_model")
public class EqPartModel extends BaseEntity {

    /** 件号 */
    private String partNo;
    /** 配件名称 */
    private String partName;
    /** 配件描述 */
    private String partDesc;
    /** 状态 */
    private Integer status;
}
