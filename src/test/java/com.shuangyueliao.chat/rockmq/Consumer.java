package com.shuangyueliao.chat.rockmq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * @author shuangyueliao
 * @create 2019/8/17 9:49
 * @Version 0.1
 */
public class Consumer {
    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("consumer");
        consumer.setNamesrvAddr("localhost:9876");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
//        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.subscribe("a", "*");
        consumer.subscribe("p2p_1", "*");
        consumer.subscribe("p2p_2", "*");
        consumer.subscribe("p2p_3", "*");
//        consumer.subscribe("p2p_5", "*");
        consumer.subscribe("group_1", "*");
        consumer.subscribe("group_2", "*");
        consumer.subscribe("group_3", "*");
//        consumer.subscribe("group_4", "*");
//        consumer.subscribe("group_5", "*");
//        consumer.subscribe("group_6", "*");
        consumer.setConsumeThreadMin(1);
        consumer.setConsumeThreadMax(1);
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                System.out.printf("aaaaaaaaaaaaaaa%s %s Receive New Messages: %s %n", new String(msgs.get(0).getBody()), Thread.currentThread().getName(), msgs);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
//                return null;
            }
        });
        consumer.start();

        System.out.printf("Consumer Started.%n");
    }
}
