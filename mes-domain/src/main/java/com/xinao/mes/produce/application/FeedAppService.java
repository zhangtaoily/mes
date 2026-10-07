package com.xinao.mes.produce.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.flow.BillStatus;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.domain.model.ProdTaskMaterial;
import com.xinao.mes.plan.domain.model.ProdTaskRoute;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMaterialMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskRouteMapper;
import com.xinao.mes.produce.domain.model.MtFeed;
import com.xinao.mes.produce.domain.model.MtFeedItem;
import com.xinao.mes.produce.dto.FeedQueryDTO;
import com.xinao.mes.produce.dto.FeedSaveDTO;
import com.xinao.mes.produce.infrastructure.mapper.MtFeedItemMapper;
import com.xinao.mes.produce.infrastructure.mapper.MtFeedMapper;
import com.xinao.mes.produce.vo.FeedDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 投料记录应用服务（聚合根 MtFeed；明细来自生产任务用料清单）
 *
 * <p>校验链：任务生效 → 任务投料未封账（投料完毕后不可再投）→ 明细本次投料不超领料剩余。
 * 联动：任务用料 fed_qty 累计 + 首道投料工序 feed_qty 累计；删除对称回退。
 */
@Service
@RequiredArgsConstructor
public class FeedAppService {

    private final MtFeedMapper feedMapper;
    private final MtFeedItemMapper itemMapper;
    private final ProdTaskMapper taskMapper;
    private final ProdTaskMaterialMapper materialMapper;
    private final ProdTaskRouteMapper routeMapper;
    private final CodeEngine codeEngine;

