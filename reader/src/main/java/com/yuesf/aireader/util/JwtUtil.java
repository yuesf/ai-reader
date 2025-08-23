package com.yuesf.aireader.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret:defaultSecretKey}")
    private String secret;

    @Value("${app.jwt.expiration:86400}")
    private long expiration; // 默认24小时

    private static final String ISSUER = "ai-reader";
    private static final String USER_ID_CLAIM = "userId";
    private static final String USERNAME_CLAIM = "username";
    private static final String ROLE_CLAIM = "role";

    /**
     * 生成JWT令牌
     */
    public String generateToken(String userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);

        return JWT.create()
                .withIssuer(ISSUER)
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .withClaim(USER_ID_CLAIM, userId)
                .withClaim(USERNAME_CLAIM, username)
                .withClaim(ROLE_CLAIM, role)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 验证JWT令牌
     */
    public DecodedJWT verifyToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                .withIssuer(ISSUER)
                .build();
        return verifier.verify(token);
    }

    /**
     * 从令牌中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            return jwt.getClaim(USER_ID_CLAIM).asString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            return jwt.getClaim(USERNAME_CLAIM).asString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从令牌中获取用户角色
     */
    public String getRoleFromToken(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            return jwt.getClaim(ROLE_CLAIM).asString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查令牌是否有效
     */
    public boolean isTokenValid(String token) {
        try {
            verifyToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
