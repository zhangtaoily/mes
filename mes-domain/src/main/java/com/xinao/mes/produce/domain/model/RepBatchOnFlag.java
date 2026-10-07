package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 上批汇报-工序标识（标识牌占用记录，了批释放） */
@Data
@TableName("rep_batch_on_flag")
public class RepBatchOnFlag {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上批汇报ID（关联 rep_batch_on.id） */
    private Long batchOnId;
    /** 工序标识(标识牌) */
    private Long flagId;
    /** 任务号 */
    private String taskNo;
}
