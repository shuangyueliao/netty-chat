package com.shuangyueliao.chat.queue.custom;

import com.shuangyueliao.chat.entity.Account;
import com.shuangyueliao.chat.entity.UserInfo;
import com.shuangyueliao.chat.handler.UserInfoManager;
import com.shuangyueliao.chat.mapper.AccountMapper;
import com.shuangyueliao.chat.proto.ChatProto;
import com.shuangyueliao.chat.queue.OfflineInfoTransmit;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author shuangyueliao
 * @create 2019/8/15 23:54
 * @Version 0.1
 */
public class CustomOfflineInfoHelper implements OfflineInfoTransmit {
    public static Map<String, List<Integer>> groupMap = new ConcurrentHashMap<>();
    public static Map<Integer, List<String>> infoMap = new ConcurrentHashMap<>();
    private ConcurrentMap<Channel, UserInfo> userInfos = UserInfoManager.userInfos;
    private List<Channel> channels = new Vector<>();
    @Autowired
    private AccountMapper accountMapper;

    @PostConstruct
    private void init() {
        List<Account> accounts = accountMapper.selectList(null);
        for (Account account : accounts) {
            infoMap.put(account.getId(), new ArrayList<>());
            List<Integer> list = groupMap.get(account.getGroupNumber());
            if (list == null) {
                List<Integer> list1 = new ArrayList<>();
                list1.add(account.getId());
                groupMap.put(account.getGroupNumber(), list1);
            } else {
                list.add(account.getId());
                groupMap.put(account.getGroupNumber(), list);
            }
        }
        pullThread();
    }

    @Override
    public void pushP2P(Integer userId, String message) {
        List<String> strings = CustomOfflineInfoHelper.infoMap.get(userId);
        strings.add(message);
    }

    @Override
    public void pushGroup(String groupNumber, String message) {
        List<Integer> list = CustomOfflineInfoHelper.groupMap.get(groupNumber);
        message = "[来自于群组" + groupNumber + "的消息]:" + message;
        for (Integer integer : list) {
            List<String> strings = CustomOfflineInfoHelper.infoMap.get(integer);
            strings.add(message);
        }
    }

    @Override
    public void registerPull(Channel channel) {
        channels.add(channel);
    }

    @Override
    public void unregisterPull(Channel channel) {
        channels.remove(channel);
    }


    public void pullThread() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                UserInfoManager.rwLock.readLock().lock();
                for (Channel channel : channels) {
                    UserInfo userInfo = userInfos.get(channel);
                    List<String> strings = CustomOfflineInfoHelper.infoMap.get(userInfo.getId());
                    for (String string : strings) {
                        channel.writeAndFlush(new TextWebSocketFrame(ChatProto.buildMessProto(userInfo.getId(), userInfo.getUsername(), string)));
                    }
                    strings.clear();
                }
                UserInfoManager.rwLock.readLock().unlock();
            }
        }).start();
    }
}
