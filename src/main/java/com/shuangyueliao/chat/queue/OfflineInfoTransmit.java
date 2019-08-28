package com.shuangyueliao.chat.queue;


import io.netty.channel.Channel;

/**
 * @author shuangyueliao
 * @create 2019/8/18 23:20
 * @Version 0.1
 */
public interface OfflineInfoTransmit {
    void pushP2P(Integer userId, String message);

    void pushGroup(String groupNumber, String message);

    void registerPull(Channel channel);

    void unregisterPull(Channel channel);
}
