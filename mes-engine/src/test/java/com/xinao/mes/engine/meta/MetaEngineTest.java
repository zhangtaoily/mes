package com.xinao.mes.engine.meta;

import com.xinao.mes.engine.meta.entity.BaseQcIndicator;
import com.xinao.mes.engine.meta.entity.MetaColumnNode;
import com.xinao.mes.engine.meta.entity.MetaTableNode;
import com.xinao.mes.engine.meta.mapper.BaseQcIndicatorMapper;
import com.xinao.mes.engine.meta.mapper.MetaColumnNodeMapper;
import com.xinao.mes.engine.meta.mapper.MetaTableNodeMapper;
import com.xinao.mes.engine.meta.vo.ColumnVO;
import com.xinao.mes.engine.meta.vo.TableVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * meta 元数据：固定列 + 质检指标纵表动态列展开
 */
class MetaEngineTest {

    private MetaTableNodeMapper tableNodeMapper;
    private MetaColumnNodeMapper columnNodeMapper;
    private BaseQcIndicatorMapper indicatorMapper;
    private MetaEngine engine;
    private List<BaseQcIndicator> indicators;

    @BeforeEach
    void setUp() {
        tableNodeMapper = mock(MetaTableNodeMapper.class);
        columnNodeMapper = mock(MetaColumnNodeMapper.class);
        indicatorMapper = mock(BaseQcIndicatorMapper.class);
        engine = new MetaEngine(tableNodeMapper, columnNodeMapper, indicatorMapper);

        MetaTableNode node = new MetaTableNode();
        node.setId(1L);
        node.setNodeCode("qc_report_item");
        node.setNodeName("检验报告-指标值矩阵");
        node.setDynamicSource("QC_INDICATOR");
        when(tableNodeMapper.selectOne(any())).thenReturn(node);

        MetaColumnNode sampleNo = new MetaColumnNode();
        sampleNo.setColumnCode("sample_no");
        sampleNo.setColumnLabel("试样号");
        sampleNo.setDataType("number");
        sampleNo.setColumnUsage("LIST");
        sampleNo.setSortable(1);
        when(columnNodeMapper.selectList(any())).thenReturn(List.of(sampleNo));

        indicators = new java.util.ArrayList<>(List.of(
                indicator(1L, "每米重量", "g/m"),
                indicator(2L, "含油率", "%")));
        when(indicatorMapper.selectList(any())).thenReturn(indicators);
    }

    private BaseQcIndicator indicator(Long id, String name, String unit) {
        BaseQcIndicator i = new BaseQcIndicator();
        i.setId(id);
        i.setIndicatorName(name);
        i.setUnit(unit);
        i.setStatus(1);
        return i;
    }

    @Test
    void fixedPlusDynamicColumns() {
        TableVO table = engine.getTable("qc_report_item");
        assertEquals("qc_report_item", table.getNodeCode());
        assertEquals(3, table.getColumns().size());

        ColumnVO first = table.getColumns().get(0);
        assertEquals("sample_no", first.getColumnCode());
        assertTrue(!first.getDynamic());

        // 动态列：ind_1 = 每米重量(g/m)
        ColumnVO dynamic = table.getColumns().get(1);
        assertEquals("ind_1", dynamic.getColumnCode());
        assertEquals("每米重量(g/m)", dynamic.getColumnLabel());
        assertTrue(dynamic.getDynamic());
    }

    @Test
    void indicatorAddedThenColumnAppears() {
        // 纵表加一个指标 → 列表自动多一列
        indicators.add(indicator(3L, "不匀率", "%"));
        TableVO table = engine.getTable("qc_report_item");
        assertEquals(4, table.getColumns().size());
        assertEquals("ind_3", table.getColumns().get(3).getColumnCode());
    }

    @Test
    void indicatorRemovedThenColumnDisappears() {
        indicators.remove(1);
        TableVO table = engine.getTable("qc_report_item");
        assertEquals(2, table.getColumns().size());
    }
}
