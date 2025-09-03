package com.yuesf.aireader.mapper.tracking;

import com.yuesf.aireader.entity.tracking.UserSession;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户会话Mapper接口
 * 提供用户会话的数据访问操作
 *
 * @author AI-Reader Team
 * @since 2025-01-09
 */
public interface UserSessionMapper {

    /**
     * 插入用户会话
     *
     * @param userSession 用户会话
     * @return 影响行数
     */
    int insert(UserSession userSession);

    /**
     * 更新用户会话
     *
     * @param userSession 用户会话
     * @return 影响行数
     */
    int update(UserSession userSession);

    /**
     * 根据会话ID查询用户会话
     *
     * @param sessionId 会话ID
     * @return 用户会话
     */
    UserSession selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据用户ID查询用户会话列表
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 用户会话列表
     */
    List<UserSession> selectByUserId(@Param("userId") String userId,
                                     @Param("limit") Integer limit,
                                     @Param("offset") Integer offset);

    /**
     * 查询活跃会话数量
     *
     * @param timeThreshold 时间阈值（毫秒）
     * @return 活跃会话数量
     */
    Long countActiveSessions(@Param("timeThreshold") Long timeThreshold);

    /**
     * 结束超时的会话
     *
     * @param timeoutThreshold 超时阈值（毫秒）
     * @return 影响行数
     */
    int endTimeoutSessions(@Param("timeoutThreshold") Long timeoutThreshold);

    /**
     * 删除过期的用户会话
     *
     * @param expireTime 过期时间
     * @return 删除数量
     */
    int deleteExpiredSessions(@Param("expireTime") Long expireTime);
}