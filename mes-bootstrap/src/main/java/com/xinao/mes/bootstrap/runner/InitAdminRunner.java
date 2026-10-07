package com.xinao.mes.bootstrap.runner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinao.mes.system.entity.SysUser;
import com.xinao.mes.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 首次启动初始化：种子数据中用户密码为占位值（非 BCrypt 格式），
 * 启动时按配置 mes.init-password 统一生成真实散列回写。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitAdminRunner implements CommandLineRunner {

    private final SysUserMapper userMapper;

    @Value("${mes.init-password}")
    private String initPassword;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        List<SysUser> pending = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1));
        int count = 0;
        for (SysUser user : pending) {
            String pwd = user.getPassword();
            if (!StringUtils.hasText(pwd) || !pwd.startsWith("$2")) {
                SysUser update = new SysUser();
                update.setId(user.getId());
                update.setPassword(encoder.encode(initPassword));
                userMapper.updateById(update);
                count++;
            }
        }
        if (count > 0) {
            log.info("已初始化 {} 个种子用户的初始密码（mes.init-password），请登录后尽快修改", count);
        }
    }
}
