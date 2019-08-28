package com.shuangyueliao.chat.queue.rocketmq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuangyueliao.chat.entity.Account;
import com.shuangyueliao.chat.entity.UserInfo;
import com.shuangyueliao.chat.handler.UserInfoManager;
import com.shuangyueliao.chat.mapper.AccountMapper;
import com.shuangyueliao.chat.proto.ChatProto;
import com.shuangyueliao.chat.queue.OfflineInfoTransmit;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author shuangyueliao
 * @create 2019/8/20 22:09
 * @Version 0.1
 */
public class RocketmqOfflineInfoHelper implements OfflineInfoTransmit {
    @Autowired
    private AccountMapper accountMapper;
    private DefaultMQProducer producer;
    private ConcurrentMap<Channel, UserInfo> userInfos = UserInfoManager.userInfos;
    private ConcurrentMap<Channel, DefaultMQPushConsumer> consumers = new ConcurrentHashMap<>();

    public RocketmqOfflineInfoHelper() {
        producer = new DefaultMQProducer("send");
        producer.setNamesrvAddr("localhost:9876");
        producer.setVipChannelEnabled(false);
        try {
            producer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }
    }

    public Map.Entry<Channel, UserInfo> getInfoByUserId(Integer userId) {
        Set<Map.Entry<Channel, UserInfo>> entries = userInfos.entrySet();
        for (Map.Entry<Channel, UserInfo> entry : entries) {
            UserInfo userInfo = entry.getValue();
            if (userInfo == null || !userInfo.isAuth()) {
                continue;
            }
            if (userInfo.getId().equals(userId)) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public void pushP2P(Integer userId, String message) {
        Message msg;
        try {
            System.out.println("pushp2p:   " + userId + "   " + message);
            msg = new Message("p2p_" + userId.toString(), message.getBytes(RemotingHelper.DEFAULT_CHARSET));
            SendResult sendResult = producer.send(msg, new MessageQueueSelector() {
                @Override
                public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                    Integer id = (Integer) arg;
                    int index = id % mqs.size();
                    return mqs.get(index);
                }
            }, userId);
            System.out.println("p2p: " + sendResult.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pushGroup(String groupNumber, String message) {
        LambdaQueryWrapper<Account> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Account::getGroupNumber, groupNumber);
        Message msg;
        try {
            msg = new Message("group_" + groupNumber, message.getBytes(RemotingHelper.DEFAULT_CHARSET));
            SendResult sendResult = producer.send(msg, new MessageQueueSelector() {
                @Override
                public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                    Integer id = (Integer) arg;
                    int index = id % mqs.size();
                    return mqs.get(index);
                }
            }, Integer.parseInt(groupNumber));
            System.out.println("group: " + sendResult.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerPull(Channel channel) {
        UserInfo userInfo = userInfos.get(channel);
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("consumer_" + userInfo.getId());
        try {
            consumer.subscribe("p2p_" + userInfo.getId(), "*");
            consumer.subscribe("group_" + userInfo.getGroupNumber(), "*");
            consumer.setNamesrvAddr("localhost:9876");
            consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
            consumer.setConsumeThreadMax(1);
            consumer.setConsumeThreadMin(1);
            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                    for (MessageExt msg : msgs) {
                        Integer userId = userInfo.getId();
                        System.out.println("consumer: " + new String(msg.getBody()));
                        UserInfoManager.rwLock.readLock().lock();
                        try {
                            Map.Entry<Channel, UserInfo> infoByUserId = getInfoByUserId(userId);
                            Channel channel = infoByUserId.getKey();
                            UserInfo userInfo = infoByUserId.getValue();
                            System.out.println("consumer:   " + userInfo.getId() + " " + userInfo.getUsername() + "  " + new String(msg.getBody()));
                            channel.writeAndFlush(new TextWebSocketFrame(ChatProto.buildMessProto(userInfo.getId(), userInfo.getUsername(), new String(msg.getBody()))));
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            UserInfoManager.rwLock.readLock().unlock();
                        }
                    }
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });
            consumer.start();
            consumers.put(channel, consumer);
        } catch (MQClientException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unregisterPull(Channel channel) {
        DefaultMQPushConsumer consumer = consumers.get(channel);
        consumer.shutdown();
        consumers.remove(channel);
    }

}
