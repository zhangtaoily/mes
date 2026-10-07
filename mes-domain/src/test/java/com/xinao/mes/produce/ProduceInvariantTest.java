package com.xinao.mes.produce;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.flow.BillStatus;
import com.xinao.mes.plan.domain.model.ProdSchedule;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.domain.model.ProdTaskMaterial;
import com.xinao.mes.plan.domain.model.ProdTaskRoute;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMaterialMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskRouteMapper;
import com.xinao.mes.produce.application.FeedAppService;
import com.xinao.mes.produce.application.TaskFinishRule;
import com.xinao.mes.produce.domain.model.MtFeed;
import com.xinao.mes.produce.domain.model.MtFeedItem;
import com.xinao.mes.produce.domain.model.RepBatchOn;
import com.xinao.mes.produce.dto.FeedSaveDTO;
import com.xinao.mes.produce.infrastructure.mapper.MtFeedItemMapper;
import com.xinao.mes.produce.infrastructure.mapper.MtFeedMapper;
import com.xinao.mes.produce.infrastructure.mapper.RepBatchOnMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M3 不变量单测：
 * 投料完毕才能了批 / 追溯批次链断链拒绝了批 / 了批联动闭环 / 下批前置校验
 */
class ProduceInvariantTest {

    private MtFeedMapper feedMapper;
    private RepBatchOnMapper batchOnMapper;
    private ProdScheduleMapper scheduleMapper;
    private ProdTaskRouteMapper routeMapper;
    private TaskFinishRule rule;

    @BeforeEach
    void setUp() {
        // 纯单测环境无 MyBatis-Plus 启动流程，手动初始化实体的 lambda 列名缓存（LambdaUpdateWrapper.set 需要）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, RepBatchOn.class);
        TableInfoHelper.initTableInfo(assistant, ProdTaskRoute.class);
        feedMapper = mock(MtFeedMapper.class);
        batchOnMapper = mock(RepBatchOnMapper.class);
        scheduleMapper = mock(ProdScheduleMapper.class);
        routeMapper = mock(ProdTaskRouteMapper.class);
        rule = new TaskFinishRule(feedMapper, batchOnMapper, scheduleMapper, routeMapper);
    }

    // ---------- 不变量1：投料完毕才能了批 ----------

    @Test
    void finishRejectedWhenNoFeedRecords() {
        when(feedMapper.selectList(any())).thenReturn(List.of());
        BizException ex = assertThrows(BizException.class, () -> rule.checkCanFinish(task()));
        assertTrue(ex.getMessage().contains("无投料记录"));
    }

    @Test
    void finishRejectedWhenFeedNotFinished() {
        when(feedMapper.selectList(any())).thenReturn(List.of(feed(0)));
        BizException ex = assertThrows(BizException.class, () -> rule.checkCanFinish(task()));
        assertTrue(ex.getMessage().contains("投料未完毕"));
    }

    // ---------- 不变量2：追溯批次链断链拒绝了批 ----------

    @Test
    void finishRejectedWhenTraceChainBroken() {
        when(feedMapper.selectList(any())).thenReturn(List.of(feed(1)));
        // 两张排程：101 有上批、102 无上批（断链）
        when(scheduleMapper.selectList(any())).thenReturn(List.of(schedule(101L, "PSC20260930000001"), schedule(102L, "PSC20260930000002")));
        when(batchOnMapper.selectCount(any())).thenReturn(1L, 0L);
        when(scheduleMapper.selectById(102L)).thenReturn(schedule(102L, "PSC20260930000002"));
        BizException ex = assertThrows(BizException.class, () -> rule.checkCanFinish(task()));
        assertTrue(ex.getMessage().contains("追溯批次链断链"));
        assertTrue(ex.getMessage().contains("PSC20260930000002"));
    }

    @Test
    void finishAllowedWhenFeedFinishedAndChainClosed() {
        when(feedMapper.selectList(any())).thenReturn(List.of(feed(0), feed(1)));
        when(scheduleMapper.selectList(any())).thenReturn(List.of(schedule(101L, "PSC20260930000001"), schedule(102L, "PSC20260930000002")));
        when(batchOnMapper.selectCount(any())).thenReturn(2L);
        assertDoesNotThrow(() -> rule.checkCanFinish(task()));
    }

    // ---------- 了批联动：上批闭环 + 批号标识释放 + 路线标记 ----------

    @Test
    void onTaskFinishedClosesBatchOnsAndMarksRoutes() {
        when(scheduleMapper.selectList(any())).thenReturn(List.of(schedule(101L, "PSC20260930000001")));
        rule.onTaskFinished(task());
        // 上批两连更：全部 is_finished=1（释放批号标识）+ 活动记录自动下批闭环
        verify(batchOnMapper, times(2)).update(isNull(), any());
        // 任务工序路线标记了批
        verify(routeMapper, times(1)).update(isNull(), any());
    }

