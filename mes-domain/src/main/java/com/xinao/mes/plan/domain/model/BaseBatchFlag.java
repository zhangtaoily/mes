package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 标识牌（批号标识色块） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_batch_flag")
public class BaseBatchFlag extends BaseEntity {

    /** 标识名：紫叶子/三角/菱形/粉色 */
    private String flagName;
    /** 别名 */
    private String alias;
    /** 标识预览图 */
    private String previewUrl;
    /** 适用工序ID */
    private Long processId;
    /** 状态：1启用 0停用 */
    private Integer status;
}
