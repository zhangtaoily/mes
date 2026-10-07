package com.xinao.mes.plan.vo;

import com.xinao.mes.engine.flow.vo.ButtonVO;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.domain.model.ProdTaskCraft;
import com.xinao.mes.plan.domain.model.ProdTaskExtra;
import com.xinao.mes.plan.domain.model.ProdTaskMaterial;
import com.xinao.mes.plan.domain.model.ProdTaskOrder;
import com.xinao.mes.plan.domain.model.ProdTaskRoute;
import lombok.Data;

import java.util.List;

/** 生产任务详情（6Tab 聚合视图 + 可用按钮） */
@Data
public class TaskDetailVO {

    private ProdTask task;
    private String workshopName;
    private List<ProdTaskOrder> orders;
    private List<ProdTaskMaterial> materials;
    private List<ProdTaskRoute> routes;
    private List<ProdTaskCraft> crafts;
    private ProdTaskExtra extra;
    private List<ButtonVO> buttons;
}
