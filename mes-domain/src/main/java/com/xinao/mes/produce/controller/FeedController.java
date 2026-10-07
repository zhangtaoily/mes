package com.xinao.mes.produce.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.domain.model.ProdTaskMaterial;
import com.xinao.mes.produce.application.FeedAppService;
import com.xinao.mes.produce.domain.model.MtFeed;
import com.xinao.mes.produce.dto.FeedQueryDTO;
import com.xinao.mes.produce.dto.FeedSaveDTO;
import com.xinao.mes.produce.vo.FeedDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 投料记录（现场管理；排程"投料回溯"数据源） */
@RestController
@RequestMapping("/api/site/feed-record")
@RequiredArgsConstructor
public class FeedController {

    private final FeedAppService feedAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("site:feedRecord:list")
    public R<PageResult<MtFeed>> page(FeedQueryDTO query) {
        return R.ok(feedAppService.page(query));
    }

    /** 详情（明细子表） */
    @GetMapping("/{id}")
    @RequiresPerm("site:feedRecord:list")
    public R<FeedDetailVO> detail(@PathVariable Long id) {
        return R.ok(feedAppService.detail(id));
    }

    /** 投料可选任务（生效且未投料完毕） */
    @GetMapping("/tasks")
    @RequiresPerm("site:feedRecord:list")
    public R<List<ProdTask>> availableTasks() {
        return R.ok(feedAppService.availableTasks());
    }

    /** 任务用料清单（选任务后带出明细） */
    @GetMapping("/materials")
    @RequiresPerm("site:feedRecord:list")
    public R<List<ProdTaskMaterial>> materials(@RequestParam String taskNo) {
        return R.ok(feedAppService.materialsOfTask(taskNo));
    }

    /** 保存投料 */
    @PostMapping
    @RequiresPerm("site:feedRecord:list")
    @OperLog(title = "保存投料记录")
    public R<Long> save(@Valid @RequestBody FeedSaveDTO dto) {
        return R.ok(feedAppService.save(dto));
    }

    /** 删除（未完毕才可删，对称回退累计量） */
    @DeleteMapping("/{id}")
    @RequiresPerm("site:feedRecord:list")
    @OperLog(title = "删除投料记录")
    public R<Void> delete(@PathVariable Long id) {
        feedAppService.delete(id);
        return R.ok();
    }
}
