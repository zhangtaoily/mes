package com.xinao.mes.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinao.mes.common.auth.JwtUtil;
import com.xinao.mes.common.auth.LoginUser;
import com.xinao.mes.common.auth.UserAuthPort;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.system.dto.LoginDTO;
import com.xinao.mes.system.dto.LoginVO;
import com.xinao.mes.system.dto.RouterVO;
import com.xinao.mes.system.entity.SysMenu;
import com.xinao.mes.system.entity.SysRole;
import com.xinao.mes.system.entity.SysRoleMenu;
import com.xinao.mes.system.entity.SysUser;
import com.xinao.mes.system.entity.SysUserRole;
import com.xinao.mes.system.mapper.SysMenuMapper;
import com.xinao.mes.system.mapper.SysRoleMapper;
import com.xinao.mes.system.mapper.SysRoleMenuMapper;
import com.xinao.mes.system.mapper.SysUserMapper;
import com.xinao.mes.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证服务：登录签发 JWT + 用户权限集加载（实现 UserAuthPort 供拦截器调用）
 */
@Service
@RequiredArgsConstructor
public class AuthService implements UserAuthPort {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 登录：校验账号密码，签发 token 并返回权限集
     */
    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getLoginName, dto.getLoginName()));
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException("登录名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("账号已停用，请联系管理员");
        }

        LoginUser loginUser = buildLoginUser(user);
        return LoginVO.builder()
                .token(jwtUtil.createToken(user.getId()))
                .userId(user.getId())
                .userName(user.getUserName())
                .empNo(user.getEmpNo())
                .orgId(user.getOrgId() == null ? null : String.valueOf(user.getOrgId()))
                .roles(loginUser.getRoles())
                .perms(loginUser.getPerms())
                .build();
    }

    /**
     * 当前用户可见菜单树（超管全量，普通用户按角色过滤）
     */
    public List<RouterVO> getMenuTree(Long userId) {
        LoginUser user = loadUserById(userId);
        if (user == null) {
            return Collections.emptyList();
        }
        List<SysMenu> menus;
        if (user.isAdmin()) {
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getMenuType, 1, 2)
                    .eq(SysMenu::getStatus, 1)
                    .eq(SysMenu::getVisible, 1)
                    .orderByAsc(SysMenu::getSortNo));
        } else {
            // 用户角色 → 角色菜单
            List<Long> roleIds = listUserRoleIds(userId);
            if (roleIds.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                            .in(SysRoleMenu::getRoleId, roleIds))
                    .stream().map(SysRoleMenu::getMenuId).distinct().toList();
            if (menuIds.isEmpty()) {
                return Collections.emptyList();
            }
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getId, menuIds)
                    .in(SysMenu::getMenuType, 1, 2)
                    .eq(SysMenu::getStatus, 1)
                    .eq(SysMenu::getVisible, 1)
                    .orderByAsc(SysMenu::getSortNo));
        }
        return buildTree(menus, 0L);
    }

    @Override
    public LoginUser loadUserById(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)
                || user.getStatus() == null || user.getStatus() != 1) {
            return null;
        }
        return buildLoginUser(user);
    }

    // ------------------------------------------------------------------

    private LoginUser buildLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setOrgId(user.getOrgId());
        loginUser.setEmpNo(user.getEmpNo());
        loginUser.setUserName(user.getUserName());
        loginUser.setLoginName(user.getLoginName());

        List<Long> roleIds = listUserRoleIds(user.getId());
        Set<String> roleKeys = roleIds.isEmpty() ? Collections.emptySet()
                : roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleKey).collect(Collectors.toSet());
        loginUser.setRoles(roleKeys);
        loginUser.setPerms(loadPerms(user.getId(), roleIds));
        return loginUser;
    }

    /**
     * 权限集：超管通配 "*"，否则取角色关联按钮的 perms
     */
    private Set<String> loadPerms(Long userId, List<Long> roleIds) {
        if (roleIds.contains(1L) || isAdminRole(roleIds)) {
            return Set.of("*");
        }
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .in(SysRoleMenu::getRoleId, roleIds))
                .stream().map(SysRoleMenu::getMenuId).distinct().toList();
        if (menuIds.isEmpty()) {
            return Collections.emptySet();
        }
        return menuMapper.selectBatchIds(menuIds).stream()
                .map(SysMenu::getPerms)
                .filter(Objects::nonNull)
                .filter(p -> !p.isBlank())
                .collect(Collectors.toSet());
    }

    private boolean isAdminRole(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return false;
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getRoleKey()));
    }

    private List<Long> listUserRoleIds(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).distinct().toList();
    }

    private List<RouterVO> buildTree(List<SysMenu> menus, Long parentId) {
        Map<Long, List<SysMenu>> byParent = menus.stream()
                .collect(Collectors.groupingBy(SysMenu::getParentId));
        byParent.values().forEach(list -> list.sort(Comparator.comparing(m ->
                m.getSortNo() == null ? 0 : m.getSortNo())));
        return buildChildren(byParent, parentId);
    }

    private List<RouterVO> buildChildren(Map<Long, List<SysMenu>> byParent, Long parentId) {
        List<RouterVO> result = new ArrayList<>();
        for (SysMenu menu : byParent.getOrDefault(parentId, List.of())) {
            RouterVO vo = RouterVO.from(menu);
            vo.setChildren(buildChildren(byParent, menu.getId()));
            result.add(vo);
        }
        return result;
    }
}
