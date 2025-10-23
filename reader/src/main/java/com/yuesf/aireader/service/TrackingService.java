package com.yuesf.aireader.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuesf.aireader.dto.tracking.TrackingBatchReportRequest;
import com.yuesf.aireader.dto.tracking.TrackingReportRequest;
import com.yuesf.aireader.entity.tracking.TrackingEvent;
import com.yuesf.aireader.entity.tracking.UserSession;
import com.yuesf.aireader.exception.BusinessException;
import com.yuesf.aireader.mapper.tracking.TrackingEventMapper;
import com.yuesf.aireader.mapper.tracking.UserSessionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 埋点服务类
 * 提供埋点数据的业务逻辑处理
 *
 * @author AI-Reader Team
 * @since 2025-01-09
 */
@Slf4j
@Service
@Transactional
public class TrackingService {

    @Autowired
    private TrackingEventMapper trackingEventMapper;

    @Autowired
    private UserSessionMapper userSessionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 上报单个埋点事件
     * 
     * 接收并处理单个埋点事件的上报，包括参数验证、数据存储和会话更新
     *
     * @param request 埋点上报请求，包含用户ID、事件类型、页面路径等信息
     * @return boolean 上报是否成功，true表示成功，false表示失败
     * @throws BusinessException 当参数验证失败或业务处理异常时抛出
     */
    public boolean reportEvent(TrackingReportRequest request) {
        try {
            // 验证请求参数
            validateTrackingRequest(request);

            // 创建或更新用户会话
            createOrUpdateSession(request.getSessionId(), request.getUserId(), 
                                convertToJson(request.getDeviceInfo()), request.getNetworkType());

            // 转换为实体对象
            TrackingEvent event = convertToTrackingEvent(request);

            // 插入埋点事件
            int result = trackingEventMapper.insert(event);

            // 更新会话统计
            updateSessionStats(request.getSessionId());

            log.info("埋点事件上报成功: userId={}, eventType={}, pagePath={}", 
                    request.getUserId(), request.getEventType(), request.getPagePath());

            return result > 0;
        } catch (Exception e) {
            log.error("埋点事件上报失败: {}", e.getMessage(), e);
            throw new BusinessException("埋点事件上报失败: " + e.getMessage());
        }
    }

