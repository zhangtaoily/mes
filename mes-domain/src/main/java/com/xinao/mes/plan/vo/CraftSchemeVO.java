package com.xinao.mes.plan.vo;

import lombok.Data;

/** 工艺库引用方案行（工艺配置/编制工艺单选择面板） */
@Data
public class CraftSchemeVO {

    /** 方案id（技术库=base_craft.id / 车间库=base_craft_workshop.id） */
    private Long schemeId;
    /** 方案编号（技术库=工艺编号CT... / 车间库=CJ+id） */
    private String schemeNo;
    private String schemeName;
    /** 引用来源：1技术工艺库 2车间工艺库 */
    private Integer refSource;
    private Long processId;
    private String processName;
    private String machineModel;
    /** 该工序下工艺项数量 */
    private Integer paramCount;
}
