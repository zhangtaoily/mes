package com.xinao.mes.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.system.dto.OrgSaveDTO;
import com.xinao.mes.system.entity.SysOrg;
import com.xinao.mes.system.entity.SysUser;
import com.xinao.mes.system.mapper.SysOrgMapper;
import com.xinao.mes.system.mapper.SysUserMapper;
import com.xinao.mes.system.vo.OrgTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组织机构服务
 */
@Service
@RequiredArgsConstructor
public class SysOrgService {

    private final SysOrgMapper orgMapper;
    private final SysUserMapper userMapper;

    /** 组织树 */
    public List<OrgTreeVO> tree() {
        List<SysOrg> all = orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                .orderByAsc(SysOrg::getSortNo));
        Map<Long, List<SysOrg>> byParent = all.stream()
                .collect(Collectors.groupingBy(SysOrg::getParentId));
        return buildTree(byParent, 0L);
    }

    /** 新增组织 */
    public void save(OrgSaveDTO dto) {
        if (dto.getParentId() != 0) {
            checkOrgExists(dto.getParentId());
        }
        SysOrg org = new SysOrg();
        copy(dto, org);
        orgMapper.insert(org);
    }

    /** 修改组织（父组织不能选自己及后代） */
    public void update(OrgSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("组织ID不能为空");
        }
        checkOrgExists(dto.getId());
        if (dto.getParentId().equals(dto.getId())) {
            throw new BizException("上级组织不能选自己");
        }
        SysOrg org = new SysOrg();
        copy(dto, org);
        org.setId(dto.getId());
        orgMapper.updateById(org);
    }

    /** 删除组织（存在下级或挂载用户时拒绝） */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long childCount = orgMapper.selectCount(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getParentId, id));
        if (childCount > 0) {
            throw new BizException("存在下级组织，不能删除");
        }
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getOrgId, id));
        if (userCount > 0) {
            throw new BizException("组织下存在用户，不能删除");
        }
        orgMapper.deleteById(id);
    }

    private List<OrgTreeVO> buildTree(Map<Long, List<SysOrg>> byParent, Long parentId) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(o -> o.getSortNo() == null ? 0 : o.getSortNo()))
                .map(org -> {
                    OrgTreeVO vo = OrgTreeVO.from(org);
                    vo.setChildren(buildTree(byParent, org.getId()));
                    return vo;
                }).toList();
    }

    private void copy(OrgSaveDTO dto, SysOrg org) {
        org.setParentId(dto.getParentId());
        org.setOrgName(dto.getOrgName());
        org.setOrgType(dto.getOrgType());
        org.setSortNo(dto.getSortNo());
        org.setStatus(dto.getStatus());
    }

    private void checkOrgExists(Long id) {
        if (orgMapper.selectById(id) == null) {
            throw new BizException("组织不存在: " + id);
        }
    }
}
