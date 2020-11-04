package com.example.gateway.filter;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import org.apache.commons.codec.Charsets;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @Author: majl
 * @Description:
 * @Created on 2020/11/4 7:49 PM
 * @Modified by:
 */
public class HttpResponseFilter {

    /**
     * 一个简单的过滤器，对响应内容添加后缀
     *
     * @param httpResponse
     * @return
     */
    public FullHttpResponse filter(final FullHttpResponse httpResponse) {
        FullHttpResponse response = null;
        try {
            String content = httpResponse.content().toString(Charsets.UTF_8);
            String s = content + " by my-netty-gateway";

            System.out.println(s);

            response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(s.getBytes(Charsets.UTF_8)));
            response.headers().set("Content-Type", "application/json");
            response.headers().setInt("Content-Length", s.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

}