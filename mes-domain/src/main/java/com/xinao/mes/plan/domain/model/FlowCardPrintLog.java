package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 工艺流转卡打印记录（补打留痕） */
@Data
@TableName("flow_card_print_log")
public class FlowCardPrintLog {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 来源ID */
    private Long sourceId;
    /** 来源(制造卡/工艺单) */
    private String sourceTable;
    /** 打印时间 */
    private LocalDateTime printTime;
    /** 打印人 */
    private String printer;
    /** 打印次数 */
    private Integer printCount;
}
