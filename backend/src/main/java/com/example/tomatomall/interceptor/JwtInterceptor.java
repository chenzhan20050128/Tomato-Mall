package com.example.tomatomall.interceptor;

import com.example.tomatomall.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.tomatomall.vo.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        // 添加payment-success到白名单
        if (path.contains("/payment-success") || 
        path.matches("/api/orders/notify")   // 其他白名单路径...
        ) 
            return true; // 直接放行，不进行身份验证
        
        // 扩展排除规则
        if (request.getRequestURI().matches("/api/orders/\\d+/pay") || 
            request.getRequestURI().matches("/api/orders/\\d+/check-payment")) {
            log.info("支付或检查支付相关路径，允许通过: {}", request.getRequestURI());

                        // 尝试从请求中获取token
                        String token = extractToken(request);
                        if (token != null) {
                            try {
                                // 从token中提取userId并设置
                                Integer userId = extractUserIdFromToken(token);
                                if (userId != null) {
                                    request.setAttribute("userId", userId);
                                    log.info("为支付请求设置用户ID: {}", userId);
                                }
                            } catch (Exception e) {
                                log.warn("解析支付请求token失败，继续处理: {}", e.getMessage());
                            }
                        }
                        
                        // 无论token是否有效都放行
            return true;
        }
        
        // 放行OPTIONS请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 不需要验证的路径
        String uri = request.getRequestURI();
        String method = request.getMethod();
        // 登录和注册路径不需要验证

        if (("/api/accounts/login".equals(uri))
                || ("/api/accounts".equals(uri) && "POST".equalsIgnoreCase(method))  // 注册接口
                || (uri.startsWith("/api/orders/notify"))  //支付宝的回调地址放行
        )
        {
            return true;
        }

        // 验证Token
        String token = request.getHeader("token");
        if (token != null && jwtUtil.validateToken(token)) {
            // 验证通过，将用户名放入请求属性中
            String username = jwtUtil.extractUsername(token);
            request.setAttribute("username", username);
            Integer userId = jwtUtil.getUserIdFromToken(token);
            // 将用户ID添加到请求属性中
            request.setAttribute("userId", userId);
            log.info("拦截器在request.setAttribute设置了用户ID: {} 用户名: {}", userId,username);
            return true;
        }

        // 验证失败，返回401
        handleUnauthorized(response);
        return false;
    }

    private void handleUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(Response.buildFailure("未授权", "401")));
    }

    private String extractToken(HttpServletRequest request) {
        // 从Authorization头获取
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // 尝试从请求参数获取
        String token = request.getParameter("token");
        if (token != null && !token.isEmpty()) {
            return token;
        }
        
        return null;
    }

    private Integer extractUserIdFromToken(String token) {
        // 实现从token获取userId的逻辑
        // 使用您现有的JwtTokenUtil或其他方式
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.error("从token中提取userId失败: {}", e.getMessage());
            return null;
        }
    }
}