package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 打印样式模板（筒贴/箱单/吊牌/制造卡/唛架/流程单/工艺单） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_print_template")
public class BasePrintTemplate extends BaseEntity {

    /** 模板编码 */
    private String templateCode;
    /** 模板名称：标准带RWS认证(英文)/标准 */
    private String templateName;
    /** 模板类型：1筒贴 2箱单 3吊牌 4制造卡 5唛架 6流程单 7工艺单 */
    private Integer templateType;
    /** 纸张类型：热敏纸76×36/A4 */
    private String paperType;
    /** LOGO 地址 */
    private String logoUrl;
    /** 打印参数模板 */
    private String content;
    /** 状态：1启用 0停用 */
    private Integer status;
}
