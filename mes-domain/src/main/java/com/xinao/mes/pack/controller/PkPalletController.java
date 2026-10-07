package com.xinao.mes.pack.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.pack.application.PkPalletAppService;
import com.xinao.mes.pack.domain.model.PkPallet;
import com.xinao.mes.pack.dto.PalletizeDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 组托（托号 TP... + 箱号绑定） */
@RestController
@RequestMapping("/api/pack/pallets")
@RequiredArgsConstructor
public class PkPalletController {

    private final PkPalletAppService palletAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("pack:pallet:list")
    public R<PageResult<PkPallet>> page(@RequestParam(required = false) String palletCode,
                                        @RequestParam(required = false) String batchNo,
                                        @RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(palletAppService.page(palletCode, batchNo, pageNum, pageSize));
    }

    /** 组托（返回新托号） */
    @PostMapping("/palletize")
    @RequiresPerm("pack:pallet:list")
    @OperLog(title = "组托")
    public R<String> palletize(@Valid @RequestBody PalletizeDTO dto) {
        return R.ok(palletAppService.palletize(dto));
    }
}
