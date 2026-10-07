package com.xinao.mes.system.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.system.dto.RoleSaveDTO;
import com.xinao.mes.system.entity.SysRole;
import com.xinao.mes.system.service.SysRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理接口（含分配权限）
 */
@RestController
@RequestMapping("/api/sys/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("sys:role:list")
    public R<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "20") Integer pageSize,
                                       @RequestParam(required = false) String roleName) {
        return R.ok(roleService.page(pageNum, pageSize, roleName));
    }

    /** 全量角色（下拉用，登录即可） */
    @GetMapping("/all")
    public R<List<SysRole>> listAll() {
        return R.ok(roleService.listAll());
    }

    /** 新增角色 */
    @PostMapping
    @RequiresPerm("sys:role:add")
    @OperLog(title = "新增角色")
    public R<Void> save(@Valid @RequestBody RoleSaveDTO dto) {
        roleService.save(dto);
        return R.ok();
    }

    /** 修改角色 */
    @PutMapping("/{id}")
    @RequiresPerm("sys:role:edit")
    @OperLog(title = "修改角色")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RoleSaveDTO dto) {
        dto.setId(id);
        roleService.update(dto);
        return R.ok();
    }

    /** 删除角色 */
    @DeleteMapping("/{id}")
    @RequiresPerm("sys:role:del")
    @OperLog(title = "删除角色")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    /** 已分配菜单/按钮 */
    @GetMapping("/{id}/menus")
    @RequiresPerm("sys:role:list")
    public R<List<Long>> listMenuIds(@PathVariable Long id) {
        return R.ok(roleService.listMenuIds(id));
    }

    /** 分配权限 */
    @PutMapping("/{id}/menus")
    @RequiresPerm("sys:role:perm")
    @OperLog(title = "分配角色权限")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return R.ok();
    }
}