    /**
     * 批量上报埋点事件
     * 
     * 接收并处理批量埋点事件的上报，提高上报效率，减少网络请求次数
     *
     * @param request 批量埋点上报请求，包含事件列表和批次ID
     * @return int 成功上报的事件数量
     * @throws BusinessException 当参数验证失败或业务处理异常时抛出
     */
    public int batchReportEvents(TrackingBatchReportRequest request) {
        try {
            if (request.getEvents() == null || request.getEvents().isEmpty()) {
                throw new BusinessException("批量上报事件列表不能为空");
            }

            // 验证所有请求参数
            for (TrackingReportRequest eventRequest : request.getEvents()) {
                validateTrackingRequest(eventRequest);
            }

            // 先创建或更新会话（确保外键约束满足）
            Map<String, String> sessionUserMap = request.getEvents().stream()
                    .collect(Collectors.toMap(
                            TrackingReportRequest::getSessionId,
                            TrackingReportRequest::getUserId,
                            (existing, replacement) -> existing
                    ));

            for (Map.Entry<String, String> entry : sessionUserMap.entrySet()) {
                String sessionId = entry.getKey();
                String userId = entry.getValue();
                
                // 获取该会话的设备信息和网络类型
                TrackingReportRequest firstEvent = request.getEvents().stream()
                        .filter(e -> e.getSessionId().equals(sessionId))
                        .findFirst()
                        .orElse(null);
                
                if (firstEvent != null) {
                    // 先创建或更新会话，确保session_id存在
                    createOrUpdateSession(sessionId, userId, 
                                        convertToJson(firstEvent.getDeviceInfo()), 
                                        firstEvent.getNetworkType());
                }
            }

            // 转换为实体对象列表
            List<TrackingEvent> events = request.getEvents().stream()
                    .map(this::convertToTrackingEvent)
                    .collect(Collectors.toList());

            // 批量插入埋点事件（此时session_id已存在）
            int result = trackingEventMapper.batchInsert(events);

            // 更新会话统计
            for (String sessionId : sessionUserMap.keySet()) {
                updateSessionStats(sessionId);
            }

            log.info("批量埋点事件上报成功: batchId={}, eventCount={}", 
                    request.getBatchId(), result);

            return result;
        } catch (Exception e) {
            log.error("批量埋点事件上报失败: {}", e.getMessage(), e);
            throw new BusinessException("批量埋点事件上报失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户行为轨迹
     * 
     * 根据用户ID和时间范围查询用户的行为轨迹，用于用户行为分析
     *
     * @param userId 用户ID，不能为空
     * @param startTime 开始时间戳（毫秒），可为null表示不限制开始时间
     * @param endTime 结束时间戳（毫秒），可为null表示不限制结束时间
     * @param limit 限制返回的记录数量，默认100条，最大不超过1000条
     * @return List<TrackingEvent> 用户行为轨迹列表，按时间倒序排列
     * @throws BusinessException 当用户ID为空或查询异常时抛出
     */
    public List<TrackingEvent> getUserPath(String userId, Long startTime, Long endTime, Integer limit) {
        try {
            if (!StringUtils.hasText(userId)) {
                throw new BusinessException("用户ID不能为空");
            }

            // 设置默认值
            if (limit == null || limit <= 0) {
                limit = 100;
            }

            // 查询用户行为轨迹
            List<TrackingEvent> events = trackingEventMapper.selectByUserId(userId, limit, 0);

            // 如果指定了时间范围，进行过滤
            if (startTime != null || endTime != null) {
                events = events.stream()
                        .filter(event -> {
                            if (startTime != null && event.getTimestamp() < startTime) {
                                return false;
                            }
                            if (endTime != null && event.getTimestamp() > endTime) {
                                return false;
                            }
                            return true;
                        })
                        .collect(Collectors.toList());
            }

            return events;
        } catch (Exception e) {
            log.error("获取用户行为轨迹失败: {}", e.getMessage(), e);
            throw new BusinessException("获取用户行为轨迹失败: " + e.getMessage());
        }
    }

    /**
     * 获取页面热力图数据
     * 
     * 统计指定页面或所有页面的点击热力图数据，用于页面优化分析
     *
     * @param pagePath 页面路径，可为null表示统计所有页面
     * @param startTime 开始时间戳（毫秒），可为null表示不限制开始时间
     * @param endTime 结束时间戳（毫秒），可为null表示不限制结束时间
     * @return List<Map<String, Object>> 热力图数据列表，包含元素ID、点击次数等信息
     * @throws BusinessException 当查询异常时抛出
     */
    public List<Map<String, Object>> getHeatmapData(String pagePath, Long startTime, Long endTime) {
        try {
            return trackingEventMapper.getHeatmapData(pagePath, startTime, endTime);
        } catch (Exception e) {
            log.error("获取热力图数据失败: {}", e.getMessage(), e);
            throw new BusinessException("获取热力图数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取埋点统计数据
     * 
     * 根据指定的分组维度统计埋点数据，支持多种维度的统计分析
     *
     * @param groupBy 分组维度，支持：page_path（页面路径）、event_type（事件类型）、date（日期）、hour（小时）
     * @param startTime 开始时间戳（毫秒），可为null表示不限制开始时间
     * @param endTime 结束时间戳（毫秒），可为null表示不限制结束时间
     * @return List<Map<String, Object>> 统计数据列表，包含分组字段和统计数量
     * @throws BusinessException 当分组维度不支持或查询异常时抛出
     */
    public List<Map<String, Object>> getStatistics(String groupBy, Long startTime, Long endTime) {
        try {
            if (!StringUtils.hasText(groupBy)) {
                groupBy = "date";
            }

            return trackingEventMapper.statisticsByDimension(groupBy, startTime, endTime);
        } catch (Exception e) {
            log.error("获取统计数据失败: {}", e.getMessage(), e);
            throw new BusinessException("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取活跃用户数量
     * 
     * 统计指定时间阈值内的活跃用户数量，用于实时监控
     *
     * @param timeThreshold 时间阈值（毫秒时间戳），统计该时间点之后的活跃用户
     * @return Long 活跃用户数量
     * @throws BusinessException 当查询异常时抛出
     */
    public Long getActiveUserCount(Long timeThreshold) {
        try {
            if (timeThreshold == null) {
                // 默认30分钟内的活跃用户
                timeThreshold = System.currentTimeMillis() - 30 * 60 * 1000;
            }

            return userSessionMapper.countActiveSessions(timeThreshold);
        } catch (Exception e) {
            log.error("获取活跃用户数量失败: {}", e.getMessage(), e);
            throw new BusinessException("获取活跃用户数量失败: " + e.getMessage());
        }
    }

    /**
     * 创建或更新用户会话
     * 
     * 根据会话ID创建新会话或更新现有会话信息，用于会话管理
     *
     * @param sessionId 会话ID，不能为空
     * @param userId 用户ID，不能为空
     * @param deviceInfo 设备信息JSON字符串，包含设备型号、系统版本等
     * @param networkType 网络类型，如wifi、4g、5g等
     * @return UserSession 用户会话对象
     * @throws BusinessException 当参数为空或操作失败时抛出
     */
    public UserSession createOrUpdateSession(String sessionId, String userId, String deviceInfo, String networkType) {
        try {
            // 查询是否已存在会话
            UserSession existingSession = userSessionMapper.selectBySessionId(sessionId);

            if (existingSession != null) {
                // 更新现有会话
                existingSession.setNetworkType(networkType);
                existingSession.setDeviceInfo(deviceInfo);
                existingSession.setUpdatedAt(LocalDateTime.now());
                
                userSessionMapper.update(existingSession);
                return existingSession;
            } else {
                // 创建新会话
                UserSession newSession = UserSession.builder()
                        .sessionId(sessionId)
                        .userId(userId)
                        .startTime(System.currentTimeMillis())
                        .pageCount(0)
                        .eventCount(0)
                        .duration(0)
                        .deviceInfo(deviceInfo)
                        .networkType(networkType)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                userSessionMapper.insert(newSession);
                return newSession;
            }
        } catch (Exception e) {
            log.error("创建或更新用户会话失败: {}", e.getMessage(), e);
            throw new BusinessException("创建或更新用户会话失败: " + e.getMessage());
        }
    }

    /**
     * 结束用户会话
     * 
     * 设置会话结束时间并计算会话持续时长，用于会话统计
     *
     * @param sessionId 会话ID，不能为空
     * @return boolean 操作是否成功，true表示成功，false表示会话不存在或已结束
     * @throws BusinessException 当会话ID为空或操作异常时抛出
     */
    public boolean endSession(String sessionId) {
        try {
            UserSession session = userSessionMapper.selectBySessionId(sessionId);
            if (session != null && session.getEndTime() == null) {
                long endTime = System.currentTimeMillis();
                session.setEndTime(endTime);
                session.setDuration((int) ((endTime - session.getStartTime()) / 1000));
                session.setUpdatedAt(LocalDateTime.now());
                
                int result = userSessionMapper.update(session);
                return result > 0;
            }
            return false;
        } catch (Exception e) {
            log.error("结束用户会话失败: {}", e.getMessage(), e);
            throw new BusinessException("结束用户会话失败: " + e.getMessage());
        }
    }

    /**
     * 清理过期数据
     * 
     * 删除指定天数之前的埋点事件和用户会话数据，用于数据维护
     *
     * @param expireDays 过期天数，大于0的整数
     * @return Map<String, Integer> 清理结果，包含删除的事件数量和会话数量
     * @throws BusinessException 当参数无效或清理异常时抛出
     */
    public Map<String, Integer> cleanupExpiredData(int expireDays) {
        try {
            long expireTime = System.currentTimeMillis() - (long) expireDays * 24 * 60 * 60 * 1000;

            // 清理过期的埋点事件
            int deletedEvents = trackingEventMapper.deleteExpiredEvents(expireTime);

            // 清理过期的用户会话
            int deletedSessions = userSessionMapper.deleteExpiredSessions(expireTime);

            Map<String, Integer> result = new HashMap<>();
            result.put("deletedEvents", deletedEvents);
            result.put("deletedSessions", deletedSessions);

            log.info("清理过期数据完成: 删除事件{}条, 删除会话{}条", deletedEvents, deletedSessions);

            return result;
        } catch (Exception e) {
            log.error("清理过期数据失败: {}", e.getMessage(), e);
            throw new BusinessException("清理过期数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时监控数据
     * 
     * 获取埋点系统的实时监控数据，包括今日事件数、活跃用户数、热门页面等
     *
     * @return Map<String, Object> 监控数据，包含各项统计指标
     * @throws BusinessException 当数据获取异常时抛出
     */
    public Map<String, Object> getDashboardData() {
        try {
            Map<String, Object> dashboard = new HashMap<>();

            // 获取今日统计
            long todayStart = getTodayStartTimestamp();
            long todayEnd = System.currentTimeMillis();

            Map<String, Object> todayParams = new HashMap<>();
            todayParams.put("startTime", todayStart);
            todayParams.put("endTime", todayEnd);

            Long todayEvents = trackingEventMapper.countEvents(todayParams);
            Long activeUsers = getActiveUserCount(todayStart);

            dashboard.put("todayEvents", todayEvents);
            dashboard.put("activeUsers", activeUsers);

            // 获取热门页面
            List<Map<String, Object>> topPages = trackingEventMapper.statisticsByDimension("page_path", todayStart, todayEnd);
            dashboard.put("topPages", topPages.stream().limit(10).collect(Collectors.toList()));

            // 获取热门事件类型
            List<Map<String, Object>> topEvents = trackingEventMapper.statisticsByDimension("event_type", todayStart, todayEnd);
            dashboard.put("topEvents", topEvents.stream().limit(10).collect(Collectors.toList()));

            return dashboard;
        } catch (Exception e) {
            log.error("获取监控面板数据失败: {}", e.getMessage(), e);
            throw new BusinessException("获取监控面板数据失败: " + e.getMessage());
        }
    }

    /**
     * 验证埋点请求参数
     * 
     * 对埋点上报请求进行参数验证，确保必要字段不为空且格式正确
     * 
     * @param request 埋点上报请求对象
     * @throws BusinessException 当参数验证失败时抛出，包含具体的错误信息
     */
    private void validateTrackingRequest(TrackingReportRequest request) {
        if (request == null) {
            throw new BusinessException("埋点请求对象不能为空");
        }
        
        if (!StringUtils.hasText(request.getUserId())) {
            throw new BusinessException("用户ID不能为空");
        }
        
        if (!StringUtils.hasText(request.getSessionId())) {
            throw new BusinessException("会话ID不能为空");
        }
        
        if (!StringUtils.hasText(request.getEventType())) {
            throw new BusinessException("事件类型不能为空");
        }
        
        if (!StringUtils.hasText(request.getPagePath())) {
            throw new BusinessException("页面路径不能为空");
        }
        
        if (request.getTimestamp() == null || request.getTimestamp() <= 0) {
            throw new BusinessException("时间戳不能为空且必须大于0");
        }
        
        // 验证时间戳不能是未来时间
        long currentTime = System.currentTimeMillis();
        if (request.getTimestamp() > currentTime + 60000) { // 允许1分钟的时间误差
            throw new BusinessException("时间戳不能是未来时间");
        }
        
        // 验证时间戳不能太久远（超过30天）
        long thirtyDaysAgo = currentTime - 30L * 24 * 60 * 60 * 1000;
        if (request.getTimestamp() < thirtyDaysAgo) {
            throw new BusinessException("时间戳不能超过30天前");
        }
    }

    /**
     * 转换为埋点事件实体
     * 
     * 将埋点上报请求对象转换为数据库实体对象
     * 
     * @param request 埋点上报请求对象
     * @return TrackingEvent 埋点事件实体对象
     */
    private TrackingEvent convertToTrackingEvent(TrackingReportRequest request) {
        return TrackingEvent.builder()
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .eventType(request.getEventType())
                .pagePath(request.getPagePath())
                .elementId(request.getElementId())
                .elementText(request.getElementText())
                .properties(convertToJson(request.getProperties()))
                .timestamp(request.getTimestamp())
                .deviceInfo(convertToJson(request.getDeviceInfo()))
                .networkType(request.getNetworkType())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 转换对象为JSON字符串
     * 
     * 将Java对象转换为JSON字符串格式，用于数据库存储
     * 
     * @param obj 待转换的对象
     * @return String JSON字符串，转换失败时返回对象的toString()结果
     */
    private String convertToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("转换JSON失败，使用toString()替代: obj={}, error={}", obj.getClass().getSimpleName(), e.getMessage());
            return obj.toString();
        }
    }

    /**
     * 更新会话统计信息
     * 
     * 统计并更新用户会话的事件数量和页面访问数量
     * 
     * @param sessionId 会话ID
     */
    private void updateSessionStats(String sessionId) {
        try {
            UserSession session = userSessionMapper.selectBySessionId(sessionId);
            if (session == null) {
                log.warn("会话不存在，无法更新统计信息: sessionId={}", sessionId);
                return;
            }

            // 统计该会话的事件数量
            List<TrackingEvent> sessionEvents = trackingEventMapper.selectBySessionId(sessionId);
            
            // 统计页面访问数量（去重）
            long pageCount = sessionEvents.stream()
                    .filter(event -> "page_view".equals(event.getEventType()))
                    .map(TrackingEvent::getPagePath)
                    .distinct()
                    .count();

            session.setEventCount(sessionEvents.size());
            session.setPageCount((int) pageCount);
            session.setUpdatedAt(LocalDateTime.now());

            int updateResult = userSessionMapper.update(session);
            if (updateResult > 0) {
                log.debug("会话统计更新成功: sessionId={}, eventCount={}, pageCount={}", 
                        sessionId, sessionEvents.size(), pageCount);
            } else {
                log.warn("会话统计更新失败: sessionId={}", sessionId);
            }
        } catch (Exception e) {
            log.error("更新会话统计异常: sessionId={}, error={}", sessionId, e.getMessage(), e);
        }
    }

    /**
     * 获取今日开始时间戳
     * 
     * 获取当天00:00:00的时间戳，用于统计今日数据
     * 
     * @return long 今日开始时间的毫秒时间戳
     */
    private long getTodayStartTimestamp() {
        LocalDateTime today = LocalDateTime.now()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        return java.sql.Timestamp.valueOf(today).getTime();
    }
}