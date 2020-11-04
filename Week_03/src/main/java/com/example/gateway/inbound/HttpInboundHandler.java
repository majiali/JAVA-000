package com.example.gateway.inbound;

import com.example.gateway.filter.HttpRequestFilter;
import com.example.gateway.outbound.HttpOutboundHandler;
import com.example.gateway.outbound.httpclient4.HttpClientOutboundHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: majl
 * @Description:
 * @Created on 2020/11/3 3:50 PM
 * @Modified by:
 */
public class HttpInboundHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(HttpInboundHandler.class);

    private final String proxyServer;
    private HttpOutboundHandler outboundHandler;

    public HttpInboundHandler(String proxyServer) {
        this.proxyServer = proxyServer;
        this.outboundHandler = new HttpClientOutboundHandler(this.proxyServer);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            FullHttpRequest request = (FullHttpRequest) msg;

            //对请求内容进行简单过滤（加一个header）
            HttpRequestFilter filter = new HttpRequestFilter();
            filter.filter(request);
            //调用outboundHandler进行处理（向被代理的服务器发起请求，拿到响应内容并输出）
            outboundHandler.handle(request, ctx);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ReferenceCountUtil.release(msg);
        }

    }
}