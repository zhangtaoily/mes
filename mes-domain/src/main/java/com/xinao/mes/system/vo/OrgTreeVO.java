package com.xinao.mes.system.vo;

import com.xinao.mes.system.entity.SysOrg;
import lombok.Data;

import java.util.List;

/**
 * 组织树节点
 */
@Data
public class OrgTreeVO {

    private Long id;
    private Long parentId;
    private String orgName;
    private Integer orgType;
    private Integer sortNo;
    private Integer status;
    private List<OrgTreeVO> children;

    public static OrgTreeVO from(SysOrg org) {
        OrgTreeVO vo = new OrgTreeVO();
        vo.setId(org.getId());
        vo.setParentId(org.getParentId());
        vo.setOrgName(org.getOrgName());
        vo.setOrgType(org.getOrgType());
        vo.setSortNo(org.getSortNo());
        vo.setStatus(org.getStatus());
        return vo;
    }
}
