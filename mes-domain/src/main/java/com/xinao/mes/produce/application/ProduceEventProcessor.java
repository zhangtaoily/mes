package com.xinao.mes.produce.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.plan.domain.model.ProdTaskOrder;
import com.xinao.mes.plan.domain.model.ProdTaskRoute;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskOrderMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskRouteMapper;
import com.xinao.mes.produce.domain.event.OutputReportedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * produce 上下文事件监听：产量汇报 → 累计任务工序产出量/订单产出量
 *
 * <p>同步监听（同线程同事务）：回写失败则产量汇报整体回滚。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProduceEventProcessor {

    private final ProdTaskRouteMapper routeMapper;
    private final ProdTaskOrderMapper orderMapper;

    @EventListener
    public void onOutputReported(OutputReportedEvent event) {
        if (event.taskId == null) {
            return;
        }
        // 工序产出量累计（任务+工序维度）
        if (event.processId != null) {
            routeMapper.update(null, Wrappers.<ProdTaskRoute>lambdaUpdate()
                    .eq(ProdTaskRoute::getTaskId, event.taskId)
                    .eq(ProdTaskRoute::getProcessId, event.processId)
                    .setSql("output_qty = IFNULL(output_qty, 0) + " + event.outputQty));
        }
        // 订单产出量累计（首单）
        ProdTaskOrder first = orderMapper.selectList(Wrappers.<ProdTaskOrder>lambdaQuery()
                        .eq(ProdTaskOrder::getTaskId, event.taskId)
                        .orderByAsc(ProdTaskOrder::getId).last("LIMIT 1"))
                .stream().findFirst().orElse(null);
        if (first != null) {
            orderMapper.update(null, Wrappers.<ProdTaskOrder>lambdaUpdate()
                    .eq(ProdTaskOrder::getId, first.getId())
                    .setSql("output_qty = IFNULL(output_qty, 0) + " + event.outputQty));
        }
        log.info("产量汇报[{}]已联动任务产出量: taskId={} qty={}",
                event.outputNo, event.taskId, event.outputQty);
    }
}
