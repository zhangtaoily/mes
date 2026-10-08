package com.xinao.mes.equip.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 故障分类（设备基础字典） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_fault_type")
public class EqFaultType extends BaseEntity {

    /** 故障编码 */
    private String faultCode;
    /** 故障名称 */
    private String faultName;
    /** 故障描述 */
    private String faultDesc;
    /** 状态 */
    private Integer status;
}
