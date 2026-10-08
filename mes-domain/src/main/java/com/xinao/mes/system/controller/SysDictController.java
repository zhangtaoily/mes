package com.xinao.mes.system.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.system.entity.SysDict;
import com.xinao.mes.system.entity.SysDictItem;
import com.xinao.mes.system.mapper.SysDictItemMapper;
import com.xinao.mes.system.mapper.SysDictMapper;
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

/** 字典管理（sys_dict / sys_dict_item，物理删除） */
@RestController
@RequestMapping("/api/sys/dicts")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictMapper dictMapper;
    private final SysDictItemMapper dictItemMapper;

    /** 字典类型分页 */
    @GetMapping
    @RequiresPerm("sys:dict:list")
    public R<PageResult<SysDict>> page(@RequestParam(required = false) String dictName,
                                       @RequestParam(required = false) String dictType,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<SysDict> p = dictMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<SysDict>lambdaQuery()
                        .like(StringUtils.isNotBlank(dictName), SysDict::getDictName, dictName)
                        .like(StringUtils.isNotBlank(dictType), SysDict::getDictType, dictType)
                        .eq(status != null, SysDict::getStatus, status)
                        .orderByAsc(SysDict::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 字典类型全量下拉 */
    @GetMapping("/all")
    @RequiresPerm("sys:dict:list")
    public R<List<SysDict>> listAll() {
        return R.ok(dictMapper.selectList(Wrappers.<SysDict>lambdaQuery()
                .orderByAsc(SysDict::getId)));
    }

    /** 新增字典类型 */
    @PostMapping
    @RequiresPerm("sys:dict:add")
    @OperLog(title = "新增字典类型")
    public R<Void> save(@RequestBody SysDict dict) {
        dictMapper.insert(dict);
        return R.ok();
    }

    /** 修改字典类型 */
    @PutMapping("/{id}")
    @RequiresPerm("sys:dict:edit")
    @OperLog(title = "修改字典类型")
    public R<Void> update(@PathVariable Long id, @RequestBody SysDict dict) {
        dict.setId(id);
        dictMapper.updateById(dict);
        return R.ok();
    }

    /** 删除字典类型（连带删除字典项） */
    @DeleteMapping("/{id}")
    @RequiresPerm("sys:dict:del")
    @OperLog(title = "删除字典类型")
    public R<Void> delete(@PathVariable Long id) {
        SysDict dict = dictMapper.selectById(id);
        if (dict != null) {
            dictItemMapper.delete(Wrappers.<SysDictItem>lambdaQuery()
                    .eq(SysDictItem::getDictType, dict.getDictType()));
            dictMapper.deleteById(id);
        }
        return R.ok();
    }

    /** 字典项列表（按类型） */
    @GetMapping("/{id}/items")
    @RequiresPerm("sys:dict:list")
    public R<List<SysDictItem>> items(@PathVariable Long id) {
        SysDict dict = dictMapper.selectById(id);
        if (dict == null) {
            return R.ok(List.of());
        }
        return R.ok(dictItemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getDictType, dict.getDictType())
                .orderByAsc(SysDictItem::getSortNo)));
    }

    /** 新增字典项 */
    @PostMapping("/{id}/items")
    @RequiresPerm("sys:dict:add")
    @OperLog(title = "新增字典项")
    public R<Void> saveItem(@PathVariable Long id, @RequestBody SysDictItem item) {
        SysDict dict = dictMapper.selectById(id);
        if (dict != null) {
            item.setDictType(dict.getDictType());
            dictItemMapper.insert(item);
        }
        return R.ok();
    }

    /** 修改字典项 */
    @PutMapping("/{id}/items/{itemId}")
    @RequiresPerm("sys:dict:edit")
    @OperLog(title = "修改字典项")
    public R<Void> updateItem(@PathVariable Long id, @PathVariable Long itemId,
                              @RequestBody SysDictItem item) {
        item.setId(itemId);
        dictItemMapper.updateById(item);
        return R.ok();
    }

    /** 删除字典项 */
    @DeleteMapping("/{id}/items/{itemId}")
    @RequiresPerm("sys:dict:del")
    @OperLog(title = "删除字典项")
    public R<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        dictItemMapper.deleteById(itemId);
        return R.ok();
    }
}
