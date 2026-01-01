package com.proxy.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable{

    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        // 기존 ProxyServer의 while문 안에 있던
        // 데이터 읽기 -> 분석 -> 전달 로직 미리 생성

        try(InputStream input = clientSocket.getInputStream()) {
            OutputStream output = clientSocket.getOutputStream();
            byte[] buffer = new byte[4096];
            int length = input.read(buffer);

            if( length > 0 ) {
                System.out.println("Tread : " + Thread.currentThread().getName());

                // 응답
                output.write("HTTP/1.1 200 OK\r\n\r\nSuccess".getBytes());
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
