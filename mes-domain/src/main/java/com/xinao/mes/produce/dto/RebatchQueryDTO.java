package com.xinao.mes.produce.dto;

import lombok.Data;

/** 翻批日志分页查询入参 */
@Data
public class RebatchQueryDTO {

    private Long workshopId;
    private Long teamId;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
