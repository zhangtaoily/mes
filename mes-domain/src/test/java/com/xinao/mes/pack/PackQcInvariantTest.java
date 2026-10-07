package com.xinao.mes.pack;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.flow.BillStatus;
import com.xinao.mes.engine.flow.FlowEngine;
import com.xinao.mes.engine.meta.mapper.BaseQcIndicatorMapper;
import com.xinao.mes.engine.script.FormulaService;
import com.xinao.mes.engine.script.GraalJsFormulaEngine;
import com.xinao.mes.engine.script.entity.QcFormula;
import com.xinao.mes.engine.script.mapper.QcFormulaMapper;
import com.xinao.mes.pack.application.PkExecuteAppService;
import com.xinao.mes.pack.domain.model.PkBox;
import com.xinao.mes.pack.domain.model.PkBoxDelete;
import com.xinao.mes.pack.domain.model.PkExecute;
import com.xinao.mes.pack.dto.BoxSaveDTO;
import com.xinao.mes.pack.infrastructure.mapper.PkBoxDeleteMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkBoxMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkExecuteMapper;
import com.xinao.mes.qc.application.QcReportAppService;
import com.xinao.mes.qc.domain.model.QcReport;
import com.xinao.mes.qc.domain.model.QcReportItem;
import com.xinao.mes.qc.infrastructure.mapper.BaseQcSchemeItemMapper;
import com.xinao.mes.qc.infrastructure.mapper.BaseQcSchemeMapper;
import com.xinao.mes.qc.infrastructure.mapper.QcReportItemMapper;
import com.xinao.mes.qc.infrastructure.mapper.QcReportMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M4 不变量单测：
 * 五列重量恒等式（毛=皮+净）/ 净→公重回潮公式（script 外置编辑即生效）/
 * $BQ 集合版公式算衍生指标并参与越限判定 / 作废与公示前置校验 / 了包封账
 */
class PackQcInvariantTest {

    /** V7 公重公式脚本（净→公重按回潮） */
    private static final String PUBLIC_WEIGHT_SCRIPT = """
            var calculate = function() {
                var net = Number($BQ("net"));
                var ms = Number($BQ("ms"));
                var ma = Number($BQ("ma"));
                if (isNaN(net) || isNaN(ms) || isNaN(ma)) { return null; }
                var result = net * (1 + ms / 100) / (1 + ma / 100);
                result = Math.round(result * 1000) / 1000;
                $SET("GZ", result);
                return result;
            };
            calculate();""";

    /** V7 不匀率公式脚本（集合版：$BQ 取试样值集合，极差/均值） */
    private static final String BYL_SCRIPT = """
            var calculate = function() {
                var a = $BQ(1);
                var nums = [];
                for (var i = 0; i < a.length; i++) { var n = Number(a[i]); if (!isNaN(n)) nums.push(n); }
                if (nums.length === 0) { return 0; }
                var max = Math.max.apply(null, nums);
                var min = Math.min.apply(null, nums);
                var avg = nums.reduce(function(s, v) { return s + v; }, 0) / nums.length;
                var result = avg === 0 ? 0 : (max - min) / avg * 100;
                result = Math.round(result * 100) / 100;
                $SET("BYL", result);
                return result;
            };
            calculate();""";

    private PkExecuteMapper executeMapper;
    private PkBoxMapper boxMapper;
    private PkBoxDeleteMapper boxDeleteMapper;
    private QcFormulaMapper formulaMapper;
    private FormulaService formulaService;
    private PkExecuteAppService executeService;

    private QcReportMapper reportMapper;
    private QcReportItemMapper itemMapper;
    private QcReportAppService reportService;

    @BeforeEach
    void setUp() {
        // 纯单测环境手动初始化 lambda 列名缓存（lambdaQuery 列名解析需要）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, PkExecute.class);
        TableInfoHelper.initTableInfo(assistant, PkBox.class);
        TableInfoHelper.initTableInfo(assistant, QcReport.class);
        TableInfoHelper.initTableInfo(assistant, QcReportItem.class);
        TableInfoHelper.initTableInfo(assistant, QcFormula.class);

        executeMapper = mock(PkExecuteMapper.class);
        boxMapper = mock(PkBoxMapper.class);
        boxDeleteMapper = mock(PkBoxDeleteMapper.class);
        formulaMapper = mock(QcFormulaMapper.class);
        // 真实脚本服务（GraalVM JS 沙箱可直跑；@PostConstruct 手动触发）
        GraalJsFormulaEngine engine = new GraalJsFormulaEngine();
        engine.init();
        formulaService = new FormulaService(formulaMapper, engine);
        CodeEngine codeEngine = mock(CodeEngine.class);
        when(codeEngine.next(any(), any())).thenReturn("BX2609300001");
        executeService = new PkExecuteAppService(executeMapper, boxMapper, boxDeleteMapper,
                formulaMapper, formulaService, codeEngine);

        reportMapper = mock(QcReportMapper.class);
        itemMapper = mock(QcReportItemMapper.class);
        reportService = new QcReportAppService(reportMapper, itemMapper,
                mock(BaseQcSchemeMapper.class), mock(BaseQcSchemeItemMapper.class),
                mock(BaseQcIndicatorMapper.class), formulaMapper, formulaService,
                codeEngine, mock(FlowEngine.class));
    }

