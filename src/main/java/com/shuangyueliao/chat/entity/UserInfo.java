package com.shuangyueliao.chat.entity;

import io.netty.channel.Channel;

/**
 * @description user实体类
 */
public class UserInfo extends Account {

    private boolean isAuth = false; // 是否认证
    private long time = 0;  // 登录时间
    private String nick;    // 昵称
    private String addr;    // 地址
    private Channel channel;// 通道

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public boolean isAuth() {
        return isAuth;
    }

    public void setAuth(boolean auth) {
        isAuth = auth;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }


}
