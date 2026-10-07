package com.xinao.mes.produce.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.plan.domain.model.BaseTeam;
import com.xinao.mes.plan.domain.model.BaseWorkshop;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.infrastructure.mapper.BaseTeamMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseWorkshopMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMapper;
import com.xinao.mes.produce.domain.model.RepAutoWindingCut;
import com.xinao.mes.produce.domain.model.RepBatchOn;
import com.xinao.mes.produce.domain.model.RepFrontReport;
import com.xinao.mes.produce.domain.model.RepRebatchDetail;
import com.xinao.mes.produce.domain.model.RepRebatchLog;
import com.xinao.mes.produce.domain.model.RepStartStop;
import com.xinao.mes.produce.domain.model.RepYarnSteam;
import com.xinao.mes.produce.dto.FrontReportQueryDTO;
import com.xinao.mes.produce.dto.RebatchGenDTO;
import com.xinao.mes.produce.dto.RebatchQueryDTO;
import com.xinao.mes.produce.dto.StopQueryDTO;
import com.xinao.mes.produce.infrastructure.mapper.RepAutoWindingCutMapper;
import com.xinao.mes.produce.infrastructure.mapper.RepBatchOnMapper;
import com.xinao.mes.produce.infrastructure.mapper.RepFrontReportMapper;
import com.xinao.mes.produce.infrastructure.mapper.RepRebatchDetailMapper;
import com.xinao.mes.produce.infrastructure.mapper.RepRebatchLogMapper;
import com.xinao.mes.produce.infrastructure.mapper.RepStartStopMapper;
import com.xinao.mes.produce.infrastructure.mapper.RepYarnSteamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 现场汇报应用服务：启停/翻批日志/前纺生产情况/自络筒切疵/蒸纱信息
 */
@Service
@RequiredArgsConstructor
public class SiteReportAppService {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final RepStartStopMapper stopMapper;
    private final RepRebatchLogMapper rebatchLogMapper;
    private final RepRebatchDetailMapper rebatchDetailMapper;
    private final RepFrontReportMapper frontMapper;
    private final RepAutoWindingCutMapper cutMapper;
    private final RepYarnSteamMapper steamMapper;
    private final RepBatchOnMapper batchOnMapper;
    private final BaseWorkshopMapper workshopMapper;
    private final BaseTeamMapper teamMapper;
    private final ProdTaskMapper taskMapper;
    private final CodeEngine codeEngine;

    // ---------- 启停汇报 ----------

