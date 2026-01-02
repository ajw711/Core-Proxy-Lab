package com.proxy.core;

import com.proxy.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AdvancedProxyServer {
    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("step3: 멀티쓰레드 프록시 시작 (Port: 8080)");

            while (true) {

                // 1. 손님 받기
                Socket clientSocket = serverSocket.accept();
                System.out.println("새로운 손님 접속: " + clientSocket.getRemoteSocketAddress());

                // 2. client 전담 핸들러 고용
                // 새로운 (Thread)엥 손님 태우기
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread thread = new Thread(clientHandler);
                thread.start();

                // 3. 다음 손님 기다리기
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
