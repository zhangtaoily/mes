package com.xinao.mes.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 字典类型（sys_dict，无逻辑删除列，物理删除） */
@Data
@TableName("sys_dict")
public class SysDict implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型：stop_reason停机原因/shift班次/defect_type疵纱分类等 */
    private String dictType;
    /** 类型名称 */
    private String dictName;
    /** 1启用 0停用 */
    private Integer status;
    private String createBy;
    private LocalDateTime createTime;
}
