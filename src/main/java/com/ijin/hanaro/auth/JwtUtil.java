// auth/JwtUtil.java
package com.ijin.hanaro.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    // 실제로는 환경변수/설정으로 분리
    private final String SECRET = "change-this-to-a-very-long-secret-key-change-this";
    private final long EXP_MS = 30 * 60 * 1000L; // 30분

    private Key key() { return Keys.hmacShaKeyFor(SECRET.getBytes()); }

    public String generateToken(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + EXP_MS))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
    }

    public String getUsername(String token) { return parse(token).getBody().getSubject(); }
    public String getRole(String token) { return (String) parse(token).getBody().get("role"); }
}