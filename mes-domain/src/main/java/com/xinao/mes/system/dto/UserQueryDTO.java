package com.xinao.mes.system.dto;

import lombok.Data;

/**
 * 用户分页查询入参
 */
@Data
public class UserQueryDTO {

    private String loginName;
    private String userName;
    private Long orgId;
    private Integer status;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
