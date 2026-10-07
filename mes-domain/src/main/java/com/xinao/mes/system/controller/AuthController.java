package com.xinao.mes.system.controller;

import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.auth.LoginUser;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.R;
import com.xinao.mes.system.dto.LoginDTO;
import com.xinao.mes.system.dto.LoginVO;
import com.xinao.mes.system.dto.RouterVO;
import com.xinao.mes.system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 认证接口：登录 / 当前用户信息 / 菜单树
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录（白名单，无需 token） */
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    /** 当前登录用户信息（前端初始化渲染按钮/权限用） */
    @GetMapping("/info")
    public R<Map<String, Object>> info() {
        LoginUser user = AuthContext.get();
        if (user == null) {
            throw new BizException(401, "未登录");
        }
        return R.ok(Map.of(
                "userId", user.getUserId(),
                "userName", user.getUserName(),
                "empNo", user.getEmpNo() == null ? "" : user.getEmpNo(),
                "orgId", user.getOrgId() == null ? 0 : user.getOrgId(),
                "roles", user.getRoles(),
                "perms", user.getPerms()
        ));
    }

    /** 当前用户菜单树 */
    @GetMapping("/menus")
    public R<List<RouterVO>> menus() {
        LoginUser user = AuthContext.get();
        if (user == null) {
            throw new BizException(401, "未登录");
        }
        return R.ok(authService.getMenuTree(user.getUserId()));
    }
}
