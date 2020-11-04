package com.example.gateway.inbound;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;


/**
 * @Author: majl
 * @Description:
 * @Created on 2020/11/3 3:47 PM
 * @Modified by:
 */
public class HttpInboundInitializer extends ChannelInitializer<SocketChannel> {

    private String proxyServer;

    public HttpInboundInitializer(String proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    protected void initChannel(SocketChannel sc) throws Exception {
        ChannelPipeline p = sc.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpObjectAggregator(1024 * 1024));
        //绑定一个handler
        p.addLast(new HttpInboundHandler(this.proxyServer));
    }
}