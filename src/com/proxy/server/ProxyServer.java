package com.proxy.server;

import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyServer {
    public static void main(String[] args) {

        // 8080 포트로 문 열기
        try(ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("ProxyServer started on port 8080");

            // Socket은 하드웨어가 아니라 OS커널 자원
            // 파일 디스크립터 형태로 관리
            // 파일 디스크립터 : 운영체제가 파일이나 소켓 같은 시스템 자원을 식별하기 위해 프로세스에 할당하는 '정수 번호'
            // Socket = (IP + Port + Protocol) ex(192.168.0.10, 443, TCP)
            // IP : 어느 컴퓨터
            // Port : 해당 컴퓨터 어느 프로세스
            // Protocol: TCP or UDP

            while (true) {
                // 누군가 접속할 때까지 일시정지 blocking
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("연결 성공: " + clientSocket.getRemoteSocketAddress());

                    // 데이터 읽는 로직

                    // 1. 브라우저가 보낸 데이터 꺼내기
                    InputStream inputStream = clientSocket.getInputStream();
                    OutputStream outputStream = clientSocket.getOutputStream();

                    // 2. 프록시 서버로 실제 서버로 연결할 '두 번째 소켓'이 필요
                    // 로컬 경우에는 로컬 다른 포트 혹은 특정 ip를 사용
                    // Socket targetSocket = new Socket("target-host.com", 80);

                    // 3. 데이터 읽기
                    byte[] buffer = new byte[4096]; //4096byte = 4KB
                    int bytesRead = inputStream.read(buffer);

                    if ( bytesRead > 0) {
                        String request = new String(buffer, 0, bytesRead);
                        System.out.println("received from browser");
                        System.out.println(request); //브라우저가 보낸 http 요청이 보여야함

                        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nHello from Proxy!";
                        outputStream.write(response.getBytes());
                        outputStream.flush(); // 데이터 즉시 전송
                    }

                    // 소켓 끊기
                    clientSocket.close();
                    System.out.println("ProxyServer stopped");
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}