package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 车间现场问题反馈（st_issue，无逻辑删除列，物理删除） */
@Data
@TableName("st_issue")
public class StIssue implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 检查人 */
    private String checkUser;
    private LocalDate checkDate;
    /** 问题所在 */
    private String issueLocation;
    /** 问题描述 */
    private String issueDesc;
    /** 整改状态 */
    private String rectifyStatus;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
