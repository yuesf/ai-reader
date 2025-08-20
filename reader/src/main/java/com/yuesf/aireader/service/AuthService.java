package com.yuesf.aireader.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.yuesf.aireader.entity.AdminUser;
import com.yuesf.aireader.mapper.AdminUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AuthService {

    private static final String ISSUER = "ai-reader";
    private static final long EXPIRE_MS = 24 * 60 * 60 * 1000L; // 24h
    private static final String SECRET = "change-this-secret"; // 可迁移到配置

    @Autowired
    private AdminUserMapper adminUserMapper;

    public String loginAndIssueToken(String username, String password) {
        AdminUser user = adminUserMapper.findByUsername(username);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("用户不存在或已禁用");
        }
        // 简单明文比对；生产中请使用哈希与盐
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return generateToken(user);
    }

    public String generateToken(AdminUser user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRE_MS);
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(String.valueOf(user.getId()))
                .withClaim("username", user.getUsername())
                .withClaim("displayName", user.getDisplayName())
                .withIssuedAt(now)
                .withExpiresAt(exp)
                .sign(algorithm);
    }

    public AdminUser verifyToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            var decoded = JWT.require(algorithm).withIssuer(ISSUER).build().verify(token);
            AdminUser user = new AdminUser();
            user.setId(Integer.parseInt(decoded.getSubject()));
            user.setUsername(decoded.getClaim("username").asString());
            user.setDisplayName(decoded.getClaim("displayName").asString());
            return user;
        } catch (Exception e) {
            return null;
        }
    }
}


