package com.shuangyueliao.chat.rabbitmq;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.*;
import com.shuangyueliao.chat.queue.rabbitmq.ConnectionUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Consumer1 {

    private static final String EXCHANGE_NAME = "test_exchange_topic";

    private  static final String QUEUE_NAME = "test_queue_topic_1";

    public static void main(String[] args) throws IOException {
        Connection connection = ConnectionUtil.getConnection();
        Channel channel = connection.createChannel();
        Channel channe2 = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME,false,false,false,null);
        channe2.queueDeclare(QUEUE_NAME + "2",false,false,false,null);
        channel.queueBind(QUEUE_NAME,EXCHANGE_NAME,"order.a");
        channe2.queueBind(QUEUE_NAME + "2",EXCHANGE_NAME,"order.*");


        Consumer consumer1 = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
                    throws IOException {
                super.handleDelivery(consumerTag, envelope, properties, body);
                System.out.println("111111" + new String(body,"UTF-8"));
                channel.basicAck(envelope.getDeliveryTag(), false);
            }

        };
        Consumer consumer2 = new DefaultConsumer(channe2) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
                    throws IOException {
                super.handleDelivery(consumerTag, envelope, properties, body);
                System.out.println("2222222" + new String(body,"UTF-8"));
                channe2.basicAck(envelope.getDeliveryTag(), false);
            }

        };
        channel.basicConsume(QUEUE_NAME,false,consumer1);
        try {
            channel.close();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        channe2.basicConsume(QUEUE_NAME,false,consumer2);
    }
}

