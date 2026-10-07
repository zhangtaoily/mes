package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 生产工序 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_process")
public class BaseProcess extends BaseEntity {

    /** 工序编号：PP000001 */
    private String processCode;
    /** 工序名：混条(一)/头针/二针/三针/四针/粗纱/细纱/并线/倍捻/摇绞/倒毛/倒丝 */
    private String processName;
    /** 前纺/后纺/复精梳/倒筒 */
    private String processType;
    /** 排序号 */
    private Integer sortNo;
    /** 物料单位（默认KG） */
    private String materialUnit;
    /** 生产单位 */
    private String prodUnit;
    /** 计数单位 */
    private String countUnit;
    /** 状态：1启用 0停用 */
    private Integer status;
}
