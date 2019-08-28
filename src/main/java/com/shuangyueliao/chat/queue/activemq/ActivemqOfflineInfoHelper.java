package com.shuangyueliao.chat.queue.activemq;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shuangyueliao.chat.entity.Account;
import com.shuangyueliao.chat.entity.UserInfo;
import com.shuangyueliao.chat.handler.UserInfoManager;
import com.shuangyueliao.chat.mapper.AccountMapper;
import com.shuangyueliao.chat.proto.ChatProto;
import com.shuangyueliao.chat.queue.OfflineInfoTransmit;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.jms.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author shuangyueliao
 * @create 2019/8/25 16:26
 * @Version 0.1
 */
public class ActivemqOfflineInfoHelper implements OfflineInfoTransmit {
    @Autowired
    private AccountMapper accountMapper;
    private ConcurrentHashMap<Integer, MessageProducer> p2pHashMap = new ConcurrentHashMap();
    private ConcurrentHashMap<String, MessageProducer> groupHashMap = new ConcurrentHashMap<>();
    private ConcurrentMap<Channel, UserInfo> userInfos = UserInfoManager.userInfos;
    private ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://127.0.0.1:61616");
    private ConcurrentHashMap<Channel, Connection> connections = new ConcurrentHashMap();
    @PostConstruct
    public void init() throws Exception {
        List<Account> accounts = accountMapper.selectList(null);
        //获取连接对象
        Connection connection = connectionFactory.createConnection();
        //开启连接
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        for (Account account : accounts) {
            //使用Session对象创建Destination对象,其中参数为：消息队列的名称
            javax.jms.Queue queue = session.createQueue("p2p_" + account.getId());
            //使用session创建消息生产者对象
            MessageProducer producer = session.createProducer(queue);
            p2pHashMap.put(account.getId(), producer);
        }
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.select("distinct groupNumber");
        List list = accountMapper.selectObjs(queryWrapper);
        for (Object o : list) {
            //使用Session对象创建Destination对象,其中参数为：消息队列的名称
            Topic topic = session.createTopic("group_" + o);
            //使用session创建消息生产者对象
            MessageProducer producer = session.createProducer(topic);
            groupHashMap.put(o.toString(), producer);
        }
        //初始化群发消息的clientId，要先初始化才能保存topic的离线消息
        initGroupClientId(accounts);
    }

    private void initGroupClientId(List<Account> accounts) {
        try {
            for (Account account : accounts) {
                String clientId = "client_" + account.getId();
                // 连接到ActiveMQ服务器
                Connection connection = connectionFactory.createConnection();
                //客户端ID,持久订阅需要设置
                connection.setClientID(clientId);
                connection.start();
                Session session = connection.createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);
                // 创建主题
                Topic topic = session.createTopic("group_" + account.getGroupNumber());
                // 创建持久订阅,指定客户端ID。
                MessageConsumer consumer = session.createDurableSubscriber(topic, clientId);
                consumer.close();
                session.close();
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pushP2P(Integer userId, String message) {
        MessageProducer messageProducer = p2pHashMap.get(userId);
        TextMessage textMessage = new ActiveMQTextMessage();
        try {
            textMessage.setText(message);
            messageProducer.send(textMessage);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pushGroup(String groupNumber, String message) {
        MessageProducer messageProducer = groupHashMap.get(groupNumber);
        TextMessage textMessage = new ActiveMQTextMessage();
        try {
            textMessage.setText(message);
            messageProducer.send(textMessage);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerPull(Channel channel) {
        UserInfo userInfo = userInfos.get(channel);
        try {
            String clientId = "client_" + userInfo.getId();
            // 连接到ActiveMQ服务器
            Connection connection = connectionFactory.createConnection();
            connections.put(channel, connection);
            //客户端ID,持久订阅需要设置
            connection.setClientID(clientId);
            connection.start();
            Session session = connection.createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);
            // 创建主题
            Topic topic = session.createTopic("group_" + userInfo.getGroupNumber());
            Queue queue = session.createQueue("p2p_" + userInfo.getId());
            // 创建持久订阅,指定客户端ID。
            MessageConsumer consumer1 = session.createDurableSubscriber(topic, clientId);
            MessageConsumer consumer2 = session.createConsumer(queue);
            topicListener(channel, userInfo, consumer1);
            topicListener(channel, userInfo, consumer2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void topicListener(Channel channel, UserInfo userInfo, MessageConsumer consumer2) throws JMSException {
        consumer2.setMessageListener(new MessageListener() {
            // 订阅接收方法
            @Override
            public void onMessage(Message message) {
                TextMessage tm = (TextMessage) message;
                UserInfoManager.rwLock.readLock().lock();
                try {
                    if (channel.isOpen()) {
                        channel.writeAndFlush(new TextWebSocketFrame(ChatProto.buildMessProto(userInfo.getId(), userInfo.getUsername(), tm.getText())));
                    } else {
                        throw new RuntimeException();
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                } finally {
                    UserInfoManager.rwLock.readLock().unlock();
                }
            }
        });
    }

    @Override
    public void unregisterPull(Channel channel) {
        Connection remove = connections.remove(channel);
        try {
            remove.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
