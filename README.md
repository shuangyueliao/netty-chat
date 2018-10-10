# HappyChat
基于Netty实现的WebSocket聊天室，实现的功能如下：<br>
The WebSocket chat room based on Netty is implemented as follows:
<br>
1. 支持昵称登录/Support nickname login；<br>
2. 支持多人同时在线/Support multiple people online at the same time；<br>
3. 同步显示在线人数/Synchronous display of online number；<br>
4. 支持文字和表情的内容/Support words and expressions；<br>
5. 浏览器与服务器保持长连接，定时心跳检测/The browser maintains a long connection with the server and detects heartbeat regularly.；
<br><br>


# 快速开始/quick start：
 将代码下载下来，导入idea为maven项目，启动HappyChatMain的server端<br>
 直接通过浏览器打开docs文件夹下的index.html，随便输入昵称登陆,开启两个浏览器窗口登陆，然后随便发送几条消息
 
Download the code, import idea into Maven project, and start the server end of HappyChatMain.java.<br>
Open index.html under the docs folder directly through the browser, enter the nickname login, open two browser windows to login, and then send a few messages randomly:
<br>

# 效果/effect：
![effect](https://github.com/lightTrace/chat-room-by-netty/blob/master/docs/pic/show.png)

<br>


# 流程描述/Process description
![discription](https://github.com/lightTrace/chat-room-by-netty/blob/master/docs/pic/flow.png)


 
