package com.xinao.mes.engine.flow;

import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.engine.flow.entity.FlowTransition;
import com.xinao.mes.engine.flow.mapper.FlowTransitionMapper;
import com.xinao.mes.engine.flow.vo.ButtonVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * flow 状态机：配置驱动迁移 + 角色过滤 + 加配置即生效
 */
class FlowEngineTest {

    private FlowTransitionMapper mapper;
    private FlowEngine engine;
    private List<FlowTransition> config;

    @BeforeEach
    void setUp() {
        mapper = mock(FlowTransitionMapper.class);
        engine = new FlowEngine(mapper);
        config = new java.util.ArrayList<>(List.of(
                transition("qc_report", "编制中", "已提交", "submit", "提交", "ROLE_QC,ROLE_ADMIN"),
                transition("qc_report", "已提交", "编制中", "withdraw", "撤回", "ROLE_QC"),
                transition("qc_report", "已提交", "已作废", "void", "作废", "*")));
        when(mapper.selectList(any())).thenReturn(config);
        engine.refresh();
    }

    private FlowTransition transition(String billType, String from, String to, String action,
                                      String actionName, String roleKeys) {
        FlowTransition t = new FlowTransition();
        t.setBillType(billType);
        t.setFromStatus(from);
        t.setToStatus(to);
        t.setAction(action);
        t.setActionName(actionName);
        t.setRoleKeys(roleKeys);
        t.setStatus(1);
        t.setSortNo(10);
        return t;
    }

    @Test
    void buttonsFilteredByRole() {
        // 质检员：编制中可见 [提交]
        List<ButtonVO> qcButtons = engine.buttons("qc_report", "编制中", Set.of("ROLE_QC"));
        assertEquals(1, qcButtons.size());
        assertEquals("submit", qcButtons.get(0).getAction());

        // 调度员：无权限，按钮为空
        List<ButtonVO> schedButtons = engine.buttons("qc_report", "编制中", Set.of("ROLE_SCHED"));
        assertTrue(schedButtons.isEmpty());

        // 超管通配
        List<ButtonVO> adminButtons = engine.buttons("qc_report", "已提交", Set.of("ROLE_ADMIN"));
        assertEquals(2, adminButtons.size());
    }

    @Test
    void fireValidTransition() {
        String to = engine.fire("qc_report", "编制中", "submit", Set.of("ROLE_QC"));
        assertEquals("已提交", to);
    }

    @Test
    void fireInvalidActionThrows() {
        assertThrows(BizException.class,
                () -> engine.fire("qc_report", "已关闭", "submit", Set.of("ROLE_QC")));
        assertThrows(BizException.class,
                () -> engine.fire("qc_report", "编制中", "submit", Set.of("ROLE_SCHED")));
    }

    @Test
    void unknownBillTypeThrows() {
        assertThrows(BizException.class,
                () -> engine.buttons("not_exist", "编制中", Set.of("ROLE_ADMIN")));
    }

    @Test
    void addTransitionTakesEffectImmediately() {
        // 加一条"编制中→已作废"迁移（不限角色），按钮立即出现——不改代码
        config.add(transition("qc_report", "编制中", "已作废", "void", "作废", "*"));
        when(mapper.insert(any())).thenReturn(1);

        FlowTransition added = transition("qc_report", "编制中", "已作废", "void", "作废", "*");
        engine.addTransition(added);

        List<ButtonVO> buttons = engine.buttons("qc_report", "编制中", Set.of("ROLE_QC"));
        assertEquals(2, buttons.size());
        assertEquals("已作废", engine.fire("qc_report", "编制中", "void", Set.of("ROLE_QC")));
    }
}
