package com.xinao.mes.equip.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 维保方式（设备基础字典） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_maintain_method")
public class EqMaintainMethod extends BaseEntity {

    /** 方式编码 */
    private String methodCode;
    /** 方式名称 */
    private String methodName;
    /** 方式描述 */
    private String methodDesc;
    /** 状态 */
    private Integer status;
}
