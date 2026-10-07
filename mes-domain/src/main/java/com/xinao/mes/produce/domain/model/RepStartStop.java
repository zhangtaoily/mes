package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 启停汇报（停机记录，设备利用率考核数据源） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rep_start_stop")
public class RepStartStop extends BaseEntity {

    /** 停机编号 */
    private String stopNo;
    /** 车间 */
    private Long workshopId;
    /** 机台 */
    private Long equipmentId;
    /** 机型 */
    private String machineModel;
    /** 汇报班组 */
    private Long teamId;
    /** 汇报人 */
    private String reportUser;
    /** 汇报日期 */
    private LocalDate reportDate;
    /** 停机时长(小时) */
    private BigDecimal stopDuration;
    /** 停机原因:撞车/落料/停机/其他 */
    private String stopReason;
    /** 备注 */
    private String remark;
}
