package com.xinao.mes.engine;

/**
 * mes-engine 易变层引擎模块（M1 实现）
 *
 * <p>四个子引擎（对应"不发版四件套"）：
 * <ul>
 *   <li>code   —— 自动编码引擎：sys_auto_code 规则配置 + Redis INCR 流水 + 按日/月重置</li>
 *   <li>flow   —— 通用状态机：编制中→已提交→生效→了批→已关闭，迁移表配置化（无审批流）</li>
 *   <li>meta   —— 元数据引擎：TableNode 列表动态列 / 质检方案纵表 / 工艺项配置</li>
 *   <li>script —— JS 脚本引擎：GraalVM JS 沙箱，$BQ/$SET 注入，质检/报产公式编辑即生效</li>
 * </ul>
 */
public final class EngineModule {

    private EngineModule() {
    }

    /** 模块版本标识，供后续 SPI 加载与缓存失效校验 */
    public static final String VERSION = "1.0.0-SNAPSHOT";
}
