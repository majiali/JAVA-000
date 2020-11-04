package com.example.gateway.outbound.httpclient4;

import com.example.gateway.filter.HttpResponseFilter;
import com.example.gateway.outbound.HttpOutboundHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.util.concurrent.*;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @Author: majl
 * @Description:
 * @Created on 2020/11/3 3:51 PM
 * @Modified by:
 */
public class HttpClientOutboundHandler implements HttpOutboundHandler {

    private static final long KEEP_ALIVE_TIME = 1000;
    private static final int QUEUE_SIZE = 2048;

    private CloseableHttpAsyncClient httpclient;
    private ExecutorService proxyService;
    private String backendUrl;

    public HttpClientOutboundHandler(String backendUrl) {
        this.backendUrl = backendUrl.endsWith("/") ? backendUrl.substring(0, backendUrl.length() - 1) : backendUrl;
        int cores = Runtime.getRuntime().availableProcessors() * 2;
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        //初始化一个线程池
        proxyService = new ThreadPoolExecutor(cores, cores,
                KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(QUEUE_SIZE),
                new NamedThreadFactory("proxyService"), handler);

        //对httpclient的异步操作进行配置
        IOReactorConfig config = IOReactorConfig.custom()
                .setConnectTimeout(1000)//连接超时
                .setSoTimeout(1000)//socket超时
                .setIoThreadCount(cores)
                .setRcvBufSize(32 * 1024)//缓冲区32k
                .build();

        httpclient = HttpAsyncClients.custom().setMaxConnTotal(40)
                .setMaxConnPerRoute(8)
                .setDefaultIOReactorConfig(config)
                .setKeepAliveStrategy((response, context) -> 6000)
                .build();
        httpclient.start();
    }

    @Override
    public void handle(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx) {
        final String url = this.backendUrl + fullRequest.uri();
        proxyService.submit(() -> fetchGet(fullRequest, ctx, url));
    }

    private void fetchGet(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx, final String url) {
        System.out.println(fullRequest.headers());
        final HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_KEEP_ALIVE);
        //出站事件：1.打开连接
        httpclient.execute(httpGet, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(final HttpResponse httpResponse) {
                try {
                    handleResponse(fullRequest, ctx, httpResponse);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {

                }
            }

            @Override
            public void failed(Exception e) {
                httpGet.abort();
                e.printStackTrace();
            }

            @Override
            public void cancelled() {
                httpGet.abort();
            }
        });
    }

    private void handleResponse(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx, final HttpResponse httpResponse) {
        FullHttpResponse response = null;
        try {
            byte[] body = EntityUtils.toByteArray(httpResponse.getEntity());
            response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(body));

            //对响应进行简单过滤
            HttpResponseFilter filter = new HttpResponseFilter();
            response = filter.filter(response);
        } catch (Exception e) {
            e.printStackTrace();
            response = new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT);
            exceptionCaught(ctx, e);
        } finally {
            if (fullRequest != null) {
                //出站事件：2.写入数据
                if (!HttpUtil.isKeepAlive(fullRequest)) {
                    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                } else {
                    ctx.write(response);
                }
            }
            //出站事件：3.刷新数据
            ctx.flush();
        }
    }

    private void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}