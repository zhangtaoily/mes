package com.xinao.mes.plan.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.application.ErpOrderAppService;
import com.xinao.mes.plan.domain.model.ErpK3Order;
import com.xinao.mes.plan.dto.ErpOrderQueryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** K3 订单查看（只读镜像 + 引入面板数据源） */
@RestController
@RequestMapping("/api/plan/erp-orders")
@RequiredArgsConstructor
public class ErpOrderController {

    private final ErpOrderAppService erpOrderAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("prod:k3:list")
    public R<PageResult<ErpK3Order>> page(ErpOrderQueryDTO query) {
        return R.ok(erpOrderAppService.page(query));
    }

    /** 详情 */
    @GetMapping("/{id}")
    @RequiresPerm("prod:k3:list")
    public R<ErpK3Order> detail(@PathVariable Long id) {
        return R.ok(erpOrderAppService.detail(id));
    }
}