    public PageResult<MtFeed> page(FeedQueryDTO query) {
        Page<MtFeed> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<MtFeed> result = feedMapper.selectPage(p, Wrappers.<MtFeed>lambdaQuery()
                .like(StringUtils.isNotBlank(query.getFeedNo()), MtFeed::getFeedNo, query.getFeedNo())
                .like(StringUtils.isNotBlank(query.getBatchNo()), MtFeed::getBatchNo, query.getBatchNo())
                .like(StringUtils.isNotBlank(query.getTaskNo()), MtFeed::getTaskNo, query.getTaskNo())
                .eq(query.getWorkshopId() != null, MtFeed::getWorkshopId, query.getWorkshopId())
                .eq(query.getIsFinish() != null, MtFeed::getIsFinish, query.getIsFinish())
                .orderByDesc(MtFeed::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    public FeedDetailVO detail(Long id) {
        MtFeed feed = require(id);
        FeedDetailVO vo = new FeedDetailVO();
        vo.setFeed(feed);
        vo.setItems(itemMapper.selectList(Wrappers.<MtFeedItem>lambdaQuery()
                .eq(MtFeedItem::getFeedId, id)));
        return vo;
    }

    /** 任务用料清单（编辑页"生产任务"lookup 带出明细） */
    public List<ProdTaskMaterial> materialsOfTask(String taskNo) {
        ProdTask task = requireTask(taskNo);
        return materialMapper.selectList(Wrappers.<ProdTaskMaterial>lambdaQuery()
                .eq(ProdTaskMaterial::getTaskId, task.getId()));
    }

    /** 投料可选任务：生效且未投料完毕封账（PDA/编辑页 lookup） */
    public List<ProdTask> availableTasks() {
        List<ProdTask> effective = taskMapper.selectList(Wrappers.<ProdTask>lambdaQuery()
                .eq(ProdTask::getDocStatus, BillStatus.EFFECTIVE)
                .orderByDesc(ProdTask::getId));
        return effective.stream()
                .filter(t -> {
                    Long finished = feedMapper.selectCount(Wrappers.<MtFeed>lambdaQuery()
                            .eq(MtFeed::getTaskNo, t.getTaskNo())
                            .eq(MtFeed::getIsFinish, 1));
                    return finished == null || finished == 0;
                }).toList();
    }

    @Transactional
    public Long save(FeedSaveDTO dto) {
        MtFeed feed = dto.getFeed();
        if (feed == null || StringUtils.isBlank(feed.getTaskNo())) {
            throw new BizException("请选择生产任务");
        }
        ProdTask task = requireTask(feed.getTaskNo());
        if (!BillStatus.EFFECTIVE.equals(task.getDocStatus())) {
            throw new BizException("任务[" + task.getTaskNo() + "]状态为[" + task.getDocStatus() + "]，仅生效任务可投料");
        }
        // 投料封账校验：任务已存在"投料完毕"单据则不可再投
        Long finished = feedMapper.selectCount(Wrappers.<MtFeed>lambdaQuery()
                .eq(MtFeed::getTaskNo, task.getTaskNo())
                .eq(MtFeed::getIsFinish, 1));
        if (finished != null && finished > 0) {
            throw new BizException("任务[" + task.getTaskNo() + "]投料已完毕封账，不可再投料");
        }
        // 明细校验：本次投料 > 0 且不超领料剩余
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException("投料明细不能为空");
        }
        for (MtFeedItem item : dto.getItems()) {
            if (item.getThisFeedQty() == null || item.getThisFeedQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("原料[" + item.getRawBatchNo() + "]本次投料量必须大于0");
            }
            ProdTaskMaterial m = materialOf(task.getId(), item.getRawBatchNo());
            if (m == null) {
                throw new BizException("原料[" + item.getRawBatchNo() + "]不在任务用料清单中");
            }
            BigDecimal fed = m.getFedQty() == null ? BigDecimal.ZERO : m.getFedQty();
            BigDecimal cap = item.getPickQty() != null ? item.getPickQty() : m.getPlanFeedQty();
            if (cap != null && fed.add(item.getThisFeedQty()).compareTo(cap) > 0) {
                throw new BizException("原料[" + item.getRawBatchNo() + "]投料量超出剩余可投（已投"
                        + fed + "，上限" + cap + "）");
            }
        }
        feed.setFeedNo(codeEngine.next("投料单", feed.getTaskNo()));
        if (StringUtils.isBlank(feed.getBatchNo())) {
            feed.setBatchNo(task.getBatchNo());
        }
        if (StringUtils.isBlank(feed.getRawDetail())) {
            feed.setRawDetail(task.getRawMaterial());
        }
        if (feed.getIsFinish() == null) {
            feed.setIsFinish(0);
        }
        if (feed.getFeedTime() == null) {
            feed.setFeedTime(java.time.LocalDateTime.now());
        }
        feedMapper.insert(feed);
        for (MtFeedItem item : dto.getItems()) {
            item.setId(null);
            item.setFeedId(feed.getId());
            ProdTaskMaterial m = materialOf(task.getId(), item.getRawBatchNo());
            BigDecimal fed = m.getFedQty() == null ? BigDecimal.ZERO : m.getFedQty();
            item.setFedQty(fed.add(item.getThisFeedQty()));
            itemMapper.insert(item);
            // 任务用料已投量累计
            materialMapper.update(null, Wrappers.<ProdTaskMaterial>lambdaUpdate()
                    .eq(ProdTaskMaterial::getId, m.getId())
                    .setSql("fed_qty = IFNULL(fed_qty, 0) + " + item.getThisFeedQty()));
        }
        // 首道投料工序投料量累计
        accumulateRoute(task.getId(), feed.getFeedQty());
        return feed.getId();
    }

    /** 删除投料单：未完毕才可删；对称回退任务用料/工序投料量 */
    @Transactional
    public void delete(Long id) {
        MtFeed feed = require(id);
        if (feed.getIsFinish() != null && feed.getIsFinish() == 1) {
            throw new BizException("投料单[" + feed.getFeedNo() + "]已完毕封账，不可删除");
        }
        ProdTask task = requireTask(feed.getTaskNo());
        List<MtFeedItem> items = itemMapper.selectList(Wrappers.<MtFeedItem>lambdaQuery()
                .eq(MtFeedItem::getFeedId, id));
        for (MtFeedItem item : items) {
            ProdTaskMaterial m = materialOf(task.getId(), item.getRawBatchNo());
            if (m != null && item.getThisFeedQty() != null) {
                materialMapper.update(null, Wrappers.<ProdTaskMaterial>lambdaUpdate()
                        .eq(ProdTaskMaterial::getId, m.getId())
                        .setSql("fed_qty = IFNULL(fed_qty, 0) - " + item.getThisFeedQty()));
            }
        }
        if (feed.getFeedQty() != null) {
            rollbackRoute(task.getId(), feed.getFeedQty());
        }
        itemMapper.delete(Wrappers.<MtFeedItem>lambdaQuery().eq(MtFeedItem::getFeedId, id));
        feedMapper.deleteById(id);
    }

    // ---------- 内部 ----------

    private void accumulateRoute(Long taskId, BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ProdTaskRoute route = firstFeedRoute(taskId);
        if (route != null) {
            routeMapper.update(null, Wrappers.<ProdTaskRoute>lambdaUpdate()
                    .eq(ProdTaskRoute::getId, route.getId())
                    .setSql("feed_qty = IFNULL(feed_qty, 0) + " + qty));
        }
    }

    private void rollbackRoute(Long taskId, BigDecimal qty) {
        ProdTaskRoute route = firstFeedRoute(taskId);
        if (route != null) {
            routeMapper.update(null, Wrappers.<ProdTaskRoute>lambdaUpdate()
                    .eq(ProdTaskRoute::getId, route.getId())
                    .setSql("feed_qty = IFNULL(feed_qty, 0) - " + qty));
        }
    }

    private ProdTaskRoute firstFeedRoute(Long taskId) {
        return routeMapper.selectList(Wrappers.<ProdTaskRoute>lambdaQuery()
                        .eq(ProdTaskRoute::getTaskId, taskId)
                        .eq(ProdTaskRoute::getIsFeed, 1)
                        .orderByAsc(ProdTaskRoute::getSeq).last("LIMIT 1"))
                .stream().findFirst().orElse(null);
    }

    private ProdTaskMaterial materialOf(Long taskId, String rawBatchNo) {
        if (rawBatchNo == null) {
            return null;
        }
        return materialMapper.selectList(Wrappers.<ProdTaskMaterial>lambdaQuery()
                        .eq(ProdTaskMaterial::getTaskId, taskId)
                        .eq(ProdTaskMaterial::getRawBatchNo, rawBatchNo)
                        .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
    }

    private ProdTask requireTask(String taskNo) {
        ProdTask task = taskMapper.selectOne(Wrappers.<ProdTask>lambdaQuery()
                .eq(ProdTask::getTaskNo, taskNo).last("LIMIT 1"));
        if (task == null) {
            throw new BizException("生产任务不存在: " + taskNo);
        }
        return task;
    }

    private MtFeed require(Long id) {
        MtFeed feed = feedMapper.selectById(id);
        if (feed == null) {
            throw new BizException("投料单不存在: " + id);
        }
        return feed;
    }
}
