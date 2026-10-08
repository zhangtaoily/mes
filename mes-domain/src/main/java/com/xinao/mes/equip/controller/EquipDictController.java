package com.xinao.mes.equip.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.R;
import com.xinao.mes.equip.domain.model.EqCheckItem;
import com.xinao.mes.equip.domain.model.EqFaultType;
import com.xinao.mes.equip.domain.model.EqMaintainItem;
import com.xinao.mes.equip.domain.model.EqMaintainMethod;
import com.xinao.mes.equip.domain.model.EqPartModel;
import com.xinao.mes.equip.infrastructure.mapper.EqCheckItemMapper;
import com.xinao.mes.equip.infrastructure.mapper.EqFaultTypeMapper;
import com.xinao.mes.equip.infrastructure.mapper.EqMaintainItemMapper;
import com.xinao.mes.equip.infrastructure.mapper.EqMaintainMethodMapper;
import com.xinao.mes.equip.infrastructure.mapper.EqPartModelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 设备基础字典（5 类统一端点，type 分发）：
 * fault-types 故障分类 / maintain-items 保养项目 / check-items 点检项目 / maintain-methods 维保方式 / part-models 配件型号。
 * 字典量小，列表不分页；全部逻辑删。
 */
@RestController
@RequestMapping("/api/equip/dicts")
@RequiredArgsConstructor
public class EquipDictController {

    private final EqFaultTypeMapper faultTypeMapper;
    private final EqMaintainItemMapper maintainItemMapper;
    private final EqCheckItemMapper checkItemMapper;
    private final EqMaintainMethodMapper maintainMethodMapper;
    private final EqPartModelMapper partModelMapper;
    private final ObjectMapper objectMapper;

    /** 字典列表（keyword 按名称列模糊） */
    @GetMapping("/{type}")
    @RequiresPerm("equip:dict:list")
    public R<List<?>> list(@PathVariable String type,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Integer status) {
        return R.ok(switch (type) {
            case "fault-types" -> dictList(faultTypeMapper, "fault_name", keyword, status);
            case "maintain-items" -> dictList(maintainItemMapper, "item_name", keyword, status);
            case "check-items" -> dictList(checkItemMapper, "item_name", keyword, status);
            case "maintain-methods" -> dictList(maintainMethodMapper, "method_name", keyword, status);
            case "part-models" -> dictList(partModelMapper, "part_name", keyword, status);
            default -> throw new BizException("未知的字典类型: " + type);
        });
    }

    /** 新增 */
    @PostMapping("/{type}")
    @RequiresPerm("equip:dict:list")
    @OperLog(title = "新增设备字典")
    public R<Long> save(@PathVariable String type, @RequestBody Map<String, Object> body) {
        return R.ok(switch (type) {
            case "fault-types" -> insert(objectMapper.convertValue(body, EqFaultType.class), faultTypeMapper);
            case "maintain-items" -> insert(objectMapper.convertValue(body, EqMaintainItem.class), maintainItemMapper);
            case "check-items" -> insert(objectMapper.convertValue(body, EqCheckItem.class), checkItemMapper);
            case "maintain-methods" -> insert(objectMapper.convertValue(body, EqMaintainMethod.class), maintainMethodMapper);
            case "part-models" -> insert(objectMapper.convertValue(body, EqPartModel.class), partModelMapper);
            default -> throw new BizException("未知的字典类型: " + type);
        });
    }

    /** 修改 */
    @PutMapping("/{type}/{id}")
    @RequiresPerm("equip:dict:list")
    @OperLog(title = "修改设备字典")
    public R<Void> update(@PathVariable String type, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        switch (type) {
            case "fault-types" -> faultTypeMapper.updateById(objectMapper.convertValue(body, EqFaultType.class));
            case "maintain-items" -> maintainItemMapper.updateById(objectMapper.convertValue(body, EqMaintainItem.class));
            case "check-items" -> checkItemMapper.updateById(objectMapper.convertValue(body, EqCheckItem.class));
            case "maintain-methods" -> maintainMethodMapper.updateById(objectMapper.convertValue(body, EqMaintainMethod.class));
            case "part-models" -> partModelMapper.updateById(objectMapper.convertValue(body, EqPartModel.class));
            default -> throw new BizException("未知的字典类型: " + type);
        }
        return R.ok();
    }

    /** 删除（逻辑删） */
    @DeleteMapping("/{type}/{id}")
    @RequiresPerm("equip:dict:list")
    @OperLog(title = "删除设备字典")
    public R<Void> delete(@PathVariable String type, @PathVariable Long id) {
        switch (type) {
            case "fault-types" -> faultTypeMapper.deleteById(id);
            case "maintain-items" -> maintainItemMapper.deleteById(id);
            case "check-items" -> checkItemMapper.deleteById(id);
            case "maintain-methods" -> maintainMethodMapper.deleteById(id);
            case "part-models" -> partModelMapper.deleteById(id);
            default -> throw new BizException("未知的字典类型: " + type);
        }
        return R.ok();
    }

    private <T> List<T> dictList(BaseMapper<T> mapper, String nameCol, String keyword, Integer status) {
        QueryWrapper<T> w = new QueryWrapper<>();
        w.like(keyword != null && !keyword.isBlank(), nameCol, keyword);
        w.eq(status != null, "status", status);
        w.orderByDesc("id");
        return mapper.selectList(w);
    }

    private <T extends com.xinao.mes.common.mybatis.BaseEntity> Long insert(T entity, BaseMapper<T> mapper) {
        mapper.insert(entity);
        return entity.getId();
    }
}
