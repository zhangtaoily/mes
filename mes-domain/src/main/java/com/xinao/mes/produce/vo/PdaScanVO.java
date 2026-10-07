package com.xinao.mes.produce.vo;

import com.xinao.mes.plan.domain.model.ProdMachineCard;
import com.xinao.mes.plan.domain.model.ProdSchedule;
import com.xinao.mes.plan.domain.model.ProdScheduleCraft;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.produce.domain.model.RepBatchOn;
import lombok.Data;

import java.util.List;

/** PDA 扫码结果（排程二维码/制造卡二维码 → 上批预览） */
@Data
public class PdaScanVO {

    private ProdSchedule schedule;
    private ProdMachineCard card;
    private ProdTask task;
    private List<ProdScheduleCraft> crafts;
    /** 该排程当前活动上批（未下批），null=可上批 */
    private RepBatchOn activeBatchOn;
    private String processName;
    private String machineNo;
}
