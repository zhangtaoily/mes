package com.xinao.mes.pack.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.pack.domain.model.PkBoxDelete;
import com.xinao.mes.pack.infrastructure.mapper.PkBoxDeleteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 箱子删除记录（删箱审计留痕，只读） */
@RestController
@RequestMapping("/api/pack/box-deletes")
@RequiredArgsConstructor
public class PkBoxDelController {

    private final PkBoxDeleteMapper boxDeleteMapper;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("pack:boxDelRecord:list")
    public R<PageResult<PkBoxDelete>> page(@RequestParam(required = false) String boxCode,
                                           @RequestParam(required = false) String palletCode,
                                           @RequestParam(required = false) String deleteUser,
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<PkBoxDelete> p = boxDeleteMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<PkBoxDelete>lambdaQuery()
                        .like(StringUtils.isNotBlank(boxCode), PkBoxDelete::getBoxCode, boxCode)
                        .like(StringUtils.isNotBlank(palletCode), PkBoxDelete::getPalletCode, palletCode)
                        .like(StringUtils.isNotBlank(deleteUser), PkBoxDelete::getDeleteUser, deleteUser)
                        .orderByDesc(PkBoxDelete::getDeleteTime).orderByDesc(PkBoxDelete::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }
}
