package com.xinao.mes.engine.code;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.engine.code.entity.SysAutoCode;
import com.xinao.mes.engine.code.entity.SysCodeLog;
import com.xinao.mes.engine.code.mapper.SysAutoCodeMapper;
import com.xinao.mes.engine.code.mapper.SysCodeLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * code 自动编码引擎
 *
 * <p>Redis INCR 原子取号（并发无重号）+ 按日重置（key 含期间，期间切换自然归 1）
 * + DB 回写当前进度（展示/核对）+ 取号日志（断号可追溯）。
 * 新单据类型 = 往 sys_auto_code 插一条配置，不发版。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeEngine {

    private static final String KEY_PREFIX = "mes:code:";
    /** 重置型 key 的过期时间（2 天，覆盖按日重置的跨期窗口） */
    private static final Duration KEY_TTL = Duration.ofDays(2);

    private final StringRedisTemplate redis;
    private final SysAutoCodeMapper ruleMapper;
    private final SysCodeLogMapper logMapper;

    /**
     * 取号
     *
     * @param codeName 编码规则名（sys_auto_code.code_name，如 "计划工艺单"）
     * @param bizRef   业务关联（可选，用于断号追溯时定位业务单据）
     * @return 完整单号，如 PI20260930000001
     */
    public String next(String codeName, String bizRef) {
        SysAutoCode rule = ruleMapper.selectOne(
                Wrappers.<SysAutoCode>lambdaQuery().eq(SysAutoCode::getCodeName, codeName));
        if (rule == null) {
            throw new BizException("编码规则不存在: " + codeName);
        }
        String period = CodeFormatter.periodOf(rule.getDateFormat(), rule.getResetType());
        String key = KEY_PREFIX + rule.getId() + ":" + period;

        Long serial = redis.opsForValue().increment(key);
        if (serial == null) {
            throw new BizException("Redis 取号失败: " + codeName);
        }
        if (serial == 1L && rule.getResetType() != null && rule.getResetType() != 1) {
            redis.expire(key, KEY_TTL);
        }

        String fullNo = CodeFormatter.format(rule, period, serial);
        writeBack(rule, period, serial);
        appendLog(rule, period, serial, fullNo, bizRef);
        return fullNo;
    }

    /** DB 回写当前进度：同期间取大者，跨期间直接切换；回写失败不影响发号 */
    private void writeBack(SysAutoCode rule, String period, long serial) {
        try {
            // 注意 NULL 语义：current_period 为 NULL 时 <> 比较结果为 NULL 而非真，需显式 isNull 兜底
            ruleMapper.update(null, Wrappers.<SysAutoCode>lambdaUpdate()
                    .eq(SysAutoCode::getId, rule.getId())
                    .and(w -> w.eq(SysAutoCode::getCurrentPeriod, period)
                            .lt(SysAutoCode::getCurrentSerial, serial)
                            .or().ne(SysAutoCode::getCurrentPeriod, period)
                            .or().isNull(SysAutoCode::getCurrentPeriod))
                    .set(SysAutoCode::getCurrentSerial, serial)
                    .set(SysAutoCode::getCurrentPeriod, period));
        } catch (Exception e) {
            log.warn("编码进度回写失败(不影响发号) rule={}", rule.getCodeName(), e);
        }
    }

    /** 取号日志：日志缺失的流水号即断号，可按 rule+period+serial 追溯 */
    private void appendLog(SysAutoCode rule, String period, long serial, String fullNo, String bizRef) {
        try {
            SysCodeLog codeLog = new SysCodeLog();
            codeLog.setRuleId(rule.getId());
            codeLog.setCodeName(rule.getCodeName());
            codeLog.setPeriod(period);
            codeLog.setSerial(serial);
            codeLog.setFullNo(fullNo);
            codeLog.setBizRef(bizRef);
            logMapper.insert(codeLog);
        } catch (Exception e) {
            log.warn("取号日志写入失败 rule={} serial={}", rule.getCodeName(), serial, e);
        }
    }
}
