package com.yuesf.aireader.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 文件信息实体类
 */
@Entity
@Table(name = "file_info")
public class FileInfo {
    @Id
    private String id;
    
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;
    
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    
    @Column(name = "file_type", length = 50)
    private String fileType;
    
    @Column(length = 100)
    private String folder;
    
    @Column(name = "upload_time")
    private LocalDateTime uploadTime;
    
    @Column(name = "upload_user_id", length = 50)
    private String uploadUserId;
    
    @Column(length = 20)
    private String status;
    
    @Column(name = "request_id", length = 100)
    private String requestId;

    // 构造函数
    public FileInfo() {}

    public FileInfo(String id, String fileName, String originalName, Long fileSize, 
                   String fileType, String folder, 
                   LocalDateTime uploadTime, String uploadUserId, String status, String requestId) {
        this.id = id;
        this.fileName = fileName;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.folder = folder;
        this.uploadTime = uploadTime;
        this.uploadUserId = uploadUserId;
        this.status = status;
        this.requestId = requestId;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getUploadUserId() {
        return uploadUserId;
    }

    public void setUploadUserId(String uploadUserId) {
        this.uploadUserId = uploadUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}