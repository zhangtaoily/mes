package com.xinao.mes.qc.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 包装质检报告保存入参（15 疑纱分类明细整表保存，合格率服务端计算） */
@Data
public class QcPackageReportSaveDTO {

    /** 空=新建 */
    private Long id;

    private String warehouseBatchNo;
    private String batchNo;
    private Long workshopId;
    private Long qcWorkshopId;
    private LocalDate docDate;
    /** 一等品数（缺省取"正纱"分类数量） */
    private Integer firstGradeQty;
    private String remark;

    private List<DefectVal> defects;

    @Data
    public static class DefectVal {
        /** 正纱/AA BB纱/尾纱/辫子纱/松捻纱/紧捻纱/重纱/轻纱/断纱/多股纱/油纱/大小肚/乱纱/圈圈纱/其它 */
        private String defectType;
        private BigDecimal value;
        private String remark;
    }
}
