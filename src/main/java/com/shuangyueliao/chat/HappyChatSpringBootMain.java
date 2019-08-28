package com.shuangyueliao.chat;

import com.shuangyueliao.chat.annotation.EnableCustomChat;
import com.shuangyueliao.chat.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WebSocket聊天室，客户端参考docs目录下的websocket.html
 */
@SpringBootApplication
@EnableCustomChat
//@EnableRocketmqChat
//@EnableActivemqChat
//@EnableRabbitmqChat
public class HappyChatSpringBootMain implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(HappyChatSpringBootMain.class);

    public static void main(String[] args) {
        SpringApplication.run(HappyChatSpringBootMain.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        final HappyChatServer server = new HappyChatServer(Constants.DEFAULT_PORT);
        server.init();
        server.start();
        // 注册进程钩子，在JVM进程关闭前释放资源
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run(){
                server.shutdown();
                logger.warn(">>>>>>>>>> jvm shutdown");
                System.exit(0);
            }
        });
    }
}
