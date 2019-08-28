package com.shuangyueliao.chat.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

/**
 * @author shuangyueliao
 * @create 2019/8/25 17:57
 * @Version 0.1
 */
public class Consumer {
    public static void main(String[] args) throws Exception{
        //创建连接工厂对象
        ConnectionFactory connectionFactory =
                new ActiveMQConnectionFactory("tcp://127.0.0.1:61616");
        //获取连接对象
        Connection connection = connectionFactory.createConnection("admin", "admin");
        //开启连接
        connection.start();
        //使用连接对象获取Session对象,并设置参数
        Session session =
                connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        //使用Session对象创建Destination对象,其中参数为：消息队列的名称
        Queue queue = session.createQueue("test-queue");
        //创建消息消费者对象
        MessageConsumer consumer = session.createConsumer(queue);
        //接收消息
        consumer.setMessageListener(new MessageListener() {
            //接收到消息的事件
            @Override
            public void onMessage(Message message) {
                //简单打印一下
                TextMessage textMessage = (TextMessage) message;
                try {
                    System.out.println(textMessage.getText());
                } catch (JMSException e) {
                    e.printStackTrace();
                }
                try {
                    throw new RuntimeException();
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread.sleep(1000);
        consumer.setMessageListener(null);
        //接收键盘输入，当在控制台输入回车时结束。（为了让该方法一直处于执行状态）
        System.in.read();
        //关闭资源
        consumer.close();
        session.close();
        connection.close();
    }
}