    // ---------- 不变量1：五列重量恒等式（毛 = 去皮 + 净重，容差 0.01） ----------

    @Test
    void boxRejectedWhenWeightIdentityBroken() {
        when(executeMapper.selectById(1L)).thenReturn(execute(0));
        BizException ex = assertThrows(BizException.class,
                () -> executeService.addBox(box("10.000", "6.280", "2.500")));
        assertTrue(ex.getMessage().contains("五列重量恒等式不成立"));
    }

    @Test
    void boxPublicWeightComputedByMoistureFormula() {
        when(executeMapper.selectById(1L)).thenReturn(execute(0));
        when(boxMapper.selectList(any())).thenReturn(List.of());
        when(formulaMapper.selectList(any())).thenReturn(List.of(publicWeightFormula()));
        when(formulaMapper.selectById(2L)).thenReturn(publicWeightFormula());

        PkBox box = executeService.addBox(box("8.780", "6.280", "2.500"));

        // 恒等式通过；公重 = 6.28×(1+13.32%)/(1+12.88%) = 6.304（回潮公式外置脚本计算）
        assertEquals(0, new BigDecimal("6.304").compareTo(box.getPublicWeight()));
        ArgumentCaptor<PkBox> captor = ArgumentCaptor.forClass(PkBox.class);
        verify(boxMapper).insert(captor.capture());
        assertEquals("BX2609300001", captor.getValue().getBoxCode());
        assertEquals(1, captor.getValue().getSeqNo());
    }

    @Test
    void boxFormulaEditedTakesEffect() {
        when(executeMapper.selectById(1L)).thenReturn(execute(0));
        when(boxMapper.selectList(any())).thenReturn(List.of());
        when(formulaMapper.selectList(any())).thenReturn(List.of(publicWeightFormula()));
        when(formulaMapper.selectById(2L)).thenReturn(publicWeightFormula());
        // 口径变化：改脚本 公重=净重×2（FormulaService.updateScript 失效缓存，编辑即生效）
        QcFormula edited = publicWeightFormula();
        edited.setScript("""
                var calculate = function() {
                    var net = Number($BQ("net"));
                    var result = Math.round(net * 2 * 1000) / 1000;
                    $SET("GZ", result);
                    return result;
                };
                calculate();""");
        formulaService.updateScript(2L, edited.getScript());
        when(formulaMapper.selectById(2L)).thenReturn(edited);

        PkBox box = executeService.addBox(box("8.780", "6.280", "2.500"));
        assertEquals(0, new BigDecimal("12.560").compareTo(box.getPublicWeight()));
    }

    @Test
    void boxRejectedAfterPacked() {
        when(executeMapper.selectById(1L)).thenReturn(execute(1));
        BizException ex = assertThrows(BizException.class,
                () -> executeService.addBox(box("8.780", "6.280", "2.500")));
        assertTrue(ex.getMessage().contains("已了包"));
    }

    @Test
    void deleteBoxWritesAuditTrail() {
        PkBox box = new PkBox();
        box.setId(9L);
        box.setPkExecuteId(1L);
        box.setBoxCode("BX2609300001");
        box.setSeqNo(1);
        box.setGrossWeight(new BigDecimal("8.780"));
        when(executeMapper.selectById(1L)).thenReturn(execute(0));
        when(boxMapper.selectById(9L)).thenReturn(box);
        when(boxMapper.selectList(any())).thenReturn(List.of());

        executeService.deleteBox(9L);

        ArgumentCaptor<PkBoxDelete> captor = ArgumentCaptor.forClass(PkBoxDelete.class);
        verify(boxDeleteMapper).insert(captor.capture());
        assertEquals("BX2609300001", captor.getValue().getBoxCode());
        verify(boxMapper).deleteById(9L);
    }

    // ---------- 不变量2：$BQ 集合版公式算衍生指标并参与越限判定 ----------

