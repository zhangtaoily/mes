package com.xinao.mes.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色菜单关联（sys_role_menu）
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;
    private Long menuId;

    public SysRoleMenu() {
    }

    public SysRoleMenu(Long roleId, Long menuId) {
        this.roleId = roleId;
        this.menuId = menuId;
    }
}
