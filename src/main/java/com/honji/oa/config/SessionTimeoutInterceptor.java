package com.honji.oa.config;

import me.chanjar.weixin.cp.api.WxCpService;
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
        Object userName = session.getAttribute("userName");
        if (userName == null) {
            String authUrl = wxService.getOauth2Service().buildAuthorizationUrl("153ebb9c.ngrok.io", "STATE", "snsapi_userinfo");
            request.setAttribute("authUrl", authUrl);
            response.sendRedirect(request.getContextPath()+"/templates/timeout.html");

        }
        return true;
    }

}
