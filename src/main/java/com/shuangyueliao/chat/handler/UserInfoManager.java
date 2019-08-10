package com.shuangyueliao.chat.handler;

import com.shuangyueliao.chat.entity.UserInfo;
import com.shuangyueliao.chat.proto.ChatProto;
import com.shuangyueliao.chat.util.BlankUtil;
import com.shuangyueliao.chat.util.NettyUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @description Channel的管理器以及user管理工具类
 */
public class UserInfoManager {
    private static Map<String, String> map = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(UserInfoManager.class);

    private static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    private static ConcurrentMap<Channel, UserInfo> userInfos = new ConcurrentHashMap<>();
    private static AtomicInteger userCount = new AtomicInteger(0);

    static {
        map.put("test1", "123456");
        map.put("test2", "123456");
        map.put("test3", "123456");
    }

    public static void addChannel(Channel channel) {
        String remoteAddr = NettyUtil.parseChannelRemoteAddr(channel);
        System.out.println("addChannel:" + remoteAddr);
        if (!channel.isActive()) {
            logger.error("channel is not active, address: {}", remoteAddr);
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setAddr(remoteAddr);
        userInfo.setChannel(channel);
        userInfo.setTime(System.currentTimeMillis());
        userInfos.put(channel, userInfo);
    }

    public static boolean saveUser(Channel channel, String nick, String password) {
        UserInfo userInfo = userInfos.get(channel);
        if (userInfo == null) {
            return false;
        }
        if (!channel.isActive()) {
            logger.error("channel is not active, address: {}, nick: {}", userInfo.getAddr(), nick);
            return false;
        }
        // 验证用户名和密码
        if (nick == null || password == null) {
            return false;
        }
        if (!password.equals(map.get(nick))) {
            return false;
        }
        // 增加一个认证用户
        userCount.incrementAndGet();
        userInfo.setNick(nick);
        userInfo.setAuth(true);
        userInfo.setUserId();
        userInfo.setTime(System.currentTimeMillis());
        return true;
    }

    /**
     * 从缓存中移除Channel，并且关闭Channel
     *
     * @param channel
     */
    public static void removeChannel(Channel channel) {
        try {
            logger.warn("channel will be remove, address is :{}", NettyUtil.parseChannelRemoteAddr(channel));
            //加上读写锁保证移除channel时，避免channel关闭时，还有别的线程对其操作，造成错误
            rwLock.writeLock().lock();
            channel.close();
            UserInfo userInfo = userInfos.get(channel);
            if (userInfo != null) {
                UserInfo tmp = userInfos.remove(channel);
                if (tmp != null && tmp.isAuth()) {
                    // 减去一个认证用户
                    userCount.decrementAndGet();
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }

    }

    /**
     * 广播普通消息
     *
     * @param message
     */
    public static void broadcastMess(int uid, String nick, String message) {
        if (!BlankUtil.isBlank(message)) {
            try {
                rwLock.readLock().lock();
                Set<Channel> keySet = userInfos.keySet();
                for (Channel ch : keySet) {
                    UserInfo userInfo = userInfos.get(ch);
                    if (userInfo == null || !userInfo.isAuth()) {
                        continue;
                    }
                    ch.writeAndFlush(new TextWebSocketFrame(ChatProto.buildMessProto(uid, nick, message)));
                }
            } finally {
                rwLock.readLock().unlock();
            }
        }
    }

    /**
     * 广播系统消息
     */
    public static void broadCastInfo(int code, Object mess) {
        try {
            rwLock.readLock().lock();
            Set<Channel> keySet = userInfos.keySet();
            for (Channel ch : keySet) {
                UserInfo userInfo = userInfos.get(ch);
                if (userInfo == null || !userInfo.isAuth()) {
                    continue;
                }
                ch.writeAndFlush(new TextWebSocketFrame(ChatProto.buildSystProto(code, mess)));
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public static void broadCastPing() {
        try {
            rwLock.readLock().lock();
            logger.info("broadCastPing userCount: {}", userCount.intValue());
            Set<Channel> keySet = userInfos.keySet();
            for (Channel ch : keySet) {
                UserInfo userInfo = userInfos.get(ch);
                //如果channel是没有用户信息或者没有授权的用户则跳过
                if (userInfo == null || !userInfo.isAuth()) {
                    continue;
                }
                ch.writeAndFlush(new TextWebSocketFrame(ChatProto.buildPingProto()));
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 发送系统消息
     *
     * @param code
     * @param mess
     */
    public static void sendInfo(Channel channel, int code, Object mess) {
        channel.writeAndFlush(new TextWebSocketFrame(ChatProto.buildSystProto(code, mess)));
    }

    public static void sendPong(Channel channel) {
        channel.writeAndFlush(new TextWebSocketFrame(ChatProto.buildPongProto()));
    }

    /**
     * 扫描并关闭失效的Channel
     */
    public static void scanNotActiveChannel() {
        Set<Channel> keySet = userInfos.keySet();
        for (Channel ch : keySet) {
            UserInfo userInfo = userInfos.get(ch);
            if (userInfo == null) {
                continue;
            }
            //如果channel没有打开或者激活或者验证用户信息时间超过10s就认为这是一个无效channel，应该移除
            if (!ch.isOpen() || !ch.isActive() || (!userInfo.isAuth() &&
                    (System.currentTimeMillis() - userInfo.getTime()) > 10000)) {
                removeChannel(ch);
            }
        }
    }


    public static UserInfo getUserInfo(Channel channel) {
        return userInfos.get(channel);
    }

    public static ConcurrentMap<Channel, UserInfo> getUserInfos() {
        return userInfos;
    }

    public static int getAuthUserCount() {
        return userCount.get();
    }

    public static void updateUserTime(Channel channel) {
        UserInfo userInfo = getUserInfo(channel);
        if (userInfo != null) {
            userInfo.setTime(System.currentTimeMillis());
        }
    }
}
