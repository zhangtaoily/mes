package com.xinao.mes.plan.vo;

import com.xinao.mes.plan.domain.model.ProdMachineCard;
import lombok.Data;

/** 制造卡列表行（带车间/工序/机台/排程号） */
@Data
public class CardListVO {

    private ProdMachineCard card;
    private String workshopName;
    private String processName;
    private String machineNo;
    private String scheduleNo;
}
