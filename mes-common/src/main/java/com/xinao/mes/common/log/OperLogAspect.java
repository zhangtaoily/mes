package com.xinao.mes.common.log;

import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.auth.LoginUser;
import com.xinao.mes.common.mapper.SysOperLogMapper;
import com.xinao.mes.common.util.IpUtil;
import com.xinao.mes.common.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志切面：异步落库 sys_oper_log
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final SysOperLogMapper operLogMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        SysOperLog record = buildLog(joinPoint, operLog.title());
        Object result;
        try {
            result = joinPoint.proceed();
            record.setStatus(1);
            record.setJsonResult(truncate(JsonUtil.toJson(result)));
        } catch (Throwable e) {
            record.setStatus(0);
            record.setErrorMsg(truncate(e.getMessage()));
            throw e;
        } finally {
            try {
                operLogMapper.insert(record);
            } catch (Exception e) {
                // 日志落库失败不影响业务
                log.error("操作日志保存失败: {}", e.getMessage());
            }
        }
        return result;
    }

    private SysOperLog buildLog(ProceedingJoinPoint joinPoint, String title) {
        SysOperLog record = new SysOperLog();
        record.setTitle(title);
        record.setOperTime(LocalDateTime.now());

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        record.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());

        // 参数序列化（过滤 request/response/file 等不可序列化对象）
        String args = Arrays.stream(joinPoint.getArgs())
                .filter(a -> !(a instanceof HttpServletRequest) && !(a instanceof HttpServletResponse) && !(a instanceof MultipartFile))
                .map(JsonUtil::toJson)
                .collect(Collectors.joining(","));
        record.setOperParam(truncate(args));

        LoginUser user = AuthContext.get();
        if (user != null) {
            record.setOperName(user.getUserName() + "(" + user.getEmpNo() + ")");
        }
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            record.setUrl(truncate(request.getRequestURI()));
            record.setOperIp(IpUtil.clientIp(request));
        }
        return record;
    }

    /** 截断超长文本，防止撑爆 TEXT 列 */
    private String truncate(String s) {
        if (s == null || s.length() <= 2000) {
            return s;
        }
        return s.substring(0, 2000);
    }
}
