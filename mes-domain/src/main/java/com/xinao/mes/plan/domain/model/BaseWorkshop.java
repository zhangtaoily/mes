package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 车间 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_workshop")
public class BaseWorkshop extends BaseEntity {

    /** 车间编号 */
    private String workshopCode;
    /** 车间名称：纺十车间/总厂复精梳车间(二)/总厂倒筒车间等 */
    private String workshopName;
    /** 1纺纱 2复精梳 3倒筒 4包装 */
    private Integer workshopType;
    /** K3车间ID */
    private String k3WorkshopId;
    /** 状态：1启用 0停用 */
    private Integer status;
}
