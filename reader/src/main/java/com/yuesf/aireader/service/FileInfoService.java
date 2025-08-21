package com.yuesf.aireader.service;

import com.yuesf.aireader.entity.FileInfo;
import com.yuesf.aireader.mapper.FileInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文件信息服务类
 */
@Service
public class FileInfoService {

    @Autowired
    private FileInfoMapper fileInfoMapper;

    /**
     * 保存文件信息
     * @param fileInfo 文件信息
     * @return 保存后的文件信息
     */
    public FileInfo saveFileInfo(FileInfo fileInfo) {
        if (fileInfo.getId() == null) {
            fileInfo.setId(generateFileId());
        }
        if (fileInfo.getUploadTime() == null) {
            fileInfo.setUploadTime(LocalDateTime.now());
        }
        if (fileInfo.getStatus() == null) {
            fileInfo.setStatus("ACTIVE");
        }
        
        fileInfoMapper.insert(fileInfo);
        return fileInfo;
    }

    /**
     * 根据ID查询文件信息
     * @param id 文件ID
     * @return 文件信息
     */
    public FileInfo getFileInfoById(String id) {
        return fileInfoMapper.selectById(id);
    }

    /**
     * 根据文件夹查询文件列表
     * @param folder 文件夹
     * @return 文件列表
     */
    public List<FileInfo> getFileInfoByFolder(String folder) {
        return fileInfoMapper.selectByFolder(folder);
    }

    /**
     * 根据上传用户ID查询文件列表
     * @param uploadUserId 上传用户ID
     * @return 文件列表
     */
    public List<FileInfo> getFileInfoByUploadUserId(String uploadUserId) {
        return fileInfoMapper.selectByUploadUserId(uploadUserId);
    }

    /**
     * 更新文件状态
     * @param id 文件ID
     * @param status 状态
     * @return 是否成功
     */
    public boolean updateFileStatus(String id, String status) {
        return fileInfoMapper.updateStatus(id, status) > 0;
    }

    /**
     * 删除文件信息（软删除）
     * @param id 文件ID
     * @return 是否成功
     */
    public boolean deleteFileInfo(String id) {
        return fileInfoMapper.deleteById(id) > 0;
    }

    /**
     * 验证文件信息是否存在且有效
     * @param id 文件ID
     * @return 是否存在
     */
    public boolean validateFileInfo(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        FileInfo fileInfo = getFileInfoById(id);
        return fileInfo != null && "ACTIVE".equals(fileInfo.getStatus());
    }

    /**
     * 生成文件ID
     * @return 文件ID
     */
    private String generateFileId() {
        return "file_" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
    }
}
