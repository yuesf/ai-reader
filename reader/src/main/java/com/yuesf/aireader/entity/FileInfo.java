package com.yuesf.aireader.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文件信息实体类
 */
@Setter
@Getter
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


    @Column(name = "page_nums")
    private Integer pageNums;
    // 构造函数
    public FileInfo() {}

}