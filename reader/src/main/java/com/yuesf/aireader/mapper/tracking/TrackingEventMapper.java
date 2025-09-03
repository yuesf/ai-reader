package com.yuesf.aireader.mapper.tracking;

import com.yuesf.aireader.entity.tracking.TrackingEvent;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 埋点事件Mapper接口
 * 提供埋点事件的数据访问操作
 *
 * @author AI-Reader Team
 * @since 2025-01-09
 */
public interface TrackingEventMapper {

    /**
     * 插入单个埋点事件
     *
     * @param trackingEvent 埋点事件
     * @return 影响行数
     */
    int insert(TrackingEvent trackingEvent);

    /**
     * 批量插入埋点事件
     *
     * @param events 埋点事件列表
     * @return 影响行数
     */
    int batchInsert(@Param("events") List<TrackingEvent> events);

    /**
     * 根据ID查询埋点事件
     *
     * @param id 事件ID
     * @return 埋点事件
     */
    TrackingEvent selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询埋点事件列表
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 埋点事件列表
     */
    List<TrackingEvent> selectByUserId(@Param("userId") String userId, 
                                       @Param("limit") Integer limit, 
                                       @Param("offset") Integer offset);

    /**
     * 根据会话ID查询埋点事件列表
     *
     * @param sessionId 会话ID
     * @return 埋点事件列表
     */
    List<TrackingEvent> selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据页面路径查询埋点事件列表
     *
     * @param pagePath 页面路径
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 埋点事件列表
     */
    List<TrackingEvent> selectByPagePath(@Param("pagePath") String pagePath,
                                         @Param("startTime") Long startTime,
                                         @Param("endTime") Long endTime,
                                         @Param("limit") Integer limit,
                                         @Param("offset") Integer offset);

    /**
     * 根据事件类型查询埋点事件列表
     *
     * @param eventType 事件类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 埋点事件列表
     */
    List<TrackingEvent> selectByEventType(@Param("eventType") String eventType,
                                          @Param("startTime") Long startTime,
                                          @Param("endTime") Long endTime,
                                          @Param("limit") Integer limit,
                                          @Param("offset") Integer offset);

    /**
     * 统计埋点事件数量
     *
     * @param params 查询参数
     * @return 事件数量
     */
    Long countEvents(@Param("params") Map<String, Object> params);

    /**
     * 按维度统计埋点事件
     *
     * @param groupBy 分组字段 (page_path, event_type, date等)
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    List<Map<String, Object>> statisticsByDimension(@Param("groupBy") String groupBy,
                                                     @Param("startTime") Long startTime,
                                                     @Param("endTime") Long endTime);

    /**
     * 获取热力图数据
     *
     * @param pagePath 页面路径
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 热力图数据
     */
    List<Map<String, Object>> getHeatmapData(@Param("pagePath") String pagePath,
                                              @Param("startTime") Long startTime,
                                              @Param("endTime") Long endTime);

    /**
     * 删除过期的埋点事件
     *
     * @param expireTime 过期时间
     * @return 删除数量
     */
    int deleteExpiredEvents(@Param("expireTime") Long expireTime);
}