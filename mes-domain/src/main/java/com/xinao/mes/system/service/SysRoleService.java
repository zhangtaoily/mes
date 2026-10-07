package com.xinao.mes.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.system.dto.RoleSaveDTO;
import com.xinao.mes.system.entity.SysRole;
import com.xinao.mes.system.entity.SysRoleMenu;
import com.xinao.mes.system.entity.SysUserRole;
import com.xinao.mes.system.mapper.SysRoleMapper;
import com.xinao.mes.system.mapper.SysRoleMenuMapper;
import com.xinao.mes.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色服务
 */
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    /** 分页查询 */
    public PageResult<SysRole> page(Integer pageNum, Integer pageSize, String roleName) {
        Page<SysRole> page = roleMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysRole>()
                        .like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName)
                        .orderByAsc(SysRole::getId));
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    /** 全量角色（用户编辑页下拉用） */
    public List<SysRole> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getId));
    }

    /** 新增角色 */
    @Transactional(rollbackFor = Exception.class)
    public void save(RoleSaveDTO dto) {
        checkRoleKeyUnique(dto.getRoleKey(), null);
        SysRole role = new SysRole();
        copy(dto, role);
        roleMapper.insert(role);
    }

    /** 修改角色 */
    @Transactional(rollbackFor = Exception.class)
    public void update(RoleSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("角色ID不能为空");
        }
        checkRoleKeyUnique(dto.getRoleKey(), dto.getId());
        SysRole role = new SysRole();
        copy(dto, role);
        role.setId(dto.getId());
        roleMapper.updateById(role);
    }

    /** 删除角色（有用户挂载时拒绝删除） */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id));
        if (userCount > 0) {
            throw new BizException("角色已分配用户，不能删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, id));
    }

    /** 已分配菜单/按钮ID集合 */
    public List<Long> listMenuIds(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    /** 分配权限：重建角色菜单关联（按钮级） */
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds != null) {
            menuIds.stream().distinct()
                    .map(menuId -> new SysRoleMenu(roleId, menuId))
                    .forEach(roleMenuMapper::insert);
        }
    }

    private void copy(RoleSaveDTO dto, SysRole role) {
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setDataScope(dto.getDataScope());
        role.setStatus(dto.getStatus());
        role.setRemark(dto.getRemark());
    }

    private void checkRoleKeyUnique(String roleKey, Long excludeId) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, roleKey)
                .ne(excludeId != null, SysRole::getId, excludeId));
        if (count > 0) {
            throw new BizException("角色编码已存在: " + roleKey);
        }
    }
}
