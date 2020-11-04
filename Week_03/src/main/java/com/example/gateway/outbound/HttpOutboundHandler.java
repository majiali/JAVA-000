package com.example.gateway.outbound;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * @Author: majl
 * @Description:
 * @Created on 2020/11/4 5:58 PM
 * @Modified by:
 */
public interface HttpOutboundHandler {

    void handle(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx);

}