    public PageResult<RepStartStop> pageStops(StopQueryDTO query) {
        Page<RepStartStop> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<RepStartStop> result = stopMapper.selectPage(p, Wrappers.<RepStartStop>lambdaQuery()
                .eq(query.getWorkshopId() != null, RepStartStop::getWorkshopId, query.getWorkshopId())
                .eq(query.getEquipmentId() != null, RepStartStop::getEquipmentId, query.getEquipmentId())
                .like(StringUtils.isNotBlank(query.getStopNo()), RepStartStop::getStopNo, query.getStopNo())
                .ge(query.getReportDateBegin() != null, RepStartStop::getReportDate, query.getReportDateBegin())
                .le(query.getReportDateEnd() != null, RepStartStop::getReportDate, query.getReportDateEnd())
                .orderByDesc(RepStartStop::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Transactional
    public Long saveStop(RepStartStop stop) {
        if (stop == null) {
            throw new BizException("停机汇报不能为空");
        }
        if (stop.getId() == null) {
            stop.setStopNo(codeEngine.next("启停汇报", stop.getReportUser()));
            stopMapper.insert(stop);
        } else {
            stopMapper.updateById(stop);
        }
        return stop.getId();
    }

    @Transactional
    public void deleteStop(Long id) {
        stopMapper.deleteById(id);
    }

    // ---------- 翻批日志 ----------

    public PageResult<RepRebatchLog> pageRebatchLogs(RebatchQueryDTO query) {
        Page<RepRebatchLog> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<RepRebatchLog> result = rebatchLogMapper.selectPage(p, Wrappers.<RepRebatchLog>lambdaQuery()
                .eq(query.getWorkshopId() != null, RepRebatchLog::getWorkshopId, query.getWorkshopId())
                .eq(query.getTeamId() != null, RepRebatchLog::getTeamId, query.getTeamId())
                .orderByDesc(RepRebatchLog::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    public List<RepRebatchDetail> rebatchDetails(Long logId) {
        return rebatchDetailMapper.selectList(Wrappers.<RepRebatchDetail>lambdaQuery()
                .eq(RepRebatchDetail::getRebatchLogId, logId));
    }

    /** 生成翻批记录：按车间+班组+时段汇总上批汇报快照（标题：【车间】【班组】【时段】翻批记录） */
    @Transactional
    public Long generateRebatch(RebatchGenDTO dto) {
        if (dto.getWorkshopId() == null || dto.getTeamId() == null
                || dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new BizException("请指定车间、班组与时段");
        }
        String workshopName = nameOfWorkshop(dto.getWorkshopId());
        String teamName = nameOfTeam(dto.getTeamId());
        String period = HM.format(dto.getStartTime()) + "~" + HM.format(dto.getEndTime());
        RepRebatchLog log = new RepRebatchLog();
        log.setTitle("【" + workshopName + "】【" + teamName + "】【" + period + "】翻批记录");
        log.setWorkshopId(dto.getWorkshopId());
        log.setTeamId(dto.getTeamId());
        log.setStartTime(dto.getStartTime());
        log.setEndTime(dto.getEndTime());
        rebatchLogMapper.insert(log);

        List<RepBatchOn> ons = batchOnMapper.selectList(Wrappers.<RepBatchOn>lambdaQuery()
                .eq(RepBatchOn::getWorkshopId, dto.getWorkshopId())
                .between(RepBatchOn::getBatchOnTime, dto.getStartTime(), dto.getEndTime())
                .orderByAsc(RepBatchOn::getBatchOnTime));
        for (RepBatchOn on : ons) {
            RepRebatchDetail d = new RepRebatchDetail();
            d.setRebatchLogId(log.getId());
            d.setWorkshopId(on.getWorkshopId());
            d.setTeamId(on.getTeamId());
            d.setBatchNo(on.getBatchNo());
            d.setTraceBatchNo(on.getTraceBatchNo());
            d.setProcessId(on.getProcessId());
            d.setEquipmentId(on.getEquipmentId());
            d.setIsBatchOff(on.getIsBatchOff());
            d.setIsFinished(on.getIsFinished());
            d.setBatchOnTime(on.getBatchOnTime());
            d.setBatchOnUser(on.getCreateBy());
            d.setBatchOffTime(on.getBatchOffTime());
            d.setBatchOffUser(on.getBatchOffUser());
            ProdTask task = taskOfBatch(on.getBatchNo());
            if (task != null) {
                d.setRawMaterial(task.getRawMaterial());
                d.setProductNo(task.getProductNo());
                d.setProductSpec(task.getProductSpec());
            }
            rebatchDetailMapper.insert(d);
        }
        return log.getId();
    }

    // ---------- 前纺生产情况汇报 ----------

    public PageResult<RepFrontReport> pageFront(FrontReportQueryDTO query) {
        Page<RepFrontReport> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<RepFrontReport> result = frontMapper.selectPage(p, Wrappers.<RepFrontReport>lambdaQuery()
                .eq(query.getWorkshopId() != null, RepFrontReport::getWorkshopId, query.getWorkshopId())
                .like(StringUtils.isNotBlank(query.getBatchNo()), RepFrontReport::getBatchNo, query.getBatchNo())
                .orderByDesc(RepFrontReport::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Transactional
    public Long saveFront(RepFrontReport report) {
        if (report == null || StringUtils.isBlank(report.getBatchNo())) {
            throw new BizException("生产批号不能为空");
        }
        if (report.getId() == null) {
            frontMapper.insert(report);
        } else {
            frontMapper.updateById(report);
        }
        return report.getId();
    }

    @Transactional
    public void deleteFront(Long id) {
        frontMapper.deleteById(id);
    }

    // ---------- 自络筒切疵 ----------

    public PageResult<RepAutoWindingCut> pageCuts(FrontReportQueryDTO query) {
        Page<RepAutoWindingCut> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<RepAutoWindingCut> result = cutMapper.selectPage(p, Wrappers.<RepAutoWindingCut>lambdaQuery()
                .eq(query.getWorkshopId() != null, RepAutoWindingCut::getWorkshopId, query.getWorkshopId())
                .like(StringUtils.isNotBlank(query.getBatchNo()), RepAutoWindingCut::getBatchNo, query.getBatchNo())
                .orderByDesc(RepAutoWindingCut::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Transactional
    public Long saveCut(RepAutoWindingCut cut) {
        if (cut == null || StringUtils.isBlank(cut.getBatchNo())) {
            throw new BizException("生产批号不能为空");
        }
        if (cut.getId() == null) {
            cutMapper.insert(cut);
        } else {
            cutMapper.updateById(cut);
        }
        return cut.getId();
    }

    @Transactional
    public void deleteCut(Long id) {
        cutMapper.deleteById(id);
    }

    // ---------- 蒸纱信息（打印/接收闭环） ----------

    public PageResult<RepYarnSteam> pageSteams(FrontReportQueryDTO query) {
        Page<RepYarnSteam> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<RepYarnSteam> result = steamMapper.selectPage(p, Wrappers.<RepYarnSteam>lambdaQuery()
                .like(StringUtils.isNotBlank(query.getBatchNo()), RepYarnSteam::getBatchNo, query.getBatchNo())
                .like(StringUtils.isNotBlank(query.getTruckNo()), RepYarnSteam::getTruckNo, query.getTruckNo())
                .eq(StringUtils.isNotBlank(query.getStatus()), RepYarnSteam::getStatus, query.getStatus())
                .orderByDesc(RepYarnSteam::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Transactional
    public Long saveSteam(RepYarnSteam steam) {
        if (steam == null || StringUtils.isBlank(steam.getTruckNo())) {
            throw new BizException("车号不能为空");
        }
        if (steam.getId() == null) {
            steamMapper.insert(steam);
        } else {
            steamMapper.updateById(steam);
        }
        return steam.getId();
    }

    /** 打印：回填打印人/时间 */
    @Transactional
    public void printSteam(Long id) {
        RepYarnSteam steam = requireSteam(id);
        if (steam.getPrintTime() != null) {
            throw new BizException("蒸纱信息[" + steam.getPrintCode() + "]已打印");
        }
        steamMapper.update(null, Wrappers.<RepYarnSteam>lambdaUpdate()
                .eq(RepYarnSteam::getId, id)
                .set(RepYarnSteam::getPrintUser, currentUser())
                .set(RepYarnSteam::getPrintTime, LocalDateTime.now()));
    }

    /** 接收：回填接收人/时间（打印/接收闭环） */
    @Transactional
    public void receiveSteam(Long id) {
        RepYarnSteam steam = requireSteam(id);
        if (steam.getPrintTime() == null) {
            throw new BizException("蒸纱信息[" + steam.getPrintCode() + "]未打印，不可接收");
        }
        if (steam.getReceiveTime() != null) {
            throw new BizException("蒸纱信息[" + steam.getPrintCode() + "]已接收");
        }
        steamMapper.update(null, Wrappers.<RepYarnSteam>lambdaUpdate()
                .eq(RepYarnSteam::getId, id)
                .set(RepYarnSteam::getReceiveUser, currentUser())
                .set(RepYarnSteam::getReceiveTime, LocalDateTime.now()));
    }

    @Transactional
    public void deleteSteam(Long id) {
        steamMapper.deleteById(id);
    }

    // ---------- 内部 ----------

    private ProdTask taskOfBatch(String batchNo) {
        if (batchNo == null) {
            return null;
        }
        return taskMapper.selectList(Wrappers.<ProdTask>lambdaQuery()
                        .eq(ProdTask::getBatchNo, batchNo).last("LIMIT 1"))
                .stream().findFirst().orElse(null);
    }

    private String nameOfWorkshop(Long workshopId) {
        BaseWorkshop w = workshopMapper.selectById(workshopId);
        return w == null ? String.valueOf(workshopId) : w.getWorkshopName();
    }

    private String nameOfTeam(Long teamId) {
        BaseTeam t = teamMapper.selectById(teamId);
        return t == null ? String.valueOf(teamId) : t.getTeamName();
    }

    private RepYarnSteam requireSteam(Long id) {
        RepYarnSteam steam = steamMapper.selectById(id);
        if (steam == null) {
            throw new BizException("蒸纱信息不存在: " + id);
        }
        return steam;
    }

    private String currentUser() {
        var user = AuthContext.get();
        return user == null ? "system" : user.getUserName();
    }
}