    @Test
    void calcComputesDerivedIndicatorAndJudgesOverLimit() {
        QcReport report = new QcReport();
        report.setId(5L);
        report.setQcNo("QC20260930000001");
        report.setDocStatus(BillStatus.DRAFT);
        when(reportMapper.selectById(5L)).thenReturn(report);

        // 指标1 每米重量 5 试样 [20,22,24,26,28]（上限26 → 28 越限）+ 指标6 不匀率（上限15，公式写回）
        QcReportItem i1 = item(1L, 5, "28", "26", null);
        QcReportItem bylRow = item(6L, 1, null, "15", null);
        List<QcReportItem> items = List.of(
                item(1L, 1, "20", "26", null), item(1L, 2, "22", "26", null),
                item(1L, 3, "24", "26", null), item(1L, 4, "26", "26", null), i1,
                bylRow);
        // calc 内 selectList 依次：矩阵全量 → writeBackDerived 查指标6（须只回该指标行）→ 越限判定全量 → detail 全量
        when(itemMapper.selectList(any()))
                .thenReturn(items)
                .thenReturn(List.of(bylRow))
                .thenReturn(items);
        QcFormula bylFormula = new QcFormula();
        bylFormula.setId(1L);
        bylFormula.setFormulaName("不匀率-通用公式");
        bylFormula.setTargetIndicatorId(6L);
        bylFormula.setScript(BYL_SCRIPT);
        when(formulaMapper.selectList(any())).thenReturn(List.of(bylFormula));
        when(formulaMapper.selectById(1L)).thenReturn(bylFormula);

        reportService.calc(5L);

        // 写回：不匀率 = (28-20)/24×100 = 33.33（$BQ 集合极差/均值）
        // 越限：不匀率 33.33 > 上限15 → is_over=1；衍生指标同样参与越限判定
        ArgumentCaptor<QcReportItem> captor = ArgumentCaptor.forClass(QcReportItem.class);
        verify(itemMapper, atLeastOnce()).updateById(captor.capture());
        // 指标6 被 updateById 两次（公式写回 + 越限重判），取最后一次（写回值 + 越限标志齐全）
        QcReportItem derived = captor.getAllValues().stream()
                .filter(i -> Long.valueOf(6L).equals(i.getIndicatorId()))
                .reduce((first, second) -> second).orElseThrow();
        assertEquals("33.33", derived.getValueStr());
        assertEquals(1, derived.getIsOver());
        // 原始指标越限：每米重量试样5=28 > 上限26
        QcReportItem over = captor.getAllValues().stream()
                .filter(i -> i.getSampleNo() == 5 && Long.valueOf(1L).equals(i.getIndicatorId()))
                .findFirst().orElseThrow();
        assertEquals(1, over.getIsOver());
    }

    // ---------- 作废/公示前置校验（作废不计入统计由集成验收覆盖） ----------

    @Test
    void doubleVoidRejected() {
        QcReport report = new QcReport();
        report.setQcNo("QC20260930000001");
        report.setDocStatus(BillStatus.VOIDED);
        BizException ex = assertThrows(BizException.class, () -> report.ensureVoidable(true));
        assertTrue(ex.getMessage().contains("勿重复作废"));
    }

    @Test
    void unvoidNonVoidedRejected() {
        QcReport report = new QcReport();
        report.setQcNo("QC20260930000001");
        report.setDocStatus(BillStatus.SUBMITTED);
        BizException ex = assertThrows(BizException.class, () -> report.ensureVoidable(false));
        assertTrue(ex.getMessage().contains("无需取消作废"));
    }

    @Test
    void publicizeRejectedWhenNotSubmitted() {
        QcReport report = new QcReport();
        report.setId(5L);
        report.setQcNo("QC20260930000001");
        report.setDocStatus(BillStatus.DRAFT);
        when(reportMapper.selectById(5L)).thenReturn(report);
        BizException ex = assertThrows(BizException.class, () -> reportService.publicize(5L));
        assertTrue(ex.getMessage().contains("仅[已提交]可公示"));
    }

    // ---------- 构造 ----------

    private PkExecute execute(int isPacked) {
        PkExecute execute = new PkExecute();
        execute.setId(1L);
        execute.setExecuteNo("PC26093000001");
        execute.setIsPacked(isPacked);
        execute.setMoistureStandard(new BigDecimal("13.32"));
        execute.setMoistureActual(new BigDecimal("12.88"));
        return execute;
    }

    private BoxSaveDTO box(String gross, String net, String tare) {
        BoxSaveDTO dto = new BoxSaveDTO();
        dto.setExecuteId(1L);
        dto.setGrainCount(24);
        dto.setActualTube(24);
        dto.setGrossWeight(new BigDecimal(gross));
        dto.setNetWeight(new BigDecimal(net));
        dto.setTareWeight(new BigDecimal(tare));
        return dto;
    }

    private QcFormula publicWeightFormula() {
        QcFormula formula = new QcFormula();
        formula.setId(2L);
        formula.setFormulaName("公重换算-回潮公式");
        formula.setFormulaType("包装称重");
        formula.setTargetIndicatorId(7L);
        formula.setScript(PUBLIC_WEIGHT_SCRIPT);
        return formula;
    }

    private QcReportItem item(Long indicatorId, int sampleNo, String value, String upper, String lower) {
        QcReportItem item = new QcReportItem();
        item.setQcReportId(5L);
        item.setIndicatorId(indicatorId);
        item.setSampleNo(sampleNo);
        item.setValueStr(value);
        item.setIsOver(0);
        item.setUpperLimit(upper);
        item.setLowerLimit(lower);
        return item;
    }
}
