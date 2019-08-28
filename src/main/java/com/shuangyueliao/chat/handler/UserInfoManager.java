package com.shuangyueliao.chat.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuangyueliao.chat.entity.Account;
import com.shuangyueliao.chat.entity.UserInfo;
import com.shuangyueliao.chat.mapper.AccountMapper;
import com.shuangyueliao.chat.proto.ChatProto;
import com.shuangyueliao.chat.queue.OfflineInfoTransmit;
import com.shuangyueliao.chat.util.BlankUtil;
import com.shuangyueliao.chat.util.NettyUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author shuangyueliao
 * @description Channel的管理器以及user管理工具类
 */
@Component
public class UserInfoManager {
    private static final Logger logger = LoggerFactory.getLogger(UserInfoManager.class);

    public static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    public static ConcurrentMap<Channel, UserInfo> userInfos = new ConcurrentHashMap<>();
    private static AtomicInteger userCount = new AtomicInteger(0);
    @Resource
    private AccountMapper accountMapper;
    private static AccountMapper accountMapperStatic;

    @Autowired
    private OfflineInfoTransmit offlineInfoTransmit;
    private static OfflineInfoTransmit offlineInfoTransmitStatic;

    public static void p2p(Integer uid, String nick, String other, String message) {
        if (!BlankUtil.isBlank(message)) {
            try {
                rwLock.readLock().lock();
                message = "[来自于用户" + nick +"的消息]:" + message;
                Set<Channel> keySet = userInfos.keySet();
                for (Channel ch : keySet) {
                    UserInfo userInfo = userInfos.get(ch);
                    // 找出对应channel进行发送
                    if (userInfo == null || !userInfo.isAuth() || !userInfo.getNick().equals(other)) {
                        continue;
                    }
                    //在线用户的个人对个人通信直接走channel，不走第三方中间件和其它
                    ch.writeAndFlush(new TextWebSocketFrame(ChatProto.buildMessProto(userInfo.getId(), userInfo.getUsername(), message)));
                    return;
                }
                LambdaQueryWrapper<Account> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                lambdaQueryWrapper.eq(Account::getUsername, other);
                Account account = accountMapperStatic.selectOne(lambdaQueryWrapper);
                if (account != null) {
                    offlineInfoTransmitStatic.pushP2P(account.getId(), message);
                }
            } finally {
                rwLock.readLock().unlock();
            }
        }
    }

    @PostConstruct
    public void init() {
        accountMapperStatic = accountMapper;
        offlineInfoTransmitStatic = offlineInfoTransmit;
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
        LambdaQueryWrapper<Account> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Account::getUsername, nick).eq(Account::getPassword, password);
        Account account = accountMapperStatic.selectOne(lambdaQueryWrapper);
        if (account == null) {
            return false;
        }
        // 增加一个认证用户
        userCount.incrementAndGet();
        userInfo.setNick(nick);
        userInfo.setAuth(true);
        userInfo.setId(account.getId());
        userInfo.setUsername(account.getUsername());
        userInfo.setGroupNumber(account.getGroupNumber());
        userInfo.setTime(System.currentTimeMillis());

        // 注册该用户推送消息的通道
        offlineInfoTransmitStatic.registerPull(channel);
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
                if (userInfo.isAuth()) {
                    offlineInfoTransmitStatic.unregisterPull(channel);
                    // 减去一个认证用户
                    userCount.decrementAndGet();
                }
                userInfos.remove(channel);
            }
        } finally {
            rwLock.writeLock().unlock();
        }

    }

    /**
     * 在同一个群中广播普通消息
     *
     * @param message
     */
    public static void broadcastMess(int uid, String nick, String message, String groupNumber) {
        if (!BlankUtil.isBlank(message)) {
            try {
                rwLock.readLock().lock();
                offlineInfoTransmitStatic.pushGroup(groupNumber, message);
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
