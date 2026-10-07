package com.xinao.mes.produce.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.produce.application.BatchOnAppService;
import com.xinao.mes.produce.dto.BatchOnQueryDTO;
import com.xinao.mes.produce.dto.BatchOnSaveDTO;
import com.xinao.mes.produce.vo.BatchOnDetailVO;
import com.xinao.mes.produce.vo.BatchOnListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 上批/下批汇报（现场管理） */
@RestController
@RequestMapping("/api/site/batch-on")
@RequiredArgsConstructor
public class BatchOnController {

    private final BatchOnAppService batchOnAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("site:batchOn:list")
    public R<PageResult<BatchOnListVO>> page(BatchOnQueryDTO query) {
        return R.ok(batchOnAppService.page(query));
    }

    /** 详情（工序标识/工艺项子表） */
    @GetMapping("/{id}")
    @RequiresPerm("site:batchOn:list")
    public R<BatchOnDetailVO> detail(@PathVariable Long id) {
        return R.ok(batchOnAppService.detail(id));
    }

    /** 上批 */
    @PostMapping
    @RequiresPerm("site:batchOn:list")
    @OperLog(title = "上批汇报")
    public R<Long> onBatch(@Valid @RequestBody BatchOnSaveDTO dto) {
        return R.ok(batchOnAppService.onBatch(dto));
    }

    /** 下批 */
    @PostMapping("/{id}/off")
    @RequiresPerm("site:batchOn:list")
    @OperLog(title = "下批汇报")
    public R<Void> offBatch(@PathVariable Long id) {
        batchOnAppService.offBatch(id);
        return R.ok();
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    @RequiresPerm("site:batchOn:list")
    @OperLog(title = "删除上批记录")
    public R<Void> delete(@PathVariable Long id) {
        batchOnAppService.delete(id);
        return R.ok();
    }
}
