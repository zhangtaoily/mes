package com.xinao.mes.system.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.system.dto.UserQueryDTO;
import com.xinao.mes.system.dto.UserSaveDTO;
import com.xinao.mes.system.entity.SysUser;
import com.xinao.mes.system.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户管理接口（按钮级权限示例：sys:user:*）
 */
@RestController
@RequestMapping("/api/sys/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("sys:user:list")
    public R<PageResult<SysUser>> page(UserQueryDTO query) {
        return R.ok(userService.page(query));
    }

    /** 新增用户 */
    @PostMapping
    @RequiresPerm("sys:user:add")
    @OperLog(title = "新增用户")
    public R<Void> save(@Valid @RequestBody UserSaveDTO dto) {
        userService.save(dto);
        return R.ok();
    }

    /** 修改用户 */
    @PutMapping("/{id}")
    @RequiresPerm("sys:user:edit")
    @OperLog(title = "修改用户")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody UserSaveDTO dto) {
        dto.setId(id);
        userService.update(dto);
        return R.ok();
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    @RequiresPerm("sys:user:del")
    @OperLog(title = "删除用户")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    /** 重置密码 */
    @PutMapping("/{id}/password")
    @RequiresPerm("sys:user:resetPwd")
    @OperLog(title = "重置用户密码")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return R.ok();
    }

    /** 启用/停用 */
    @PutMapping("/{id}/status/{status}")
    @RequiresPerm("sys:user:edit")
    @OperLog(title = "修改用户状态")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        userService.changeStatus(id, status);
        return R.ok();
    }
}
