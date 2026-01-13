package com.proxy.filter;

import java.io.IOException;
import java.net.Socket;

public class GoogleRoutingStrategy implements RoutingStrategy {


    @Override
    public Socket getTargetSocket() throws IOException {
        // 타임아웃 설정
        Socket socket = new Socket("google.com", 80);
        socket.setSoTimeout(3000); // 3초간 데이터 안 오면 종료(스트리밍 안정성)
        return socket;
    }
}
