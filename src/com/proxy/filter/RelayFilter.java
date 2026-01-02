package com.proxy.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class RelayFilter implements HttpFilter {
    @Override
    public void doFilter(Socket clientSocket, byte[] requestData, ProxyFilterChain chain) {
        System.out.println("RelayFilter.doFilter");

        try(Socket targetSocket = new Socket("google.com", 80);
            OutputStream targetOut = targetSocket.getOutputStream();
            InputStream targetIn = targetSocket.getInputStream()) {

            // 1. 브라우저 요청 구글로 전달
            targetOut.write(requestData);
            targetOut.flush();

            // 2. 구글의 응답을 받아서 브라우저에게 다시 전달
            OutputStream clientOut = clientSocket.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            // 구글이 보내주는 데이터를 읽어서 브라우저 소켓에 넣어주기
            if((bytesRead = targetIn.read(buffer)) > 0) {
                clientOut.write(buffer, 0, bytesRead);
                clientOut.flush();
            }

            System.out.println("RelayFilter 전송 완료");

        } catch (IOException e){
            System.err.println("Relay 에러: " + e.getMessage());
        }

        // 마지막 필터인 경우에도 습관적으로 다음 체인을 호출. (나중에 필터가 추가될 있으니)
        chain.handleNext(clientSocket, requestData);
    }
}
