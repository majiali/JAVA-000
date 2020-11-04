package com.example.gateway.inbound;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: majl
 * @Description:
 * @Created on 2020/11/3 3:38 PM
 * @Modified by:
 */
public class HttpInboundServer {

    private static Logger logger = LoggerFactory.getLogger(HttpInboundServer.class);

    private int port;

    private String proxyServer;

    public HttpInboundServer(int port, String proxyServer) {
        this.port = port;
        this.proxyServer = proxyServer;
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(16);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 128)//握手时未完成的连接能保存多少个
                    .option(ChannelOption.TCP_NODELAY, true)//nagle算法开启
                    .option(ChannelOption.SO_KEEPALIVE, true)//告诉client端连接是长连接
                    .option(ChannelOption.SO_REUSEADDR, true)//重用http地址
                    .option(ChannelOption.SO_RCVBUF, 32 * 1024)//缓冲区大小，32k
                    .option(ChannelOption.SO_SNDBUF, 32 * 1024)//缓冲区大小，32k
                    .option(EpollChannelOption.SO_REUSEPORT, true)//重用端口
                    .childOption(ChannelOption.SO_KEEPALIVE, true)//对worker层级起作用
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);//把bytebuffer内存池管理起来，提高内存使用效率

            b.group(bossGroup, workerGroup)//绑定bossGroup和workerGroup
                    .channel(NioServerSocketChannel.class)//表明使用的是Nio的socketchannel（也可以选择不用nio）
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new HttpInboundInitializer(this.proxyServer));//绑定worker的channelpipiline

            Channel channel = b.bind(this.port).sync().channel();
            logger.info("开启netty http服务器，监听地址和端口为 http://127.0.0.1:" + port + '/');
            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}