package com.xinao.mes.produce.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.produce.application.TruckAppService;
import com.xinao.mes.produce.domain.model.RepTruck;
import com.xinao.mes.produce.dto.TruckMoveDTO;
import com.xinao.mes.produce.dto.TruckQueryDTO;
import com.xinao.mes.produce.dto.TruckSaveDTO;
import com.xinao.mes.produce.dto.TruckSplitDTO;
import com.xinao.mes.produce.vo.TruckDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 复精梳毛球周转车（绑定/称重/入库/移库/出库 + 拆分，全程移动轨迹） */
@RestController
@RequestMapping("/api/site/truck")
@RequiredArgsConstructor
public class TruckController {

    private final TruckAppService truckAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("site:truck:list")
    public R<PageResult<RepTruck>> page(TruckQueryDTO query) {
        return R.ok(truckAppService.page(query));
    }

    /** 详情（含移动轨迹） */
    @GetMapping("/{id}")
    @RequiresPerm("site:truck:list")
    public R<TruckDetailVO> detail(@PathVariable Long id) {
        return R.ok(truckAppService.detail(id));
    }

    /** 保存（新增/空车修改，车号唯一） */
    @PostMapping
    @RequiresPerm("site:truck:list")
    @OperLog(title = "保存周转车")
    public R<Long> save(@RequestBody TruckSaveDTO dto) {
        return R.ok(truckAppService.save(dto));
    }

    /** 通用移动动作：绑定/称重/入库/移库/出库 */
    @PostMapping("/move")
    @RequiresPerm("site:truck:list")
    @OperLog(title = "周转车移动")
    public R<Void> move(@RequestBody TruckMoveDTO dto) {
        truckAppService.move(dto);
        return R.ok();
    }

    /** 车辆拆分：源车扣减 + 新车承接（双向轨迹） */
    @PostMapping("/split")
    @RequiresPerm("site:truck:list")
    @OperLog(title = "周转车拆分")
    public R<Long> split(@RequestBody TruckSplitDTO dto) {
        return R.ok(truckAppService.split(dto));
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    @RequiresPerm("site:truck:list")
    @OperLog(title = "删除周转车")
    public R<Void> delete(@PathVariable Long id) {
        truckAppService.delete(id);
        return R.ok();
    }
}
