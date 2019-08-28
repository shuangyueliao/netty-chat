package com.shuangyueliao.chat.handler;

import com.alibaba.fastjson.JSONObject;
import com.shuangyueliao.chat.entity.UserInfo;
import com.shuangyueliao.chat.proto.ChatCode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description 广播返回用户的信息
 */
public class MessageHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame)
            throws Exception {
        UserInfo userInfo = UserInfoManager.getUserInfo(ctx.channel());
        if (userInfo != null && userInfo.isAuth()) {
            JSONObject json = JSONObject.parseObject(frame.text());
            String mess = json.getString("mess");
            if (mess != null) {
                String reg = "^@(\\w+)@";
                Pattern pattern =  Pattern.compile(reg);
                Matcher matcher = pattern.matcher(mess);
                if (matcher.find()) {
                    try {
                        int i = mess.indexOf("@", 1);
                        UserInfoManager.p2p(userInfo.getId(), userInfo.getNick(), matcher.group(1), mess.substring(i + 1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }
            }
            // 广播返回用户发送的消息文本
            UserInfoManager.broadcastMess(userInfo.getId(), userInfo.getNick(), mess, userInfo.getGroupNumber());
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        UserInfoManager.removeChannel(ctx.channel());
        UserInfoManager.broadCastInfo(ChatCode.SYS_USER_COUNT,UserInfoManager.getAuthUserCount());
        super.channelUnregistered(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("connection error and close the channel", cause);
        UserInfoManager.removeChannel(ctx.channel());
        UserInfoManager.broadCastInfo(ChatCode.SYS_USER_COUNT, UserInfoManager.getAuthUserCount());
    }

}
