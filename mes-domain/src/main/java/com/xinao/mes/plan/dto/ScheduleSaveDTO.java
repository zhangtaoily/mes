package com.xinao.mes.plan.dto;

import com.xinao.mes.plan.domain.model.ProdSchedule;
import com.xinao.mes.plan.domain.model.ProdScheduleCraft;
import com.xinao.mes.plan.domain.model.ProdScheduleMaterial;
import lombok.Data;

import java.util.List;

/** 生产排程保存入参（任务明细走 addTaskDetail/removeTaskDetail） */
@Data
public class ScheduleSaveDTO {

    /** 主信息（id=null 新增，否则更新） */
    private ProdSchedule schedule;
    private List<ProdScheduleCraft> crafts;
    private List<ProdScheduleMaterial> materials;
}
