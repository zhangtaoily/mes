package com.xinao.mes.produce.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.produce.domain.model.RepTruck;
import com.xinao.mes.produce.domain.model.RepTruckMove;
import com.xinao.mes.produce.dto.TruckMoveDTO;
import com.xinao.mes.produce.dto.TruckQueryDTO;
import com.xinao.mes.produce.dto.TruckSaveDTO;
import com.xinao.mes.produce.dto.TruckSplitDTO;
import com.xinao.mes.produce.infrastructure.mapper.RepTruckMapper;
import com.xinao.mes.produce.infrastructure.mapper.RepTruckMoveMapper;
import com.xinao.mes.produce.vo.TruckDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 复精梳毛球周转车应用服务（毛球在制品物流载体）
 *
 * <p>操作走统一 move 动作记移动轨迹（绑定/称重/入库/移库/出库）；
 * 车辆拆分 = 源车扣减 + 新车生成 + 双向轨迹。
 */
@Service
@RequiredArgsConstructor
public class TruckAppService {

    private final RepTruckMapper truckMapper;
    private final RepTruckMoveMapper moveMapper;

    public PageResult<RepTruck> page(TruckQueryDTO query) {
        Page<RepTruck> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<RepTruck> result = truckMapper.selectPage(p, Wrappers.<RepTruck>lambdaQuery()
                .like(StringUtils.isNotBlank(query.getTruckNo()), RepTruck::getTruckNo, query.getTruckNo())
                .like(StringUtils.isNotBlank(query.getBatchNo()), RepTruck::getBatchNo, query.getBatchNo())
                .eq(query.getWorkshopId() != null, RepTruck::getWorkshopId, query.getWorkshopId())
                .eq(StringUtils.isNotBlank(query.getStatus()), RepTruck::getStatus, query.getStatus())
                .orderByDesc(RepTruck::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    public TruckDetailVO detail(Long id) {
        RepTruck truck = require(id);
        TruckDetailVO vo = new TruckDetailVO();
        vo.setTruck(truck);
        vo.setMoves(moveMapper.selectList(Wrappers.<RepTruckMove>lambdaQuery()
                .eq(RepTruckMove::getTruckId, id).orderByDesc(RepTruckMove::getId)));
        return vo;
    }

    /** 保存（新增/空车修改） */
    @Transactional
    public Long save(TruckSaveDTO dto) {
        RepTruck truck = dto.getTruck();
        if (truck == null || StringUtils.isBlank(truck.getTruckNo())) {
            throw new BizException("车号不能为空");
        }
        if (truck.getId() == null) {
            Long exist = truckMapper.selectCount(Wrappers.<RepTruck>lambdaQuery()
                    .eq(RepTruck::getTruckNo, truck.getTruckNo()));
            if (exist != null && exist > 0) {
                throw new BizException("车号[" + truck.getTruckNo() + "]已存在");
            }
            truckMapper.insert(truck);
        } else {
            require(truck.getId());
            truckMapper.updateById(truck);
        }
        return truck.getId();
    }

    /**
     * 通用移动动作：更新车主表（位置/重量/球数/状态）+ 记移动轨迹
     * opName：绑定/称重/入库/移库/出库
     */
    @Transactional
    public void move(TruckMoveDTO dto) {
        if (dto.getTruckId() == null || StringUtils.isBlank(dto.getOpName())) {
            throw new BizException("请指定周转车与操作名称");
        }
        RepTruck truck = require(dto.getTruckId());
        String from = truck.getLocation();
        if (StringUtils.isNotBlank(dto.getToLocation())) {
            truck.setLocation(dto.getToLocation());
        }
        if (dto.getBallCount() != null) {
            truck.setBallCount(dto.getBallCount());
        }
        if (dto.getGrossWeight() != null) {
            truck.setGrossWeight(dto.getGrossWeight());
        }
        if (dto.getNetWeight() != null) {
            truck.setNetWeight(dto.getNetWeight());
        }
        if (dto.getStandardWeight() != null) {
            truck.setStandardWeight(dto.getStandardWeight());
        }
        if (dto.getMoistureStandard() != null) {
            truck.setMoistureStandard(dto.getMoistureStandard());
        }
        if (dto.getMoistureActual() != null) {
            truck.setMoistureActual(dto.getMoistureActual());
        }
        if (StringUtils.isNotBlank(dto.getBagColor())) {
            truck.setBagColor(dto.getBagColor());
        }
        if ("入库".equals(dto.getOpName()) || "出库".equals(dto.getOpName()) || "绑定".equals(dto.getOpName())) {
            truck.setStatus(dto.getOpName());
        }
        truckMapper.updateById(truck);

        RepTruckMove mv = new RepTruckMove();
        mv.setTruckId(truck.getId());
        mv.setFromLocation(from);
        mv.setToLocation(dto.getToLocation());
        mv.setScanWorkshop(dto.getScanWorkshop());
        mv.setRecombedBatchNo(dto.getRecombedBatchNo());
        mv.setSpinBatchNo(dto.getSpinBatchNo());
        mv.setBagColor(truck.getBagColor());
        mv.setBallCount(dto.getBallCount() != null ? dto.getBallCount() : truck.getBallCount());
        mv.setGrossWeight(dto.getGrossWeight() != null ? dto.getGrossWeight() : truck.getGrossWeight());
        mv.setNetWeight(dto.getNetWeight() != null ? dto.getNetWeight() : truck.getNetWeight());
        mv.setStandardWeight(dto.getStandardWeight() != null ? dto.getStandardWeight() : truck.getStandardWeight());
        mv.setMoistureStandard(dto.getMoistureStandard() != null ? dto.getMoistureStandard() : truck.getMoistureStandard());
        mv.setMoistureActual(dto.getMoistureActual() != null ? dto.getMoistureActual() : truck.getMoistureActual());
        mv.setOpName(dto.getOpName());
        mv.setMoveNetWeight(dto.getMoveNetWeight());
        mv.setMoveBallCount(dto.getMoveBallCount());
        mv.setIsFirst(dto.getIsFirst());
        mv.setIsFinished(dto.getIsFinished());
        moveMapper.insert(mv);
    }

    /** 车辆拆分：源车扣减净重/毛球数，新车承接，双向轨迹 */
    @Transactional
    public Long split(TruckSplitDTO dto) {
        if (dto.getTruckId() == null || StringUtils.isBlank(dto.getNewTruckNo())) {
            throw new BizException("请指定源车与新车号");
        }
        RepTruck source = require(dto.getTruckId());
        BigDecimal moveNet = dto.getMoveNetWeight() == null ? BigDecimal.ZERO : dto.getMoveNetWeight();
        Integer moveBalls = dto.getMoveBallCount() == null ? 0 : dto.getMoveBallCount();
        BigDecimal srcNet = source.getNetWeight() == null ? BigDecimal.ZERO : source.getNetWeight();
        Integer srcBalls = source.getBallCount() == null ? 0 : source.getBallCount();
        if (moveNet.compareTo(BigDecimal.ZERO) <= 0 && moveBalls <= 0) {
            throw new BizException("拆分净重/毛球数必须大于0");
        }
        if (moveNet.compareTo(srcNet) > 0 || moveBalls > srcBalls) {
            throw new BizException("拆分量超出源车当前量（净重" + srcNet + "，毛球" + srcBalls + "）");
        }
        // 源车扣减
        source.setNetWeight(srcNet.subtract(moveNet));
        source.setBallCount(srcBalls - moveBalls);
        truckMapper.updateById(source);
        RepTruckMove out = baseMove(source, "车辆拆分");
        out.setFromLocation(source.getLocation());
        out.setToLocation(dto.getToLocation());
        out.setMoveNetWeight(moveNet);
        out.setMoveBallCount(moveBalls);
        moveMapper.insert(out);

        // 新车承接
        RepTruck target = new RepTruck();
        target.setTruckNo(dto.getNewTruckNo());
        target.setStatus("车辆拆分");
        target.setWorkshopId(source.getWorkshopId());
        target.setBatchNo(source.getBatchNo());
        target.setRawMaterial(source.getRawMaterial());
        target.setLocation(dto.getToLocation());
        target.setBagColor(source.getBagColor());
        target.setBallCount(moveBalls);
        target.setNetWeight(moveNet);
        truckMapper.insert(target);
        RepTruckMove in = baseMove(target, "车辆拆分");
        in.setFromLocation(source.getLocation());
        in.setToLocation(dto.getToLocation());
        in.setMoveNetWeight(moveNet);
        in.setMoveBallCount(moveBalls);
        moveMapper.insert(in);
        return target.getId();
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        truckMapper.deleteById(id);
        moveMapper.delete(Wrappers.<RepTruckMove>lambdaQuery().eq(RepTruckMove::getTruckId, id));
    }

    // ---------- 内部 ----------

    private RepTruckMove baseMove(RepTruck truck, String opName) {
        RepTruckMove mv = new RepTruckMove();
        mv.setTruckId(truck.getId());
        mv.setOpName(opName);
        mv.setBagColor(truck.getBagColor());
        mv.setBallCount(truck.getBallCount());
        mv.setGrossWeight(truck.getGrossWeight());
        mv.setNetWeight(truck.getNetWeight());
        mv.setStandardWeight(truck.getStandardWeight());
        mv.setMoistureStandard(truck.getMoistureStandard());
        mv.setMoistureActual(truck.getMoistureActual());
        return mv;
    }

    private RepTruck require(Long id) {
        RepTruck truck = truckMapper.selectById(id);
        if (truck == null) {
            throw new BizException("周转车不存在: " + id);
        }
        return truck;
    }
}
