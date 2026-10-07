package com.xinao.mes.engine.meta;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.engine.meta.entity.BaseQcIndicator;
import com.xinao.mes.engine.meta.entity.MetaColumnNode;
import com.xinao.mes.engine.meta.entity.MetaTableNode;
import com.xinao.mes.engine.meta.mapper.BaseQcIndicatorMapper;
import com.xinao.mes.engine.meta.mapper.MetaColumnNodeMapper;
import com.xinao.mes.engine.meta.mapper.MetaTableNodeMapper;
import com.xinao.mes.engine.meta.vo.ColumnVO;
import com.xinao.mes.engine.meta.vo.TableVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * meta 元数据引擎（TableNode 模式）
 *
 * <p>列表列/查询项由配置驱动：meta_column_node 固定列 + 动态源展开。
 * 质检方案纵表（base_qc_indicator）增删指标后，列表页自动多列/少列——每次实时查询，天然"编辑即生效"。
 */
@Service
@RequiredArgsConstructor
public class MetaEngine {

    private final MetaTableNodeMapper tableNodeMapper;
    private final MetaColumnNodeMapper columnNodeMapper;
    private final BaseQcIndicatorMapper indicatorMapper;

    /**
     * 取表节点完整列定义（固定列 + 动态列）
     */
    public TableVO getTable(String nodeCode) {
        MetaTableNode node = tableNodeMapper.selectOne(
                Wrappers.<MetaTableNode>lambdaQuery().eq(MetaTableNode::getNodeCode, nodeCode));
        if (node == null) {
            throw new BizException("元数据节点不存在: " + nodeCode);
        }

        List<ColumnVO> columns = new ArrayList<>(fixedColumns(node.getId()));
        if ("QC_INDICATOR".equals(node.getDynamicSource())) {
            columns.addAll(indicatorColumns());
        }

        TableVO table = new TableVO();
        table.setNodeCode(node.getNodeCode());
        table.setNodeName(node.getNodeName());
        table.setTableName(node.getTableName());
        table.setColumns(columns);
        return table;
    }

    /**
     * 新增质检指标（纵表）——列表页随即多一列
     */
    public BaseQcIndicator addIndicator(String indicatorCode, String indicatorName, String unit) {
        BaseQcIndicator indicator = new BaseQcIndicator();
        indicator.setIndicatorCode(indicatorCode);
        indicator.setIndicatorName(indicatorName);
        indicator.setUnit(unit);
        indicator.setIndicatorType(1);
        indicator.setStatus(1);
        indicatorMapper.insert(indicator);
        return indicator;
    }

    /**
     * 删除质检指标——列表页随即少一列（逻辑删除）
     */
    public void removeIndicator(Long indicatorId) {
        indicatorMapper.deleteById(indicatorId);
    }

    private List<ColumnVO> fixedColumns(Long nodeId) {
        return columnNodeMapper.selectList(
                        Wrappers.<MetaColumnNode>lambdaQuery()
                                .eq(MetaColumnNode::getNodeId, nodeId)
                                .eq(MetaColumnNode::getVisible, 1)
                                .orderByAsc(MetaColumnNode::getSortNo))
                .stream()
                .map(c -> new ColumnVO(
                        c.getColumnCode(), c.getColumnLabel(),
                        c.getDataType() == null ? "string" : c.getDataType(),
                        c.getColumnUsage() == null ? "BOTH" : c.getColumnUsage(),
                        c.getQueryType() == null ? "eq" : c.getQueryType(),
                        c.getDictType(),
                        c.getSortable() != null && c.getSortable() == 1,
                        c.getWidth(),
                        false))
                .collect(java.util.stream.Collectors.toList());
    }

    /** 质检指标纵表展开为动态列（指标增删即时反映） */
    private List<ColumnVO> indicatorColumns() {
        return indicatorMapper.selectList(
                        Wrappers.<BaseQcIndicator>lambdaQuery()
                                .eq(BaseQcIndicator::getStatus, 1)
                                .orderByAsc(BaseQcIndicator::getId))
                .stream()
                .map(i -> new ColumnVO(
                        "ind_" + i.getId(),
                        i.getUnit() == null || i.getUnit().isBlank()
                                ? i.getIndicatorName()
                                : i.getIndicatorName() + "(" + i.getUnit() + ")",
                        "string", "LIST", "eq", null, false, 110, true))
                .collect(java.util.stream.Collectors.toList());
    }
}
