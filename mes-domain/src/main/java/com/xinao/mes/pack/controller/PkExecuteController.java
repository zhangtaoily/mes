package com.xinao.mes.pack.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.pack.application.PkExecuteAppService;
import com.xinao.mes.pack.domain.model.PkBox;
import com.xinao.mes.pack.domain.model.PkExecute;
import com.xinao.mes.pack.dto.BoxSaveDTO;
import com.xinao.mes.pack.dto.PkExecuteQueryDTO;
import com.xinao.mes.pack.vo.PkExecuteDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 包装执行（逐箱称重：五列重量恒等式 + 公重公式换算） */
@RestController
@RequestMapping("/api/pack/executes")
@RequiredArgsConstructor
public class PkExecuteController {

    private final PkExecuteAppService executeAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("pack:execute:list")
    public R<PageResult<PkExecute>> page(PkExecuteQueryDTO query) {
        return R.ok(executeAppService.page(query));
    }

    /** 详情（主表 + 箱子明细 + 汇总） */
    @GetMapping("/{id}")
    @RequiresPerm("pack:execute:list")
    public R<PkExecuteDetailVO> detail(@PathVariable Long id) {
        return R.ok(executeAppService.detail(id));
    }

    /** 逐箱称重（恒等式校验 + 公重公式） */
    @PostMapping("/{id}/boxes")
    @RequiresPerm("pack:execute:list")
    @OperLog(title = "逐箱称重")
    public R<PkBox> addBox(@PathVariable Long id, @Valid @RequestBody BoxSaveDTO dto) {
        dto.setExecuteId(id);
        return R.ok(executeAppService.addBox(dto));
    }

    /** 删箱（审计留痕 + 汇总回退） */
    @DeleteMapping("/boxes/{boxId}")
    @RequiresPerm("pack:execute:list")
    @OperLog(title = "删除包装箱")
    public R<Void> deleteBox(@PathVariable Long boxId) {
        executeAppService.deleteBox(boxId);
        return R.ok();
    }

    /** 了包 */
    @PostMapping("/{id}/finish")
    @RequiresPerm("pack:execute:list")
    @OperLog(title = "包装了包")
    public R<Void> finish(@PathVariable Long id) {
        executeAppService.finish(id);
        return R.ok();
    }
}
