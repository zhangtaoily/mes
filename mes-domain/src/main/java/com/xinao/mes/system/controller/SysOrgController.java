package com.xinao.mes.system.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.R;
import com.xinao.mes.system.dto.OrgSaveDTO;
import com.xinao.mes.system.service.SysOrgService;
import com.xinao.mes.system.vo.OrgTreeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 组织机构接口
 */
@RestController
@RequestMapping("/api/sys/orgs")
@RequiredArgsConstructor
public class SysOrgController {

    private final SysOrgService orgService;

    /** 组织树（登录即可查看） */
    @GetMapping("/tree")
    public R<List<OrgTreeVO>> tree() {
        return R.ok(orgService.tree());
    }

    /** 新增组织 */
    @PostMapping
    @RequiresPerm("sys:org:add")
    @OperLog(title = "新增组织")
    public R<Void> save(@Valid @RequestBody OrgSaveDTO dto) {
        orgService.save(dto);
        return R.ok();
    }

    /** 修改组织 */
    @PutMapping("/{id}")
    @RequiresPerm("sys:org:edit")
    @OperLog(title = "修改组织")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody OrgSaveDTO dto) {
        dto.setId(id);
        orgService.update(dto);
        return R.ok();
    }

    /** 删除组织 */
    @DeleteMapping("/{id}")
    @RequiresPerm("sys:org:del")
    @OperLog(title = "删除组织")
    public R<Void> delete(@PathVariable Long id) {
        orgService.delete(id);
        return R.ok();
    }
}
