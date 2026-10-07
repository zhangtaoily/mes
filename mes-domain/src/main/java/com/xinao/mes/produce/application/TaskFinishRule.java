package com.xinao.mes.produce.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.plan.domain.model.ProdSchedule;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.domain.model.ProdTaskRoute;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskRouteMapper;
import com.xinao.mes.produce.domain.model.MtFeed;
import com.xinao.mes.produce.domain.model.RepBatchOn;
import com.xinao.mes.produce.infrastructure.mapper.MtFeedMapper;
import com.xinao.mes.produce.infrastructure.mapper.RepBatchOnMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产任务了批规则（produce 域对 plan 域的了批校验与联动）
 *
 * <p>M3 两大不变量：
 * <ul>
 *   <li>投料完毕才能了批：任务必须存在投料单且至少一张已勾"投料完毕"</li>
 *   <li>追溯批次链闭环：任务每张未删排程必须有上批记录（有排产无上批=断链，拒绝了批）</li>
 * </ul>
 * 通过后联动：活动上批自动下批闭环 + 批号标识释放（is_finished=1）+ 任务工序路线标记了批。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskFinishRule {

    private final MtFeedMapper feedMapper;
    private final RepBatchOnMapper batchOnMapper;
    private final ProdScheduleMapper scheduleMapper;
    private final ProdTaskRouteMapper routeMapper;

    /** 了批前置校验：投料完毕 + 追溯批次链闭环（拒绝路径须可测） */
    public void checkCanFinish(ProdTask task) {
        // 不变量1：投料完毕才能了批
        List<MtFeed> feeds = feedMapper.selectList(Wrappers.<MtFeed>lambdaQuery()
                .eq(MtFeed::getTaskNo, task.getTaskNo()));
        if (feeds.isEmpty()) {
            throw new BizException("任务[" + task.getTaskNo() + "]无投料记录，投料完毕才能了批");
        }
        boolean anyFinished = feeds.stream().anyMatch(f -> f.getIsFinish() != null && f.getIsFinish() == 1);
        if (!anyFinished) {
            throw new BizException("任务[" + task.getTaskNo() + "]投料未完毕，投料完毕才能了批");
        }
        // 不变量2：追溯批次链闭环——每张未删排程必须有上批记录
        for (Long scheduleId : scheduleIdsOfTask(task.getId())) {
            Long ons = batchOnMapper.selectCount(Wrappers.<RepBatchOn>lambdaQuery()
                    .eq(RepBatchOn::getScheduleId, scheduleId));
            if (ons == null || ons == 0) {
                ProdSchedule s = scheduleMapper.selectById(scheduleId);
                String no = s == null ? String.valueOf(scheduleId) : s.getScheduleNo();
                throw new BizException("排程[" + no + "]无上批记录，追溯批次链断链，拒绝了批");
            }
        }
    }

    /** 了批联动：上批记录全部闭环（活动记录自动下批）+ 释放批号标识 + 工序路线标记了批 */
    @Transactional
    public void onTaskFinished(ProdTask task) {
        List<Long> scheduleIds = scheduleIdsOfTask(task.getId());
        if (!scheduleIds.isEmpty()) {
            String user = currentUser();
            LocalDateTime now = LocalDateTime.now();
            // 全部上批记录标记了批（批号标识随之释放：占用查询条件为 is_finished=0）
            batchOnMapper.update(null, Wrappers.<RepBatchOn>lambdaUpdate()
                    .in(RepBatchOn::getScheduleId, scheduleIds)
                    .set(RepBatchOn::getIsFinished, 1));
            // 活动上批自动下批闭环
            batchOnMapper.update(null, Wrappers.<RepBatchOn>lambdaUpdate()
                    .in(RepBatchOn::getScheduleId, scheduleIds)
                    .eq(RepBatchOn::getIsBatchOff, 0)
                    .set(RepBatchOn::getIsBatchOff, 1)
                    .set(RepBatchOn::getBatchOffTime, now)
                    .set(RepBatchOn::getBatchOffUser, user));
        }
        routeMapper.update(null, Wrappers.<ProdTaskRoute>lambdaUpdate()
                .eq(ProdTaskRoute::getTaskId, task.getId())
                .set(ProdTaskRoute::getIsFinishBatch, 1));
        log.info("任务[{}]已了批：上批记录闭环+批号标识释放+工序路线标记了批", task.getTaskNo());
    }

    /**
     * 任务的未删排程（按 prod_schedule.task_id 直查全部工序排程；
     * 排程撤回即逻辑删，天然排除。不可用 prod_schedule_task——那是末道工序占用表，会漏非末道排程）
     */
    private List<Long> scheduleIdsOfTask(Long taskId) {
        return scheduleMapper.selectList(Wrappers.<ProdSchedule>lambdaQuery()
                        .eq(ProdSchedule::getTaskId, taskId))
                .stream().map(ProdSchedule::getId).distinct().toList();
    }

    private String currentUser() {
        var user = AuthContext.get();
        return user == null ? "system" : user.getUserName();
    }
}
