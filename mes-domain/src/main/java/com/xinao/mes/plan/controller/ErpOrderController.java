package com.xinao.mes.plan.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.application.ErpOrderAppService;
import com.xinao.mes.plan.domain.model.ErpK3Order;
import com.xinao.mes.plan.dto.ErpOrderQueryDTO;
import com.xinao.mes.plan.dto.ErpOrderSaveDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** K3 订单（镜像查看 + 手工录入/编辑，引入面板数据源） */
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

    /** 手工录入（订单号留空自动取号 SO） */
    @PostMapping
    @RequiresPerm("prod:k3:add")
    @OperLog(title = "手工录入K3订单")
    public R<Long> create(@Valid @RequestBody ErpOrderSaveDTO dto) {
        return R.ok(erpOrderAppService.create(dto));
    }

    /** 编辑订单（订单号不可改） */
    @PutMapping("/{id}")
    @RequiresPerm("prod:k3:add")
    @OperLog(title = "编辑K3订单")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ErpOrderSaveDTO dto) {
        erpOrderAppService.update(id, dto);
        return R.ok();
    }
}
