package com.rk.agent.common.security;

import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;

/**
 * 登录用户上下文（基于 ThreadLocal）
 */
public class SecurityContextHolder {

    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    public static void set(LoginUser user) {
        CONTEXT.set(user);
    }

    public static LoginUser get() {
        return CONTEXT.get();
    }

    public static LoginUser required() {
        LoginUser user = CONTEXT.get();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long requireUserId() {
        return required().getUserId();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
