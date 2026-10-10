package com.xinao.mes.pack.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.R;
import com.xinao.mes.pack.application.PkExecuteAppService;
import com.xinao.mes.pack.application.PkPalletAppService;
import com.xinao.mes.pack.application.PkTaskAppService;
import com.xinao.mes.pack.application.PkTraceAppService;
import com.xinao.mes.pack.domain.model.PkBox;
import com.xinao.mes.pack.domain.model.PkExecute;
import com.xinao.mes.pack.domain.model.PkLabelReprint;
import com.xinao.mes.pack.domain.model.PkTask;
import com.xinao.mes.pack.dto.BoxSaveDTO;
import com.xinao.mes.pack.dto.PalletizeDTO;
import com.xinao.mes.pack.dto.PdaScanBoxDTO;
import com.xinao.mes.pack.infrastructure.mapper.PkBoxMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkExecuteMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkTaskMapper;
import com.xinao.mes.pack.vo.PdaBoxScanVO;
import com.xinao.mes.pack.vo.PkExecuteDetailVO;
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

import java.time.LocalDate;
import java.util.List;

/**
 * PDA 包装线 BFF（M2：装箱称重 / 组托 / 筒贴箱贴打印）
 *
 * <p>与 PC 共用 pack application 层（五列重量恒等式、公重公式、重复组托拦截、
 * 了包不可再称重/删箱等不变量同源），仅做 PDA 差异补齐：
 * 包装工缺省当前用户、组托 executeId 按箱码自动解析、箱贴补打按箱码带出全部信息。
 */
@RestController
@RequestMapping("/api/pda/pack")
@RequiredArgsConstructor
public class PdaPackController {

    private final PkExecuteAppService executeAppService;
    private final PkPalletAppService palletAppService;
    private final PkTaskAppService taskAppService;
    private final PkTraceAppService traceAppService;
    private final PkExecuteMapper executeMapper;
    private final PkBoxMapper boxMapper;
    private final PkTaskMapper taskMapper;

    /** 包装执行单查询（单号/包装批号/入库批号模糊，未了包在前，最多50条） */
    @GetMapping("/executes")
    @RequiresPerm("pack:execute:list")
    public R<List<PkExecute>> executes(@RequestParam(required = false) String keyword) {
        String kw = StringUtils.isBlank(keyword) ? null : keyword.trim();
        return R.ok(executeMapper.selectList(Wrappers.<PkExecute>lambdaQuery()
                .and(kw != null, w -> w.like(PkExecute::getExecuteNo, kw)
                        .or().like(PkExecute::getPackBatchNo, kw)
                        .or().like(PkExecute::getWarehouseBatchNo, kw))
                .orderByAsc(PkExecute::getIsPacked)
                .orderByDesc(PkExecute::getId)
                .last("LIMIT 50")));
    }

    /** 执行单详情（主表 + 箱子明细 + 汇总） */
    @GetMapping("/executes/{id}")
    @RequiresPerm("pack:execute:list")
    public R<PkExecuteDetailVO> detail(@PathVariable Long id) {
        return R.ok(executeAppService.detail(id));
    }

    /** PDA 逐箱称重（恒等式校验 + 公重公式同 PC；包装工缺省当前用户） */
    @PostMapping("/boxes")
    @RequiresPerm("pack:execute:list")
    @OperLog(title = "PDA逐箱称重")
    public R<PkBox> addBox(@Valid @RequestBody BoxSaveDTO dto) {
        if (dto.getExecuteId() == null) {
            throw new BizException("包装执行单不能为空");
        }
        if (StringUtils.isBlank(dto.getPackUser())) {
            var user = AuthContext.get();
            dto.setPackUser(user == null ? "PDA" : user.getUserName());
        }
        return R.ok(executeAppService.addBox(dto));
    }

    /** PDA 删箱（审计留痕 + 汇总回退，同 PC） */
    @DeleteMapping("/boxes/{boxId}")
    @RequiresPerm("pack:execute:list")
    @OperLog(title = "PDA删箱")
    public R<Void> deleteBox(@PathVariable Long boxId) {
        executeAppService.deleteBox(boxId);
        return R.ok();
    }

    /** PDA 了包（至少一箱才可了包，同 PC） */
    @PostMapping("/executes/{id}/finish")
    @RequiresPerm("pack:execute:list")
    @OperLog(title = "PDA包装了包")
    public R<Void> finish(@PathVariable Long id) {
        executeAppService.finish(id);
        return R.ok();
    }

    /** 组托扫箱：箱码 → 箱子 + 所属执行单（不存在/已组托提前拦截） */
    @PostMapping("/scan-box")
    @RequiresPerm("pack:pallet:list")
    public R<PdaBoxScanVO> scanBox(@RequestBody PdaScanBoxDTO dto) {
        PkBox box = mustGetBox(dto == null ? null : dto.getBoxCode());
        if (StringUtils.isNotBlank(box.getPalletCode())) {
            throw new BizException("箱子[" + box.getBoxCode() + "]已组托[" + box.getPalletCode() + "]，不可重复组托");
        }
        PdaBoxScanVO vo = new PdaBoxScanVO();
        vo.setBox(box);
        vo.setExecute(mustGetExecute(box.getPkExecuteId()));
        return R.ok(vo);
    }

