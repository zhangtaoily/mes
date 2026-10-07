package com.xinao.mes.plan.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 批量操作入参（提交/撤回/了批/关闭/删除等） */
@Data
public class BatchIdsDTO {

    @NotEmpty(message = "请先勾选")
    private List<Long> ids;
}
