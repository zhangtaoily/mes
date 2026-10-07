package com.xinao.mes.plan.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.plan.domain.model.ErpK3Order;
import com.xinao.mes.plan.dto.ErpOrderQueryDTO;
import com.xinao.mes.plan.infrastructure.mapper.ErpK3OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** K3 订单只读镜像（M5 插件接入前手工维护，仅查询；引入量回写发生在工艺单引入） */
@Service
@RequiredArgsConstructor
public class ErpOrderAppService {

    private final ErpK3OrderMapper k3Mapper;

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
}
