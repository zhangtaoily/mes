package com.xinao.mes.produce.vo;

import com.xinao.mes.produce.domain.model.RepBatchOn;
import com.xinao.mes.produce.domain.model.RepBatchOnCraft;
import com.xinao.mes.produce.domain.model.RepBatchOnFlag;
import lombok.Data;

import java.util.List;

/** 上批汇报详情（工序标识/工艺项子表） */
@Data
public class BatchOnDetailVO {

    private RepBatchOn batchOn;
    private List<RepBatchOnFlag> flags;
    private List<RepBatchOnCraft> crafts;
    private String scheduleNo;
    private String processName;
    private String machineNo;
    private String teamName;
    private String taskNo;
}
