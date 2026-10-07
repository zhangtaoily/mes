package com.xinao.mes.system.dto;

import com.xinao.mes.system.entity.SysMenu;
import lombok.Data;

import java.util.List;

/**
 * 前端路由/菜单树节点
 */
@Data
public class RouterVO {

    private Long id;
    private Long parentId;
    private String menuName;
    private Integer menuType;
    private String path;
    private String component;
    private String perms;
    private List<RouterVO> children;

    public static RouterVO from(SysMenu menu) {
        RouterVO vo = new RouterVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setPerms(menu.getPerms());
        return vo;
    }
}
