package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 车间产线 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_prod_line")
public class BaseProdLine extends BaseEntity {

    /** 所属车间ID */
    private Long workshopId;
    /** 产线名称：纺十车间2路 */
    private String lineName;
    /** K3生产线ID */
    private String k3LineId;
    /** 状态：1启用 0停用 */
    private Integer status;
}
