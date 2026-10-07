package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 翻批日志（按车间+班组+时段汇总生成，标题：【车间】【班组】【时段】翻批记录） */
@Data
@TableName("rep_rebatch_log")
public class RepRebatchLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 【纺八车间】【丙班】【时段】翻批记录 */
    private String title;
    /** 车间 */
    private Long workshopId;
    /** 班组 */
    private Long teamId;
    /** 开始时间 */
    private LocalDateTime startTime;
    /** 结束时间 */
    private LocalDateTime endTime;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
