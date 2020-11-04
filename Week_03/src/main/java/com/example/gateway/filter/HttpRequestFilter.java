package com.example.gateway.filter;

import io.netty.handler.codec.http.FullHttpRequest;

/**
 * @Author: majl
 * @Description:
 * @Created on 2020/11/4 4:56 PM
 * @Modified by:
 */
public class HttpRequestFilter {

    /**
     * 在request中加一个header
     *
     * @param fullHttpRequest
     * @return
     */
    public void filter(FullHttpRequest fullHttpRequest) throws Exception {
        fullHttpRequest.headers().add("nio", "tingshu");
    }

}