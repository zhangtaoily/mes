package com.xinao.mes.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 字典项（sys_dict_item，按 dict_type 关联，无逻辑删除列，物理删除） */
@Data
@TableName("sys_dict_item")
public class SysDictItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属字典类型 */
    private String dictType;
    /** 字典项标签 */
    private String itemLabel;
    /** 字典项值 */
    private String itemValue;
    /** 排序 */
    private Integer sortNo;
    /** 1启用 0停用 */
    private Integer status;
    private String createBy;
    private LocalDateTime createTime;
}
