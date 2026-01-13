package com.proxy.filter;

import java.io.IOException;
import java.net.Socket;

public class GoogleRoutingStrategy implements RoutingStrategy {


    @Override
    public Socket getTargetSocket() throws IOException {
        // 구글 대신 내 컴퓨터(localhost)의 8000번 포트로 연결!
        Socket socket = new Socket("localhost", 8000);
        socket.setSoTimeout(3000); // 3초간 데이터 안 오면 종료(스트리밍 안정성)
        return socket;
    }
}
