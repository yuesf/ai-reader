package com.yuesf.aireader.mapper;

import com.yuesf.aireader.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminUserMapper {

    @Select("SELECT id, username, password, display_name as displayName, status FROM admin_users WHERE username = #{username} LIMIT 1")
    AdminUser findByUsername(@Param("username") String username);
}


