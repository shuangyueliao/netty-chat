package com.shuangyueliao.chat.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

/**
 * @author shuangyueliao
 * @create 2019/8/25 18:35
 * @Version 0.1
 */
public class TopicProducer {
    public static void main(String[] args) throws JMSException {
        // 连接到ActiveMQ服务器
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("username",
                "password","tcp://127.0.0.1:61616");
        Connection connection = factory.createConnection();
        connection.start();
        Session session = connection.createSession(Boolean.TRUE,
                Session.AUTO_ACKNOWLEDGE);
        // 创建主题
        Topic topic = session.createTopic("slimsmart.topic.test");
        MessageProducer producer = session.createProducer(topic);
        // NON_PERSISTENT 非持久化 PERSISTENT 持久化,发送消息时用使用持久模式
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        TextMessage message = session.createTextMessage();
        message.setText("topic 消息。");
        message.setStringProperty("property", "消息Property");
        // 发布主题消息
        producer.send(message);
        System.out.println("Sent message: " + message.getText());
        session.commit();
        session.close();
        connection.close();
    }
}
