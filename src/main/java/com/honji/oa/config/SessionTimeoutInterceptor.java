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
            System.out.println("has code===");
            return true;
        }

        Object userName = session.getAttribute("userName");
        if (userName == null) {//session过期需要重新使用微信网页登录授权
            System.out.println("session deprecated===");
            System.out.println(request.getRequestURI());
            String uri = request.getRequestURI();
            String rootPath = request.getRequestURL().substring(0,
                    request.getRequestURL().length() - uri.length());
            String indexPath = rootPath.concat("/index");
            if (uri.contains("/toAudit")) { //从微信消息窗口进来的不能跳转到首页，需要直接去到审核页面
                indexPath = rootPath.concat(uri);
            }

            System.out.println("indexPath" + indexPath);
            String authUrl = wxService.getOauth2Service().buildAuthorizationUrl(indexPath, "STATE", "snsapi_userinfo");
            response.sendRedirect(authUrl);
            return false;
        }
        return true;
    }

}
