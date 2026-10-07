package com.xinao.mes.engine.flow;

/**
 * 通用单据状态常量（DB 中存中文，各单据类型按需取子集）
 *
 * <p>典型链路：编制中→已提交→生效→了批→已关闭；旁路：撤回/作废/取消作废。
 * 仅状态迁移，无审批流。
 */
public final class BillStatus {

    public static final String DRAFT = "编制中";
    public static final String SUBMITTED = "已提交";
    public static final String EFFECTIVE = "生效";
    public static final String BATCH_OFF = "了批";
    public static final String CLOSED = "已关闭";
    public static final String VOIDED = "已作废";

    private BillStatus() {
    }
}
