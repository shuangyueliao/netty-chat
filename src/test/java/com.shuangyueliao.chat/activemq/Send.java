package com.shuangyueliao.chat.activemq;


import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import javax.jms.*;

/**
 * @author shuangyueliao
 * @create 2019/8/25 17:17
 * @Version 0.1
 */
public class Send {
    public static void main(String[] args) throws Exception {
        //创建连接工厂对象
        ConnectionFactory connectionFactory =
                new ActiveMQConnectionFactory("tcp://127.0.0.1:61616");
        //获取连接对象
        Connection connection = connectionFactory.createConnection();
        //开启连接
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        //使用Session对象创建Destination对象,其中参数为：消息队列的名称
        javax.jms.Queue queue = session.createQueue("test-queue");
        javax.jms.Queue queue1 = session.createQueue("test-queue1");
        //使用session创建消息生产者对象
        MessageProducer producer = session.createProducer(queue);
        MessageProducer producer1 = session.createProducer(queue1);
        //创建消息对象
        TextMessage message = new ActiveMQTextMessage();
        message.setText("这是一个测试消息");
        //发送消息
        producer.send(message);
        producer1.send(message);
        //关闭资源
        producer.close();
        session.close();
        connection.close();
    }
}
