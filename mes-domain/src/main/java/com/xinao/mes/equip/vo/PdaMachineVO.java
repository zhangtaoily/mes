package com.xinao.mes.equip.vo;

import com.xinao.mes.plan.domain.model.BaseEquipment;
import lombok.Data;

/**
 * PDA 扫机台结果：机台 + 车间名 + 点检责任人（点检/报修页共用）
 */
@Data
public class PdaMachineVO {

    /** 机台（基础信息） */
    private BaseEquipment equipment;

    /** 车间名称 */
    private String workshopName;

    /** 该机台点检责任人（顿号拼接，可空） */
    private String dutyUsers;
}
