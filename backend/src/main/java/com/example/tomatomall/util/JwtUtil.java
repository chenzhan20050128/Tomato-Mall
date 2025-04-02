package com.example.tomatomall.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret:defaultSecret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration; // 默认24小时

    // 从token中获取用户ID
    public Integer getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return Integer.valueOf(claims.get("userId").toString());
        } catch (Exception e) {
            return null;
        }
    }

    // 生成token
    public String generateToken(Integer userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return doGenerateToken(claims);
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    private String doGenerateToken(Map<String, Object> claims) {
        Date createdDate = new Date();
        Date expirationDate = new Date(createdDate.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(createdDate)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    public String extractUsername(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    // 验证token是否有效
    public boolean validateToken(String token) {
        try {
            // 验证token是否过期
            final Date expiration = getExpirationDateFromToken(token);
            return !expiration.before(new Date());
        } catch (Exception e) {
            // 解析失败或其他异常表示token无效
            return false;
        }
    }

    // 从token中获取过期时间
    private Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // 从token中获取指定类型的声明
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
}