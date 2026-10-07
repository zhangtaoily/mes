package com.xinao.mes.produce.vo;

import com.xinao.mes.produce.domain.model.RepBatchOn;
import lombok.Data;

/** 上批汇报列表行（排程号/工序/机台/班组/标识名联查展示） */
@Data
public class BatchOnListVO {

    private RepBatchOn batchOn;
    private String scheduleNo;
    private String processName;
    private String machineNo;
    private String teamName;
    private String flagName;
}
