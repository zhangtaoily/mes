package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 工艺参数（技术/车间工艺库共用的工艺项，craft_id 指向 base_craft 或 base_craft_workshop） */
@Data
@TableName("base_craft_param")
public class BaseCraftParam {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工艺ID（指向 base_craft 或 base_craft_workshop） */
    private Long craftId;
    /** 所属工序ID */
    private Long processId;
    /** 机型（车间工艺库按 工序+机型 带出） */
    private String machineModel;
    /** 工艺项：车速/定长/定量/张力/并合根数/牵伸倍数/喂入单重等 */
    private String paramName;
    /** 工艺值 */
    private String paramValue;
    /** 下限 */
    private String lowerLimit;
    /** 上限 */
    private String upperLimit;
    /** 单位 */
    private String unit;
    /** 排序号 */
    private Integer sortNo;
    /** 创建人 */
    private String createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新人 */
    private String updateBy;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
