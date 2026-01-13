package com.proxy.filter;

import java.io.IOException;
import java.net.Socket;

public class LoggingFilter implements HttpFilter {
    @Override
    public void doFilter(Socket clientSocket, byte[] requestData, ProxyFilterChain chain) throws IOException {
        System.out.println("LoggingFilter.doFilter");
        System.out.println(new String(requestData));

        // 다음 필터로 넘겨주기
        chain.handleNext(clientSocket, requestData);
    }
}
