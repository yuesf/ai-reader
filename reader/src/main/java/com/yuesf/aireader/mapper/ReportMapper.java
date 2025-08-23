package com.yuesf.aireader.mapper;

import com.yuesf.aireader.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ReportMapper {

    List<Report> selectReports(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("source") String source,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categories") List<String> categories,
            @Param("sources") List<String> sources,
            @Param("sortBy") String sortBy,
            @Param("sortOrder") String sortOrder,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    long countReports(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("source") String source,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categories") List<String> categories,
            @Param("sources") List<String> sources
    );

    Report selectById(@Param("id") String id);

    int insertReport(Report report);

    int insertReportTags(@Param("reportId") String reportId, @Param("tags") java.util.List<String> tags);

    int updateReport(Report report);

    int deleteById(@Param("id") String id);

    int deleteTagsByReportId(@Param("id") String id);

    int batchDeleteByIds(@Param("ids") java.util.List<String> ids);

    int batchDeleteTagsByIds(@Param("ids") java.util.List<String> ids);
}
