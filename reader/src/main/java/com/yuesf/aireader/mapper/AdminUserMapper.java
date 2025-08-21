package com.yuesf.aireader.mapper;

import com.yuesf.aireader.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AdminUserMapper {

    @Select("SELECT id, username, password, display_name as displayName, status FROM admin_users WHERE username = #{username} LIMIT 1")
    AdminUser findByUsername(@Param("username") String username);

    @Select({
            "<script>",
            "SELECT id, username, password, display_name as displayName, status FROM admin_users",
            "<where>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    (username LIKE '%' || #{keyword} || '%' OR display_name LIKE '%' || #{keyword} || '%')",
            "  </if>",
            "  <if test='status != null'>",
            "    status = #{status}",
            "  </if>",
            "</where>",
            "ORDER BY id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    java.util.List<AdminUser> listUsers(@Param("keyword") String keyword, @Param("status") Integer status,
                                        @Param("limit") Integer limit, @Param("offset") Integer offset);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM admin_users",
            "<where>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    (username LIKE '%' || #{keyword} || '%' OR display_name LIKE '%' || #{keyword} || '%')",
            "  </if>",
            "  <if test='status != null'>",
            "    status = #{status}",
            "  </if>",
            "</where>",
            "</script>"
    })
    Long countUsers(@Param("keyword") String keyword, @Param("status") Integer status);

    @Insert("INSERT INTO admin_users(username, password, display_name, status) VALUES(#{username}, #{password}, #{displayName}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createUser(AdminUser user);

    @Update({
            "<script>",
            "UPDATE admin_users",
            "<set>",
            "  <if test='username != null'>username=#{username},</if>",
            "  <if test='password != null'>password=#{password},</if>",
            "  <if test='displayName != null'>display_name=#{displayName},</if>",
            "  <if test='status != null'>status=#{status},</if>",
            "</set>",
            "WHERE id=#{id}",
            "</script>"
    })
    int updateUser(AdminUser user);

    @Delete("DELETE FROM admin_users WHERE id=#{id}")
    int deleteUser(@Param("id") Integer id);
}


