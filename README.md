# netty-chat(v2.0)
基于Netty实现的WebSocket聊天室，实现的功能如下：<br>
The WebSocket chat room based on Netty is implemented as follows:
<br>
1. 支持登录
2. 支持多人同时在线
3. 同步显示在线人数
4. 支持文字和表情的内容
5. 浏览器与服务器保持长连接，定时心跳检测
6. 支持群聊
7. 支持单聊
8. 支持接收离线消息
9. 能无缝切换内存数组、rabbitmq、activemq、rocketmq四种不同方法来存储和转发聊天消息
<br><br>


# 快速开始/quick start：
服务端：
1. 建立数据库nettychat,导入netty-chat\docs\sql\nettychat.sql文件入数据库
2. 在文件netty-chat\src\main\resources\application.properties中修改数据库连接信息
3. 运行包目录com.shuangyueliao.chat下的类HappyChatSpringBootMain的main方法 
（默认用内存数组存放和转发聊天信息，如需切换使用中间件rabbitmq、activemq或rocketmq,则先启动相应的中间件，然后在类HappyChatSpringBootMain上关闭注解@EnableCustomChat，然后打开相应注释（rabbitmq对应注解@EnableRabbitmqChat、rocketmq对应@EnableRocketmqChat、activemq对应@EnableActivemqChat）

![info](https://github.com/shuangyueliao/netty-chat/blob/master/docs/docimg/1.png?raw=true)


客户端：以浏览器方式打开docs目录下的index.html，登录的用户名和密码在数据库中
![info](https://github.com/shuangyueliao/netty-chat/blob/master/docs/docimg/%E5%BE%AE%E4%BF%A1%E6%88%AA%E5%9B%BE_20190828122757.png?raw=true)


# 效果图
![login](https://github.com/shuangyueliao/netty-chat/blob/master/docs/docimg/login.png?raw=true)
<br>
![chat](https://github.com/shuangyueliao/netty-chat/blob/master/docs/docimg/%E5%BE%AE%E4%BF%A1%E6%88%AA%E5%9B%BE_20190828094840.png?raw=true)
