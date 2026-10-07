package com.xinao.mes.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 组织新增/修改入参
 */
@Data
public class OrgSaveDTO {

    private Long id;

    /** 顶级传 0 */
    @NotNull(message = "上级组织不能为空")
    private Long parentId;

    @NotBlank(message = "组织名称不能为空")
    private String orgName;

    @NotNull(message = "组织类型不能为空")
    private Integer orgType;

    private Integer sortNo;
    private Integer status;
}
