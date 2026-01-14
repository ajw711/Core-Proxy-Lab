package com.proxy.core;

import com.proxy.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdvancedProxyServer {
    public static void main(String[] args) {

        // 스레드 풀 생성
        // 손님 100명이 와도 10명이서 나눠서 처리하고 나머지 대기
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        try(ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("step8: HTTPS 터널링 (Port: 8080)");

            while (true) {

                // 1. 손님 받기
                Socket clientSocket = serverSocket.accept();
                System.out.println("새로운 손님 접속: " + clientSocket.getRemoteSocketAddress());

                // 2. client 전담 핸들러 고용
                //(스레드 풀 방식: 10명 안에서 돌려막기)
                threadPool.execute(new ClientHandler(clientSocket, threadPool));

                // 3. 다음 손님 기다리기
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }


}
