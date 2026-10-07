package com.xinao.mes.produce.dto;

import com.xinao.mes.produce.domain.model.RepBatchOn;
import com.xinao.mes.produce.domain.model.RepBatchOnCraft;
import com.xinao.mes.produce.domain.model.RepBatchOnFlag;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 上批保存入参（PC/PDA 同源） */
@Data
public class BatchOnSaveDTO {

    @NotNull(message = "上批主信息不能为空")
    private RepBatchOn batchOn;

    /** 工序标识（标识牌，可多条） */
    private List<RepBatchOnFlag> flags;

    /** 工艺项（工艺项/工艺值/检验值） */
    private List<RepBatchOnCraft> crafts;
}
