package com.xinao.mes.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.system.dto.UserQueryDTO;
import com.xinao.mes.system.dto.UserSaveDTO;
import com.xinao.mes.system.entity.SysUser;
import com.xinao.mes.system.entity.SysUserRole;
import com.xinao.mes.system.mapper.SysUserMapper;
import com.xinao.mes.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 分页查询 */
    public PageResult<SysUser> page(UserQueryDTO query) {
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysUser> result = userMapper.selectPage(page, new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(query.getLoginName()), SysUser::getLoginName, query.getLoginName())
                .like(StringUtils.hasText(query.getUserName()), SysUser::getUserName, query.getUserName())
                .eq(query.getOrgId() != null, SysUser::getOrgId, query.getOrgId())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .orderByDesc(SysUser::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 新增用户（登录名唯一校验 + BCrypt 加密 + 角色关联） */
    @Transactional(rollbackFor = Exception.class)
    public void save(UserSaveDTO dto) {
        checkLoginNameUnique(dto.getLoginName(), null);
        SysUser user = new SysUser();
        copy(dto, user);
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BizException("初始密码不能为空");
        }
        user.setPassword(encoder.encode(dto.getPassword()));
        userMapper.insert(user);
        saveRoles(user.getId(), dto.getRoleIds());
    }

    /** 修改用户（不传密码则不改密码；重建角色关联） */
    @Transactional(rollbackFor = Exception.class)
    public void update(UserSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("用户ID不能为空");
        }
        SysUser exist = userMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("用户不存在");
        }
        checkLoginNameUnique(dto.getLoginName(), dto.getId());
        SysUser user = new SysUser();
        copy(dto, user);
        user.setId(dto.getId());
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(encoder.encode(dto.getPassword()));
        }
        userMapper.updateById(user);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, dto.getId()));
        saveRoles(dto.getId(), dto.getRoleIds());
    }

    /** 删除用户（逻辑删 + 清理角色关联） */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, id));
    }

    /** 重置密码 */
    public void resetPassword(Long id, String password) {
        if (!StringUtils.hasText(password)) {
            throw new BizException("新密码不能为空");
        }
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(encoder.encode(password));
        userMapper.updateById(user);
    }

    /** 启用/停用 */
    public void changeStatus(Long id, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    private void copy(UserSaveDTO dto, SysUser user) {
        user.setOrgId(dto.getOrgId());
        user.setEmpNo(dto.getEmpNo());
        user.setUserName(dto.getUserName());
        user.setLoginName(dto.getLoginName());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus());
    }

    private void checkLoginNameUnique(String loginName, Long excludeId) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getLoginName, loginName)
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (count > 0) {
            throw new BizException("登录名已存在: " + loginName);
        }
    }

    private void saveRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        roleIds.stream().distinct()
                .map(roleId -> new SysUserRole(userId, roleId))
                .forEach(userRoleMapper::insert);
    }
}
