package com.xinao.mes.engine.flow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.engine.flow.entity.FlowTransition;
import com.xinao.mes.engine.flow.mapper.FlowTransitionMapper;
import com.xinao.mes.engine.flow.vo.ButtonVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * flow 通用状态机
 *
 * <p>迁移全部来自 flow_transition 配置（加一条配置按钮即变化，不改代码）；
 * 按钮可见性 = f(当前状态, 角色)，由接口下发给前端；仅状态迁移，无审批流。
 * 配置缓存在内存，新增/修改配置后自动失效重载。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowEngine {

    private final FlowTransitionMapper transitionMapper;

    /** billType → 启用的迁移列表 */
    private volatile Map<String, List<FlowTransition>> cache = Collections.emptyMap();

    @PostConstruct
    public synchronized void refresh() {
        List<FlowTransition> all = transitionMapper.selectList(
                Wrappers.<FlowTransition>lambdaQuery().eq(FlowTransition::getStatus, 1));
        cache = all.stream()
                .sorted(Comparator.comparingInt(t -> t.getSortNo() == null ? 0 : t.getSortNo()))
                .collect(Collectors.groupingBy(FlowTransition::getBillType));
        log.info("flow 配置已加载: {} 个单据类型, {} 条迁移", cache.size(), all.size());
    }

    /**
     * 当前状态下可见按钮（角色过滤后）
     */
    public List<ButtonVO> buttons(String billType, String status, Set<String> roles) {
        return transitionsOf(billType).stream()
                .filter(t -> status.equals(t.getFromStatus()))
                .filter(t -> canPass(t, roles))
                .map(t -> new ButtonVO(t.getAction(), t.getActionName(), t.getToStatus()))
                .collect(Collectors.toList());
    }

    /**
     * 校验并执行迁移，返回目标状态；非法迁移直接抛业务异常
     */
    public String fire(String billType, String fromStatus, String action, Set<String> roles) {
        return transitionsOf(billType).stream()
                .filter(t -> fromStatus.equals(t.getFromStatus()))
                .filter(t -> action.equals(t.getAction()))
                .filter(t -> canPass(t, roles))
                .findFirst()
                .map(FlowTransition::getToStatus)
                .orElseThrow(() -> new BizException(
                        "单据[%s]在状态[%s]下无可用动作[%s]".formatted(billType, fromStatus, action)));
    }

    /**
     * 新增迁移配置并即时生效（不发版验收点）
     */
    public void addTransition(FlowTransition transition) {
        if (transition.getBillType() == null || transition.getFromStatus() == null
                || transition.getToStatus() == null || transition.getAction() == null) {
            throw new BizException("billType/fromStatus/toStatus/action 不能为空");
        }
        transitionMapper.insert(transition);
        refresh();
    }

    private List<FlowTransition> transitionsOf(String billType) {
        List<FlowTransition> list = cache.get(billType);
        if (list == null) {
            throw new BizException("单据类型未配置状态机: " + billType);
        }
        return list;
    }

    /** 角色校验：*=全部；ROLE_ADMIN 通配；否则角色编码交集 */
    private boolean canPass(FlowTransition transition, Set<String> roles) {
        String roleKeys = transition.getRoleKeys();
        if (roleKeys == null || roleKeys.isBlank() || "*".equals(roleKeys.trim())) {
            return true;
        }
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        if (roles.contains("ROLE_ADMIN")) {
            return true;
        }
        return Arrays.stream(roleKeys.split(","))
                .map(String::trim)
                .anyMatch(roles::contains);
    }
}