    /** 箱子信息查询（箱贴补打用：已组托的箱子也可查询，不拦截） */
    @GetMapping("/box-info")
    @RequiresPerm("pack:trace:list")
    public R<PdaBoxScanVO> boxInfo(@RequestParam String boxCode) {
        PkBox box = mustGetBox(boxCode);
        PdaBoxScanVO vo = new PdaBoxScanVO();
        vo.setBox(box);
        vo.setExecute(mustGetExecute(box.getPkExecuteId()));
        return R.ok(vo);
    }

    /** PDA 组托（executeId 可空：按第一个箱码自动解析；重复组托由应用层拦截） */
    @PostMapping("/palletize")
    @RequiresPerm("pack:pallet:list")
    @OperLog(title = "PDA组托")
    public R<String> palletize(@Valid @RequestBody PalletizeDTO dto) {
        if (dto.getExecuteId() == null) {
            if (dto.getBoxCodes() == null || dto.getBoxCodes().isEmpty()) {
                throw new BizException("请扫描要组托的箱子");
            }
            PkBox first = mustGetBox(dto.getBoxCodes().get(0));
            dto.setExecuteId(first.getPkExecuteId());
        }
        return R.ok(palletAppService.palletize(dto));
    }

    /** 包装任务查询（单号/生产批号/入库批号模糊，未了批在前，最多50条） */
    @GetMapping("/tasks")
    @RequiresPerm("pack:task:list")
    public R<List<PkTask>> tasks(@RequestParam(required = false) String keyword) {
        String kw = StringUtils.isBlank(keyword) ? null : keyword.trim();
        return R.ok(taskMapper.selectList(Wrappers.<PkTask>lambdaQuery()
                .and(kw != null, w -> w.like(PkTask::getTaskNo, kw)
                        .or().like(PkTask::getBatchNo, kw)
                        .or().like(PkTask::getWarehouseBatchNo, kw))
                .orderByAsc(PkTask::getIsFinished)
                .orderByDesc(PkTask::getId)
                .last("LIMIT 50")));
    }

    /** PDA 筒贴打印（留痕 pk_tube_print） */
    @PostMapping("/print-tube/{taskId}")
    @RequiresPerm("pack:task:list")
    @OperLog(title = "PDA筒贴打印")
    public R<Void> printTube(@PathVariable Long taskId, @RequestParam(required = false) Integer qty) {
        taskAppService.printTube(taskId, qty);
        return R.ok();
    }

    /** PDA 箱贴补打（扫箱码，箱子/执行/任务信息自动带出，FG 取号留痕） */
    @PostMapping("/label-reprint")
    @RequiresPerm("pack:trace:list")
    @OperLog(title = "PDA箱贴补打")
    public R<Long> labelReprint(@RequestBody PdaScanBoxDTO dto) {
        PkBox box = mustGetBox(dto == null ? null : dto.getBoxCode());
        PkExecute execute = mustGetExecute(box.getPkExecuteId());
        PkTask task = execute.getPkTaskId() == null ? null : taskMapper.selectById(execute.getPkTaskId());

        PkLabelReprint reprint = new PkLabelReprint();
        reprint.setBizType("箱贴补打");
        reprint.setBoxCode(box.getBoxCode());
        reprint.setBoxSeq(box.getSeqNo());
        reprint.setPublicWeight(box.getPublicWeight());
        reprint.setNetWeight(box.getNetWeight());
        reprint.setGrossWeight(box.getGrossWeight());
        reprint.setTareWeight(box.getTareWeight());
        reprint.setPackSpec(box.getPackSpec());
        reprint.setPalletCode(box.getPalletCode());
        reprint.setWarehouseBatchNo(execute.getWarehouseBatchNo());
        reprint.setDocDate(LocalDate.now());
        if (task != null) {
            reprint.setPkTaskId(task.getId());
            reprint.setBatchNo(task.getBatchNo());
            reprint.setK3OrderNo(task.getK3OrderNo());
            reprint.setDyeVatNo(task.getDyeVatNo());
            reprint.setProductName(task.getProductName());
            reprint.setNameEn(task.getNameEn());
            reprint.setProductSpec(task.getProductSpec());
            reprint.setCustomerName(task.getCustomerName());
            reprint.setRawDesc(task.getRawMaterial());
            reprint.setCustomerBatchNo(task.getCustomerBatchNo());
        } else {
            reprint.setDyeVatNo(execute.getDyeVatNo());
            reprint.setProductName(execute.getProductName());
            reprint.setNameEn(execute.getNameEn());
            reprint.setProductSpec(execute.getProductSpec());
        }
        return R.ok(traceAppService.reprint(reprint));
    }

    // ==================== 私有方法 ====================

    private PkBox mustGetBox(String boxCode) {
        if (StringUtils.isBlank(boxCode)) {
            throw new BizException("箱子编码不能为空");
        }
        PkBox box = boxMapper.selectOne(Wrappers.<PkBox>lambdaQuery()
                .eq(PkBox::getBoxCode, boxCode.trim())
                .last("LIMIT 1"));
        if (box == null) {
            throw new BizException("箱子[" + boxCode + "]不存在");
        }
        return box;
    }

    private PkExecute mustGetExecute(Long id) {
        PkExecute execute = executeMapper.selectById(id);
        if (execute == null) {
            throw new BizException("包装执行不存在: " + id);
        }
        return execute;
    }
}
