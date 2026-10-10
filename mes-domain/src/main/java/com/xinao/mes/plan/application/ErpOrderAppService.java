package com.xinao.mes.plan.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.plan.domain.model.ErpK3Order;
import com.xinao.mes.plan.dto.ErpOrderQueryDTO;
import com.xinao.mes.plan.dto.ErpOrderSaveDTO;
import com.xinao.mes.plan.infrastructure.mapper.ErpK3OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** K3 订单镜像（M5 插件接入前手工维护；查询+手工录入/编辑，引入量回写发生在工艺单引入） */
@Service
@RequiredArgsConstructor
public class ErpOrderAppService {

    private final ErpK3OrderMapper k3Mapper;
    private final CodeEngine codeEngine;

    public PageResult<ErpK3Order> page(ErpOrderQueryDTO query) {
        Page<ErpK3Order> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<ErpK3Order> result = k3Mapper.selectPage(p, Wrappers.<ErpK3Order>lambdaQuery()
                .like(StringUtils.isNotBlank(query.getOrderNo()), ErpK3Order::getOrderNo, query.getOrderNo())
                .like(StringUtils.isNotBlank(query.getBatchNo()), ErpK3Order::getBatchNo, query.getBatchNo())
                .like(StringUtils.isNotBlank(query.getProductName()), ErpK3Order::getProductName, query.getProductName())
                .eq(query.getIsClosed() != null, ErpK3Order::getIsClosed, query.getIsClosed())
                .orderByDesc(ErpK3Order::getOrderDate).orderByDesc(ErpK3Order::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    public ErpK3Order detail(Long id) {
        ErpK3Order order = k3Mapper.selectById(id);
        if (order == null) {
            throw new BizException("K3订单不存在: " + id);
        }
        return order;
    }

    /** 手工录入（订单号留空自动取号 SO；默认未引入/未完结/未作废） */
    @Transactional
    public Long create(ErpOrderSaveDTO dto) {
        String orderNo = StringUtils.isNotBlank(dto.getOrderNo())
                ? dto.getOrderNo().trim() : codeEngine.next("手工订单", dto.getBatchNo());
        Long dup = k3Mapper.selectCount(Wrappers.<ErpK3Order>lambdaQuery()
                .eq(ErpK3Order::getOrderNo, orderNo));
        if (dup > 0) {
            throw new BizException("订单号已存在: " + orderNo);
        }
        ErpK3Order order = new ErpK3Order();
        order.setOrderNo(orderNo);
        copySaveFields(dto, order);
        if (order.getUnit() == null) {
            order.setUnit("KG");
        }
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
        }
        order.setImportedQty(BigDecimal.ZERO);
        order.setIsClosed(0);
        order.setVoidStatus(0);
        order.setDelFlag(0);
        var user = AuthContext.get();
        order.setCreateBy(user == null ? "system" : user.getUserName());
        order.setCreateTime(LocalDateTime.now());
        order.setMesCreateTime(LocalDateTime.now());
        k3Mapper.insert(order);
        return order.getId();
    }

    /** 编辑（订单号不可改；已有引入量时订单数量不得小于已引入量） */
    @Transactional
    public void update(Long id, ErpOrderSaveDTO dto) {
        ErpK3Order db = detail(id);
        if (StringUtils.isNotBlank(dto.getOrderNo()) && !dto.getOrderNo().trim().equals(db.getOrderNo())) {
            throw new BizException("订单号不可修改");
        }
        if (db.getImportedQty() != null && db.getImportedQty().compareTo(BigDecimal.ZERO) > 0
                && dto.getK3Qty().compareTo(db.getImportedQty()) < 0) {
            throw new BizException("订单数量不得小于已引入量 " + db.getImportedQty() + " " + db.getUnit());
        }
        copySaveFields(dto, db);
        var user = AuthContext.get();
        db.setUpdateBy(user == null ? "system" : user.getUserName());
        db.setUpdateTime(LocalDateTime.now());
        db.setMesUpdateTime(LocalDateTime.now());
        k3Mapper.updateById(db);
    }

    private void copySaveFields(ErpOrderSaveDTO dto, ErpK3Order order) {
        order.setBatchNo(dto.getBatchNo());
        order.setK3Qty(dto.getK3Qty());
        order.setUnit(dto.getUnit());
        order.setOrderDate(dto.getOrderDate());
        order.setPlanStartDate(dto.getPlanStartDate());
        order.setPlanEndDate(dto.getPlanEndDate());
        order.setWorkshopName(dto.getWorkshopName());
        order.setProductName(dto.getProductName());
        order.setColorNo(dto.getColorNo());
        order.setSpinCount(dto.getSpinCount());
        order.setMaterialDetail(dto.getMaterialDetail());
        order.setMaterialSpec(dto.getMaterialSpec());
        order.setRawMaterial(dto.getRawMaterial());
        order.setCustomerName(dto.getCustomerName());
        order.setDyeType(dto.getDyeType());
    }
}
