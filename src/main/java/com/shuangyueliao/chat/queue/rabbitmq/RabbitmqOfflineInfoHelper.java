package com.shuangyueliao.chat.queue.rabbitmq;

import com.rabbitmq.client.*;
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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

/**
 * @author shuangyueliao
 * @create 2019/8/27 22:53
 * @Version 0.1
 */
public class RabbitmqOfflineInfoHelper implements OfflineInfoTransmit {
    private static final String EXCHANGE_NAME = "chat_exchange_topic";
    private com.rabbitmq.client.Channel rabbitmqChannel = null;
    private ConcurrentMap<Channel, UserInfo> userInfos = UserInfoManager.userInfos;
    private ConcurrentMap<Channel, com.rabbitmq.client.Channel> consumers = new ConcurrentHashMap<>();
    @Autowired
    private AccountMapper accountMapper;

    @PostConstruct
    public void init() throws IOException {
        Connection connection = ConnectionUtil.getConnection();
        rabbitmqChannel = connection.createChannel();
        //声明交换机
        rabbitmqChannel.exchangeDeclare(EXCHANGE_NAME, "topic");
        List<Account> accounts = accountMapper.selectList(null);
        for (Account account : accounts) {
            rabbitmqChannel.queueDeclare("queue_" + account.getId(), false, false, false, null);
            rabbitmqChannel.queueBind("queue_" + account.getId(), EXCHANGE_NAME, "p2p." + account.getId());
            rabbitmqChannel.queueBind("queue_" + account.getId(), EXCHANGE_NAME, "group." + account.getGroupNumber());
        }
    }

    @Override
    public void pushP2P(Integer userId, String message) {
        try {
            rabbitmqChannel.basicPublish(EXCHANGE_NAME, "p2p." + userId, false, false, null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void pushGroup(String groupNumber, String message) {
        try {
            rabbitmqChannel.basicPublish(EXCHANGE_NAME, "group." + groupNumber, false, false, null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void registerPull(Channel channel) {
        UserInfo userInfo = userInfos.get(channel);
        Connection connection = ConnectionUtil.getConnection();
        try {
            com.rabbitmq.client.Channel connectionChannel = connection.createChannel();
            Consumer consumer = new DefaultConsumer(connectionChannel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                        throws IOException {
                    super.handleDelivery(consumerTag, envelope, properties, body);
                    String s = new String(body, "UTF-8");
                    UserInfoManager.rwLock.readLock().lock();
                    try {
                        if (channel.isOpen()) {
                            channel.writeAndFlush(new TextWebSocketFrame(ChatProto.buildMessProto(userInfo.getId(), userInfo.getUsername(), s)));
                            connectionChannel.basicAck(envelope.getDeliveryTag(), false);
                        } else {
                            throw new RuntimeException();
                        }
                    } finally {
                        UserInfoManager.rwLock.readLock().unlock();
                    }
                }
            };
            consumers.put(channel, connectionChannel);
            connectionChannel.basicConsume("queue_" + userInfo.getId(), false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unregisterPull(Channel channel) {
        com.rabbitmq.client.Channel remove = consumers.remove(channel);
        try {
            remove.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
