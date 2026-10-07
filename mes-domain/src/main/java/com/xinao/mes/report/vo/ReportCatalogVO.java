package com.xinao.mes.report.vo;

import lombok.Data;

import java.util.List;

/** 报表中心目录（原型 reports.html 左侧分类树：P0 已实现，P1/P2 后置灰显） */
@Data
public class ReportCatalogVO {

    private String category;
    /** 分类下报表清单 */
    private List<ReportItem> reports;

    @Data
    public static class ReportItem {
        /** 报表编码（即接口路径段） */
        private String code;
        private String name;
        /** P0/P1/P2 */
        private String priority;
        /** 是否已实现（P0=true） */
        private boolean implemented;
    }
}
