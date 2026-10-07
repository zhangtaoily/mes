package com.xinao.mes.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单/按钮（sys_menu）：menu_type 1目录 2菜单 3按钮
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单ID，顶级为 0 */
    private Long parentId;
    /** 菜单名 */
    private String menuName;
    /** 1目录 2菜单 3按钮 */
    private Integer menuType;
    /** 路由地址 */
    private String path;
    /** 前端组件 */
    private String component;
    /** 按钮权限串，如 sys:user:add */
    private String perms;
    /** 是否可见 */
    private Integer visible;
    /** 1启用 0停用 */
    private Integer status;
    /** 已停用标记（业务性停用，如"每日排班(已停用)"） */
    private Integer stopped;
    /** 排序号 */
    private Integer sortNo;

    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
