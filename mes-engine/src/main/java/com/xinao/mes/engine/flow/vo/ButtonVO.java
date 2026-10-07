package com.xinao.mes.engine.flow.vo;

import lombok.Data;

/**
 * 状态机按钮（接口下发前端：f(状态, 角色) → 按钮列表）
 */
@Data
public class ButtonVO {

    /** 动作编码，前端据此调 fire 接口 */
    private String action;

    /** 按钮名称，如 提交/撤回/作废 */
    private String actionName;

    /** 目标状态 */
    private String toStatus;

    public ButtonVO() {
    }

    public ButtonVO(String action, String actionName, String toStatus) {
        this.action = action;
        this.actionName = actionName;
        this.toStatus = toStatus;
    }
}
