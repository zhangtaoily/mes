package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 投料记录明细（原料批号/领料数量/本次投料量/已投料量） */
@Data
@TableName("mt_feed_item")
public class MtFeedItem {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投料单ID（关联 mt_feed.id） */
    private Long feedId;
    /** 原料批号 */
    private String rawBatchNo;
    /** 物料号 */
    private String materialNo;
    /** 物料名称 */
    private String materialName;
    /** 物料规格 */
    private String materialSpec;
    /** 领料数量 */
    private BigDecimal pickQty;
    /** 本次投料量 */
    private BigDecimal thisFeedQty;
    /** 已投料量 */
    private BigDecimal fedQty;
}
