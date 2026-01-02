package com.proxy.handler;

import com.proxy.filter.LoggingFilter;
import com.proxy.filter.ProxyFilterChain;
import com.proxy.filter.RelayFilter;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class ClientHandler implements Runnable{

    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        // 필터 체인 기반 비즈니스 로직 실행
        // 해당 메서드는 필터들을 조립하고 실행하는 역할

        try {
            InputStream input =clientSocket.getInputStream();
            byte[] buffer = new byte[4096]; // 데이터를 담을 임시 바구니
            int length = input.read(buffer);

            if (length > 0) {
                // 데이터 정제
                // 4096바이트 중 실제 들어온 데이터만 딱 잘라서 추출
                byte[] requestData = new byte[length];
                System.arraycopy(buffer, 0, requestData, 0, length);

                // 파이프라인 구축
                // 요청을 위한 전용 필터 체인 생성 (Thread-safe 설계)
                ProxyFilterChain chain = new ProxyFilterChain();

                // 원하는 기능을 순서대로 넣기 (느슨한 결합: 기능 추가/삭제 자유로움)
                chain.addFilter(new LoggingFilter()); // 1. 요청 기록
                chain.addFilter(new RelayFilter()); // 2. 외부 서버 전달

                // 엔진 가동
                // 조립된 체인에 첫 번째 필터를 실행하도록 신호 전달
                chain.handleNext(clientSocket, requestData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { clientSocket.close(); } catch (IOException e) { e.printStackTrace();}
        }
    }
}
