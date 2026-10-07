package com.xinao.mes.plan.dto;

import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.domain.model.ProdTaskExtra;
import com.xinao.mes.plan.domain.model.ProdTaskMaterial;
import com.xinao.mes.plan.domain.model.ProdTaskOrder;
import com.xinao.mes.plan.domain.model.ProdTaskRoute;
import lombok.Data;

import java.util.List;

/** 生产任务保存入参（工艺配置走 applyCraft 单独接口） */
@Data
public class TaskSaveDTO {

    /** 主信息（id=null 新增，否则更新） */
    private ProdTask task;
    private List<ProdTaskOrder> orders;
    private List<ProdTaskMaterial> materials;
    private List<ProdTaskRoute> routes;
    private ProdTaskExtra extra;
}
