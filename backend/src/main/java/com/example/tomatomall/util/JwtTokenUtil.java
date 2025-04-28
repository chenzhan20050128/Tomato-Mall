package com.example.tomatomall.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    public Integer getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
                    
            return Integer.parseInt(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}