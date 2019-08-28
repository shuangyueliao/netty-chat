package com.shuangyueliao.chat.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.shuangyueliao.chat.queue.rabbitmq.ConnectionUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Producer {

    private static final String EXCHANGE_NAME = "test_exchange_topic";

    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = ConnectionUtil.getConnection();
        Channel channel = connection.createChannel();
        //声明交换机
        channel.exchangeDeclare(EXCHANGE_NAME,"topic");
        String message = "匹配insert";
        channel.basicPublish(EXCHANGE_NAME,"order.a",false,false,null,message.getBytes());

        channel.close();
        connection.close();
    }
}

