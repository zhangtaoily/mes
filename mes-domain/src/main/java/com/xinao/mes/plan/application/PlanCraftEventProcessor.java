package com.xinao.mes.plan.application;

import com.xinao.mes.plan.domain.event.PlanCraftSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * plan 上下文事件监听：工艺单提交 → 自动生成生产任务
 *
 * <p>同步监听（同线程同事务）：任务未生成成功则工艺单提交整体回滚。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanCraftEventProcessor {

    private final ProdTaskAppService prodTaskAppService;

    @EventListener
    public void onCraftSubmitted(PlanCraftSubmittedEvent event) {
        log.info("工艺单[{}]已提交，自动生成生产任务", event.craftNo);
        prodTaskAppService.generateFromCraft(event);
    }
}
