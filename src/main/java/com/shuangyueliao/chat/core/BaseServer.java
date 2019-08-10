package com.shuangyueliao.chat.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description 抽象类，提前准备boosGroup，workGroup，ServerBootstrap
 */
public abstract class BaseServer implements Server {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected String host = "localhost";
    protected int port = 8099;

    protected DefaultEventLoopGroup defLoopGroup;
    protected NioEventLoopGroup bossGroup;
    protected NioEventLoopGroup workGroup;
    protected NioServerSocketChannel ssch;
    protected ChannelFuture cf;
    protected ServerBootstrap b;

    public void init(){
        //处理pipline上的handler线程数量，因为pipline上有HttpServerCodec、HttpObjectAggregator、ChunkedWriterHandler
        //IdleStateHandler、UserAuthHandler、MessageHandler共6种，所以线程数量跟6差不多的8
        defLoopGroup = new DefaultEventLoopGroup(8, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "DEFAULTEVENTLOOPGROUP_" + index.incrementAndGet());
            }
        });
        //接收连接的线程数量为可用处理器
        bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "BOSS_" + index.incrementAndGet());
            }
        });
        //处理连接的线程数量为可用处理器的10倍
        workGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 10, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "WORK_" + index.incrementAndGet());
            }
        });

        b = new ServerBootstrap();
    }

    @Override
    public void shutdown() {
        if (defLoopGroup != null) {
            defLoopGroup.shutdownGracefully();
        }
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
    }
}
