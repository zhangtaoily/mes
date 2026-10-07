package com.xinao.mes.plan.vo;

import com.xinao.mes.engine.flow.vo.ButtonVO;
import com.xinao.mes.plan.domain.model.ProdSchedule;
import com.xinao.mes.plan.domain.model.ProdScheduleCraft;
import com.xinao.mes.plan.domain.model.ProdScheduleMaterial;
import lombok.Data;

import java.util.List;

/** 排程详情（3Tab + 任务明细含实时剩余量） */
@Data
public class ScheduleDetailVO {

    private ProdSchedule schedule;
    private String workshopName;
    private String processName;
    private String machineNo;
    private List<ProdScheduleCraft> crafts;
    private List<ProdScheduleMaterial> materials;
    private List<TaskItemVO> tasks;

    /** 任务明细行（新增面板选择后插入的行，含剩余可排） */
    @Data
    public static class TaskItemVO {
        private Long id;
        private Long taskId;
        private String taskNo;
        private String productNo;
        private String productName;
        private String spec;
        private String auxAttr;
        private String rawDetail;
        private String processName;
        private java.math.BigDecimal planQty;
        private java.math.BigDecimal scheduledQty;
        private java.math.BigDecimal scheduleQty;
        /** 实时剩余可排（=任务计划产量-未删排程占用合计） */
        private java.math.BigDecimal remaining;
        private String unit;
    }
}
