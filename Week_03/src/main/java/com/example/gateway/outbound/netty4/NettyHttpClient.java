package com.example.gateway.outbound.netty4;

import com.example.gateway.filter.HttpResponseFilter;
import com.example.gateway.outbound.HttpOutboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import java.net.URI;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @Author: majl
 * @Description:
 * @Created on 2020/11/4 5:16 PM
 * @Modified by:
 */
public class NettyHttpClient implements HttpOutboundHandler {

    private URI uri;
    private String host;
    private int port;

    private EventLoopGroup workerGroup;
    private Channel channel;

    /**
     * @param backendUrl http://localhost:8088/api/hello
     */
    public NettyHttpClient(String backendUrl) {
        backendUrl = backendUrl.endsWith("/") ? backendUrl.substring(0, backendUrl.length() - 1) : backendUrl;

        this.workerGroup = new NioEventLoopGroup();
        try {
            this.uri = new URI(backendUrl);
            this.host = uri.getHost();
            this.port = uri.getPort();

            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new NettyHttpClientInitializer());

            channel = b.connect(host, port).sync().channel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx) {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
        for (String name : fullRequest.headers().names()) {
            request.headers().set(name, fullRequest.headers().get(name));
        }

        request.headers().set(HttpHeaderNames.HOST, host);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

        try {
            channel.writeAndFlush(fullRequest).addListener((ChannelFutureListener) channelFuture -> {
                System.out.println(channelFuture);
            });
            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }

    }

}

class NettyHttpClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpClientCodec());
        p.addLast(new HttpContentDecompressor());//这里要添加解压，不然打印时会乱码
        p.addLast(new HttpObjectAggregator(123433));//添加HttpObjectAggregator,NettyHttpClientHandler才会收到FullHttpResponse
        p.addLast(new NettyHttpClientHandler());
    }
}

class NettyHttpClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        FullHttpResponse response = (FullHttpResponse) msg;
        try {
            //对响应进行简单过滤
            HttpResponseFilter filter = new HttpResponseFilter();
            response = filter.filter(response);
        } catch (Exception e) {
            e.printStackTrace();
            response = new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT);
            exceptionCaught(ctx, e);
        } finally {
            //出站事件：2.写入数据
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            //出站事件：3.刷新数据
            ctx.flush();
        }
    }
}