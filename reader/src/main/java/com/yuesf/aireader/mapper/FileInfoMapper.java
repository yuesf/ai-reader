package com.yuesf.aireader.mapper;

import com.yuesf.aireader.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件信息Mapper接口
 */
@Mapper
public interface FileInfoMapper {
    
    /**
     * 插入文件信息
     * @param fileInfo 文件信息
     * @return 影响行数
     */
    int insert(FileInfo fileInfo);
    
    /**
     * 根据ID查询文件信息
     * @param id 文件ID
     * @return 文件信息
     */
    FileInfo selectById(@Param("id") String id);
    
    /**
     * 根据文件夹查询文件列表
     * @param folder 文件夹
     * @return 文件列表
     */
    List<FileInfo> selectByFolder(@Param("folder") String folder);
    
    /**
     * 根据上传用户ID查询文件列表
     * @param uploadUserId 上传用户ID
     * @return 文件列表
     */
    List<FileInfo> selectByUploadUserId(@Param("uploadUserId") String uploadUserId);
    
    /**
     * 更新文件状态
     * @param id 文件ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") String id, @Param("status") String status);
    
    /**
     * 删除文件信息（软删除）
     * @param id 文件ID
     * @return 影响行数
     */
    int deleteById(@Param("id") String id);
}
