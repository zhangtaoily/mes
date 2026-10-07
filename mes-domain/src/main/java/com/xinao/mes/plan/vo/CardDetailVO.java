package com.xinao.mes.plan.vo;

import com.xinao.mes.plan.domain.model.ProdMachineCard;
import lombok.Data;

/** 制造卡详情/列表行（A4 打印数据源） */
@Data
public class CardDetailVO {

    private ProdMachineCard card;
    private String workshopName;
    private String processName;
    private String machineNo;
    private String scheduleNo;
    private String workOrderNo;
    /** 已打印次数（打印留痕） */
    private Integer printCount;
}
