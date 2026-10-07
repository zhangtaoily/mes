package com.xinao.mes.plan.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.application.MachineCardAppService;
import com.xinao.mes.plan.dto.BatchIdsDTO;
import com.xinao.mes.plan.dto.CardQueryDTO;
import com.xinao.mes.plan.vo.CardDetailVO;
import com.xinao.mes.plan.vo.CardListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 上机制造卡（生成打印 + 补打留痕 + 撤回释放排程） */
@RestController
@RequestMapping("/api/plan/cards")
@RequiredArgsConstructor
public class MachineCardController {

    private final MachineCardAppService cardAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("craft:card:list")
    public R<PageResult<CardListVO>> page(CardQueryDTO query) {
        return R.ok(cardAppService.page(query));
    }

    /** 详情（A4 打印数据源） */
    @GetMapping("/{id}")
    @RequiresPerm("craft:card:list")
    public R<CardDetailVO> detail(@PathVariable Long id) {
        return R.ok(cardAppService.detail(id));
    }

    /** 打印上机制造卡：勾选排程生成并发送打印 */
    @PostMapping("/generate")
    @RequiresPerm("craft:card:list")
    @OperLog(title = "打印上机制造卡")
    public R<List<Long>> generate(@Valid @RequestBody BatchIdsDTO dto) {
        return R.ok(cardAppService.generate(dto.getIds()));
    }

    /** 补打（留痕），返回累计打印次数 */
    @PutMapping("/{id}/print")
    @RequiresPerm("craft:card:list")
    @OperLog(title = "补打制造卡")
    public R<Integer> print(@PathVariable Long id) {
        return R.ok(cardAppService.print(id));
    }

    /** 撤回（对应排程恢复可重排） */
    @PutMapping("/{id}/withdraw")
    @RequiresPerm("craft:card:list")
    @OperLog(title = "撤回制造卡")
    public R<Void> withdraw(@PathVariable Long id) {
        cardAppService.withdraw(id);
        return R.ok();
    }
}
