package com.xinao.mes.qc.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.qc.domain.model.BaseQcIndicator;
import com.xinao.mes.qc.domain.model.BaseQcScheme;
import com.xinao.mes.qc.domain.model.BaseQcSchemeItem;
import com.xinao.mes.qc.infrastructure.mapper.BaseQcIndicatorMapper;
import com.xinao.mes.qc.infrastructure.mapper.BaseQcSchemeItemMapper;
import com.xinao.mes.qc.infrastructure.mapper.BaseQcSchemeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 质检方案维护（方案 CRUD + 启停 + 指标项 + 指标下拉） */
@RestController
@RequiredArgsConstructor
public class QcSchemeController {

    private final BaseQcSchemeMapper schemeMapper;
    private final BaseQcSchemeItemMapper schemeItemMapper;
    private final BaseQcIndicatorMapper indicatorMapper;

    /** 方案分页 */
    @GetMapping("/api/base/qc-schemes")
    @RequiresPerm("base:qcScheme:list")
    public R<PageResult<BaseQcScheme>> pageSchemes(@RequestParam(required = false) String schemeCode,
                                                   @RequestParam(required = false) String schemeName,
                                                   @RequestParam(required = false) Long workshopId,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<BaseQcScheme> p = schemeMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<BaseQcScheme>lambdaQuery()
                        .like(StringUtils.isNotBlank(schemeCode), BaseQcScheme::getSchemeCode, schemeCode)
                        .like(StringUtils.isNotBlank(schemeName), BaseQcScheme::getSchemeName, schemeName)
                        .eq(workshopId != null, BaseQcScheme::getWorkshopId, workshopId)
                        .eq(status != null, BaseQcScheme::getStatus, status)
                        .orderByDesc(BaseQcScheme::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 新增方案 */
    @PostMapping("/api/base/qc-schemes")
    @RequiresPerm("base:qcScheme:list")
    @OperLog(title = "新增质检方案")
    public R<Void> saveScheme(@RequestBody BaseQcScheme scheme) {
        schemeMapper.insert(scheme);
        return R.ok();
    }

    /** 修改方案 */
    @PutMapping("/api/base/qc-schemes/{id}")
    @RequiresPerm("base:qcScheme:list")
    @OperLog(title = "修改质检方案")
    public R<Void> updateScheme(@PathVariable Long id, @RequestBody BaseQcScheme scheme) {
        scheme.setId(id);
        schemeMapper.updateById(scheme);
        return R.ok();
    }

    /** 启用/停用方案 */
    @PutMapping("/api/base/qc-schemes/{id}/status/{status}")
    @RequiresPerm("base:qcScheme:list")
    @OperLog(title = "修改质检方案状态")
    public R<Void> changeSchemeStatus(@PathVariable Long id, @PathVariable Integer status) {
        BaseQcScheme scheme = new BaseQcScheme();
        scheme.setId(id);
        scheme.setStatus(status);
        schemeMapper.updateById(scheme);
        return R.ok();
    }

    /** 删除方案（同时删除指标项） */
    @DeleteMapping("/api/base/qc-schemes/{id}")
    @RequiresPerm("base:qcScheme:list")
    @OperLog(title = "删除质检方案")
    public R<Void> deleteScheme(@PathVariable Long id) {
        schemeMapper.deleteById(id);
        schemeItemMapper.delete(Wrappers.<BaseQcSchemeItem>lambdaQuery()
                .eq(BaseQcSchemeItem::getSchemeId, id));
        return R.ok();
    }

    /** 方案指标项列表 */
    @GetMapping("/api/base/qc-schemes/{id}/items")
    @RequiresPerm("base:qcScheme:list")
    public R<List<BaseQcSchemeItem>> schemeItems(@PathVariable Long id) {
        return R.ok(schemeItemMapper.selectList(Wrappers.<BaseQcSchemeItem>lambdaQuery()
                .eq(BaseQcSchemeItem::getSchemeId, id)
                .orderByAsc(BaseQcSchemeItem::getSortNo)));
    }

    /** 新增指标项 */
    @PostMapping("/api/base/qc-schemes/{id}/items")
    @RequiresPerm("base:qcScheme:list")
    @OperLog(title = "新增方案指标项")
    public R<Void> saveSchemeItem(@PathVariable Long id, @RequestBody BaseQcSchemeItem item) {
        item.setSchemeId(id);
        schemeItemMapper.insert(item);
        return R.ok();
    }

    /** 修改指标项 */
    @PutMapping("/api/base/qc-schemes/{id}/items/{itemId}")
    @RequiresPerm("base:qcScheme:list")
    @OperLog(title = "修改方案指标项")
    public R<Void> updateSchemeItem(@PathVariable Long id, @PathVariable Long itemId,
                                    @RequestBody BaseQcSchemeItem item) {
        item.setId(itemId);
        item.setSchemeId(id);
        schemeItemMapper.updateById(item);
        return R.ok();
    }

    /** 删除指标项 */
    @DeleteMapping("/api/base/qc-schemes/{id}/items/{itemId}")
    @RequiresPerm("base:qcScheme:list")
    @OperLog(title = "删除方案指标项")
    public R<Void> deleteSchemeItem(@PathVariable Long id, @PathVariable Long itemId) {
        schemeItemMapper.deleteById(itemId);
        return R.ok();
    }

    /** 质检指标下拉（启用状态全量） */
    @GetMapping("/api/base/qc-indicators")
    public R<List<BaseQcIndicator>> indicators() {
        return R.ok(indicatorMapper.selectList(Wrappers.<BaseQcIndicator>lambdaQuery()
                .eq(BaseQcIndicator::getStatus, 1)
                .orderByAsc(BaseQcIndicator::getId)));
    }
}