    @Test
    void onTaskFinishedSkipsBatchOnWhenNoSchedule() {
        when(scheduleMapper.selectList(any())).thenReturn(List.of());
        rule.onTaskFinished(task());
        // 无排程不触达上批记录，仅标记路线
        verify(batchOnMapper, times(0)).update(isNull(), any());
        verify(routeMapper, times(1)).update(isNull(), any());
    }

    // ---------- 投料拒绝路径（校验链：任务生效 → 未封账 → 明细不超剩余） ----------

    @Test
    void feedRejectedWhenTaskNotEffective() {
        ProdTaskMapper taskMapper = mock(ProdTaskMapper.class);
        ProdTask draft = task();
        draft.setDocStatus(BillStatus.DRAFT);
        when(taskMapper.selectOne(any())).thenReturn(draft);
        FeedAppService service = new FeedAppService(feedMapper, mock(MtFeedItemMapper.class),
                taskMapper, mock(ProdTaskMaterialMapper.class), mock(ProdTaskRouteMapper.class), mock(CodeEngine.class));
        BizException ex = assertThrows(BizException.class, () -> service.save(feedDTO("600")));
        assertTrue(ex.getMessage().contains("仅生效任务可投料"));
    }

    @Test
    void feedRejectedWhenOverRemaining() {
        ProdTaskMapper taskMapper = mock(ProdTaskMapper.class);
        ProdTaskMaterialMapper materialMapper = mock(ProdTaskMaterialMapper.class);
        // 任务生效、无封账单；原料已投1500 上限2000，本次600 超剩余500
        when(taskMapper.selectOne(any())).thenReturn(task());
        when(feedMapper.selectCount(any())).thenReturn(0L);
        ProdTaskMaterial m = new ProdTaskMaterial();
        m.setId(1L);
        m.setTaskId(99L);
        m.setRawBatchNo("RL26090001");
        m.setPlanFeedQty(new BigDecimal("2000"));
        m.setFedQty(new BigDecimal("1500"));
        when(materialMapper.selectList(any())).thenReturn(List.of(m));

        FeedAppService service = new FeedAppService(feedMapper, mock(MtFeedItemMapper.class),
                taskMapper, materialMapper, mock(ProdTaskRouteMapper.class), mock(CodeEngine.class));
        BizException ex = assertThrows(BizException.class, () -> service.save(feedDTO("600")));
        assertTrue(ex.getMessage().contains("超出剩余可投"));
        // 剩余恰好等于本次投料量时通过（1500+500=2000）
        assertDoesNotThrow(() -> service.save(feedDTO("500")));
    }

    // ---------- 下批前置校验（纯领域对象） ----------

    @Test
    void offBatchRejectedWhenAlreadyOff() {
        RepBatchOn off = new RepBatchOn();
        off.setBatchOnNo("SR20260930000001");
        off.setIsBatchOff(1);
        BizException ex = assertThrows(BizException.class, off::ensureOffable);
        assertTrue(ex.getMessage().contains("已下批"));

        RepBatchOn active = new RepBatchOn();
        active.setBatchOnNo("SR20260930000002");
        active.setIsBatchOff(0);
        assertDoesNotThrow(active::ensureOffable);
    }

    // ---------- 构造 ----------

    private ProdTask task() {
        ProdTask t = new ProdTask();
        t.setId(99L);
        t.setTaskNo("PT20260930000001");
        t.setDocStatus(BillStatus.EFFECTIVE);
        t.setPlanQty(new BigDecimal("3000"));
        return t;
    }

    private MtFeed feed(int isFinish) {
        MtFeed f = new MtFeed();
        f.setFeedNo("MF20260930000001");
        f.setTaskNo("PT20260930000001");
        f.setIsFinish(isFinish);
        return f;
    }

    private ProdSchedule schedule(Long id, String scheduleNo) {
        ProdSchedule s = new ProdSchedule();
        s.setId(id);
        s.setScheduleNo(scheduleNo);
        s.setTaskId(99L);
        return s;
    }

    /** 投料入参：单条明细 RL26090001（上限2000） */
    private FeedSaveDTO feedDTO(String thisFeedQty) {
        FeedSaveDTO dto = new FeedSaveDTO();
        MtFeed feed = new MtFeed();
        feed.setTaskNo("PT20260930000001");
        feed.setFeedQty(new BigDecimal(thisFeedQty));
        dto.setFeed(feed);
        MtFeedItem item = new MtFeedItem();
        item.setRawBatchNo("RL26090001");
        item.setPickQty(new BigDecimal("2000"));
        item.setThisFeedQty(new BigDecimal(thisFeedQty));
        dto.setItems(List.of(item));
        return dto;
    }
}
