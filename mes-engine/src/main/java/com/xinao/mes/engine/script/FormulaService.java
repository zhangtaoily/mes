package com.xinao.mes.engine.script;

import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.engine.script.entity.QcFormula;
import com.xinao.mes.engine.script.mapper.QcFormulaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公式服务：脚本存 DB（qc_formula），短 TTL 缓存 + 主动失效 —— 编辑即生效，不发版
 */
@Service
@RequiredArgsConstructor
public class FormulaService {

    private static final long TTL_MS = 5_000;

    private final QcFormulaMapper formulaMapper;
    private final FormulaEngine engine;

    private record CacheEntry(QcFormula formula, long loadedAt) {
    }

    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 按公式ID执行（values: 指标ID → 检验值）
     */
    public FormulaResult evalById(Long formulaId, Map<Object, Object> values) {
        return engine.eval(load(formulaId).getScript(), values);
    }

    /**
     * 编辑脚本，立即失效缓存
     */
    public QcFormula updateScript(Long formulaId, String script) {
        QcFormula formula = formulaMapper.selectById(formulaId);
        if (formula == null) {
            throw new BizException("公式不存在: " + formulaId);
        }
        formula.setScript(script);
        formulaMapper.updateById(formula);
        cache.remove(formulaId);
        return formula;
    }

    public QcFormula getById(Long formulaId) {
        return load(formulaId);
    }

    private QcFormula load(Long formulaId) {
        CacheEntry entry = cache.get(formulaId);
        long now = System.currentTimeMillis();
        if (entry == null || now - entry.loadedAt() > TTL_MS) {
            QcFormula formula = formulaMapper.selectById(formulaId);
            if (formula == null) {
                throw new BizException("公式不存在: " + formulaId);
            }
            cache.put(formulaId, new CacheEntry(formula, now));
            return formula;
        }
        return entry.formula();
    }
}
