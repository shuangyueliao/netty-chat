package com.shuangyueliao.chat.rabbitmq;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.*;
import com.shuangyueliao.chat.queue.rabbitmq.ConnectionUtil;

import java.io.IOException;

public class Consumer2 {

    private static final String EXCHANGE_NAME = "test_exchange_topic";

    private  static final String QUEUE_NAME = "test_queue_topic_2";

    public static void main(String[] args) throws IOException {
        Connection connection = ConnectionUtil.getConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME,false,false,false,null);

        channel.queueBind(QUEUE_NAME,EXCHANGE_NAME,"order.insert");

        channel.basicQos(1);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
                super.handleDelivery(consumerTag, envelope, properties, body);
                System.out.println("接收消息：" + new String(body, "UTF-8"));
            }
        };
        channel.basicConsume(QUEUE_NAME,true,consumer);

    }
}

