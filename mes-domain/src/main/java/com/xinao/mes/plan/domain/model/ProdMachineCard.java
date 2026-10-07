package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.exception.BizException;
import lombok.Data;

import java.time.LocalDateTime;

/** 上机制造卡（A4 打印 + 二维码；已排在做可撤回） */
@Data
@TableName("prod_machine_card")
public class ProdMachineCard {

    public static final String STATUS_DOING = "已排在做";
    public static final String STATUS_OFF = "了机";

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 制造卡号 */
    private String cardNo;
    /** 排程ID */
    private Long scheduleId;
    /** 任务ID */
    private Long taskId;
    /** 生产批号 */
    private String batchNo;
    /** 批号标识ID */
    private Long batchFlagId;
    /** 车间ID */
    private Long workshopId;
    /** 工序ID */
    private Long processId;
    /** 机型 */
    private String machineModel;
    /** 机台ID */
    private Long equipmentId;
    /** 状态（已排在做/了机） */
    private String status;
    /** 支数（如 45.4/2） */
    private String countSpec;
    /** 品名规格（如 MwU5545 2/48 NM） */
    private String productSpec;
    /** 原料 */
    private String rawMaterial;
    /** 车速（如 350） */
    private String speed;
    /** 定长（如 20500） */
    private String fixedLength;
    /** 定量（如 0.9kg/只） */
    private String quota;
    /** 张力（如 防缩:22/20） */
    private String tension;
    /** 并合根数（如 2根） */
    private String mergeCount;
    /** 工艺说明 */
    private String craftDesc;
    /** 二维码 */
    private String qrCode;
    /** 打印时间 */
    private LocalDateTime printTime;
    /** 撤回时间 */
    private LocalDateTime withdrawTime;
    /** 创建人 */
    private String createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新人 */
    private String updateBy;
    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 仅[已排在做]可撤回：撤回后对应排程恢复可重排 */
    public void ensureWithdrawable() {
        if (!STATUS_DOING.equals(status)) {
            throw new BizException("制造卡[" + cardNo + "]状态为[" + status + "]，仅[已排在做]可撤回");
        }
        if (withdrawTime != null) {
            throw new BizException("制造卡[" + cardNo + "]已撤回，不可重复操作");
        }
    }
}
