package com.xinao.mes.pack.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.pack.domain.model.PkErpIn;
import com.xinao.mes.pack.domain.model.PkLabelReprint;
import com.xinao.mes.pack.domain.model.PkTubePrint;
import com.xinao.mes.pack.domain.model.PkWmsIn;
import com.xinao.mes.pack.infrastructure.mapper.PkErpInMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkLabelReprintMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkTubePrintMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkWmsInMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 包装追溯应用服务（箱贴补打 / 筒贴打印记录 / WMS / ERP 入库链路查询） */
@Service
@RequiredArgsConstructor
public class PkTraceAppService {

    private final PkLabelReprintMapper labelReprintMapper;
    private final PkTubePrintMapper tubePrintMapper;
    private final PkWmsInMapper wmsInMapper;
    private final PkErpInMapper erpInMapper;
    private final CodeEngine codeEngine;

    /** 箱贴补打列表 */
    public PageResult<PkLabelReprint> labelReprints(String docNo, String warehouseBatchNo, int pageNum, int pageSize) {
        Page<PkLabelReprint> p = Page.of(pageNum, pageSize);
        Page<PkLabelReprint> result = labelReprintMapper.selectPage(p, Wrappers.<PkLabelReprint>lambdaQuery()
                .like(StringUtils.isNotBlank(docNo), PkLabelReprint::getDocNo, docNo)
                .like(StringUtils.isNotBlank(warehouseBatchNo), PkLabelReprint::getWarehouseBatchNo, warehouseBatchNo)
                .orderByDesc(PkLabelReprint::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 箱贴补打留痕（doc_no=FG...） */
    @Transactional
    public Long reprint(PkLabelReprint reprint) {
        if (reprint.getDocNo() == null || reprint.getDocNo().isBlank()) {
            reprint.setDocNo(codeEngine.next("箱贴补打", reprint.getWarehouseBatchNo()));
        }
        if (reprint.getBizType() == null || reprint.getBizType().isBlank()) {
            reprint.setBizType("箱贴补打");
        }
        reprint.setPrintTime(LocalDateTime.now());
        labelReprintMapper.insert(reprint);
        return reprint.getId();
    }

    /** 筒贴打印记录（打印量统计与人员追溯） */
    public PageResult<PkTubePrint> tubePrints(String batchNo, String printUser, int pageNum, int pageSize) {
        Page<PkTubePrint> p = Page.of(pageNum, pageSize);
        Page<PkTubePrint> result = tubePrintMapper.selectPage(p, Wrappers.<PkTubePrint>lambdaQuery()
                .like(StringUtils.isNotBlank(batchNo), PkTubePrint::getBatchNo, batchNo)
                .like(StringUtils.isNotBlank(printUser), PkTubePrint::getPrintUser, printUser)
                .orderByDesc(PkTubePrint::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** WMS 入库记录（包装→组托→WMS 链路查询） */
    public PageResult<PkWmsIn> wmsIn(String warehouseBatchNo, int pageNum, int pageSize) {
        Page<PkWmsIn> p = Page.of(pageNum, pageSize);
        Page<PkWmsIn> result = wmsInMapper.selectPage(p, Wrappers.<PkWmsIn>lambdaQuery()
                .like(StringUtils.isNotBlank(warehouseBatchNo), PkWmsIn::getWarehouseBatchNo, warehouseBatchNo)
                .orderByDesc(PkWmsIn::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** ERP 入库记录（WMS→ERP 链路查询） */
    public PageResult<PkErpIn> erpIn(String warehouseBatchNo, int pageNum, int pageSize) {
        Page<PkErpIn> p = Page.of(pageNum, pageSize);
        Page<PkErpIn> result = erpInMapper.selectPage(p, Wrappers.<PkErpIn>lambdaQuery()
                .like(StringUtils.isNotBlank(warehouseBatchNo), PkErpIn::getWarehouseBatchNo, warehouseBatchNo)
                .orderByDesc(PkErpIn::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }
}
