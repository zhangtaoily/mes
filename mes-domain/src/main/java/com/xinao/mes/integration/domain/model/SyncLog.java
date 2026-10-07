package com.xinao.mes.integration.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 接口同步日志（log_sync）——外部系统交互的幂等与重试账本。
 *
 * <p>幂等键：(sync_type, iface_id) 唯一索引（V8）——K3 内码 / WMS 托盘号；
 * 同键重推命中唯一约束即跳过；失败行（sync_result=0）由重试任务按类型路由重放。
 * 五列重量在 WMS 推送场景落列，便于对账。
 */
@Data
@TableName("log_sync")
public class SyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 同步类型：K3订单 / K3回写 / WMS入库 */
    private String syncType;
    private String opName;
    /** 业务单号：订单号 / 托盘号 */
    private String bizNo;
    /** 接口侧幂等键（K3内码 / 托盘号）；NULL 表示每次留痕的动作（如回写） */
    private String ifaceId;
    /** 1成功 0失败 */
    private Integer syncResult;
    /** 重试次数 */
    private Integer retryCount;
    private BigDecimal costSeconds;
    private String reqParam;
    private String respParam;
    private BigDecimal publicWeight;
    private BigDecimal grossWeight;
    private BigDecimal tareWeight;
    private BigDecimal netWeight;
    /** 依赖 DB DEFAULT CURRENT_TIMESTAMP（插入不传） */
    private LocalDateTime createTime;
}
