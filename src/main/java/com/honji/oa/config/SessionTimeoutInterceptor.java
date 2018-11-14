package com.honji.oa.config;

import me.chanjar.weixin.cp.api.WxCpService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Component
public class SessionTimeoutInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private WxCpService wxService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws IOException {

        HttpSession session = request.getSession();
        String code = request.getParameter("code");
        if (StringUtils.isNotEmpty(code)) {//有code参数表示微信网页登录授权回调，直接放行
            return true;
        }

        Object userName = session.getAttribute("userName");
        if (userName == null) {//session过期需要重新使用微信网页登录授权
            String rootPath = request.getRequestURL().substring(0,
                    request.getRequestURL().length() - request.getRequestURI().length());
            String indexPath = rootPath.concat("/index");
            String authUrl = wxService.getOauth2Service().buildAuthorizationUrl(indexPath, "STATE", "snsapi_userinfo");
            response.sendRedirect(authUrl);
            return false;
        }
        return true;
    }

}
