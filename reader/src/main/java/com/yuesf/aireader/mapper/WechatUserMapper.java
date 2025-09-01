package com.yuesf.aireader.mapper;

import com.yuesf.aireader.entity.WechatUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WechatUserMapper {

    WechatUser findByOpenId(String openId);

    int insert(WechatUser user);

    int update(WechatUser user);

    WechatUser findById(String openId);

    int deleteById(String openId);
}
