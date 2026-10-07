package com.xinao.mes.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.result.R;
import com.xinao.mes.system.dto.RouterVO;
import com.xinao.mes.system.entity.SysMenu;
import com.xinao.mes.system.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单接口：全量菜单树（角色分配权限页使用）
 */
@RestController
@RequestMapping("/api/sys/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuMapper menuMapper;

    /** 全量菜单树（目录/菜单/按钮，含停用） */
    @GetMapping("/tree")
    @RequiresPerm("sys:menu:list")
    public R<List<RouterVO>> tree() {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortNo));
        Map<Long, List<SysMenu>> byParent = all.stream()
                .collect(Collectors.groupingBy(SysMenu::getParentId));
        return R.ok(buildTree(byParent, 0L));
    }

    private List<RouterVO> buildTree(Map<Long, List<SysMenu>> byParent, Long parentId) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(menu -> {
                    RouterVO vo = RouterVO.from(menu);
                    vo.setChildren(buildTree(byParent, menu.getId()));
                    return vo;
                }).toList();
    }
}
