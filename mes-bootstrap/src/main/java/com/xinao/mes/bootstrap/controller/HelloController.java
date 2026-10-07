package com.xinao.mes.bootstrap.controller;

import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.auth.LoginUser;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * M0 验收接口：连通性 / 鉴权 / 按钮级权限演示
 */
@RestController
@RequestMapping("/api/hello")
public class HelloController {

    /** 公开接口（白名单）：验证服务与数据库连通 */
    @GetMapping
    public R<Map<String, Object>> hello() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", "新澳毛纺 MES");
        data.put("stage", "M0 工程奠基");
        data.put("modules", "common / domain(system) / engine(骨架) / plugin-api / bootstrap");
        data.put("time", LocalDateTime.now());
        return R.ok(data);
    }

    /** 需登录：验证 JWT 鉴权链路（无 token 返回 401） */
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        LoginUser user = AuthContext.get();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getUserId());
        data.put("userName", user.getUserName());
        data.put("empNo", user.getEmpNo());
        data.put("roles", user.getRoles());
        return R.ok(data);
    }

    /** 需权限 sys:user:list：验证按钮级权限拦截（无权限返回 403，超管放行） */
    @GetMapping("/perm")
    @RequiresPerm("sys:user:list")
    public R<String> perm() {
        return R.ok("你拥有 sys:user:list 权限");
    }
}
