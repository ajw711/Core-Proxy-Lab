package com.proxy.handler;

import com.proxy.filter.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

public class ClientHandler implements Runnable{

    private final Socket clientSocket;
    private final ExecutorService threadPool;

    public ClientHandler(Socket clientSocket, ExecutorService threadPool) {
        this.clientSocket = clientSocket;
        this.threadPool = threadPool;
    }

    @Override
    public void run() {

        // 필터 체인 기반 비즈니스 로직 실행
        // 해당 메서드는 필터들을 조립하고 실행하는 역할

        try (InputStream in = clientSocket.getInputStream()){
            byte[] buffer = new byte[8192];
            int readLen = in.read(buffer);
            if (readLen == -1) return;

            String firstLine = new String(buffer, 0, readLen).split("\r\n")[0];

            byte[] requestData = Arrays.copyOf(buffer, readLen);

            if(firstLine.startsWith("CONNECT")){
                // HTTPS: 터널링 모드 (단순 전달)
                System.out.println("터널링 모드 시작");
                handleHttpsConnect(clientSocket, firstLine);
            } else {
                // HTTP: 기존 필터 체인 모드 (데이터 변조 가능)
                ProxyFilterChain filterChain = new ProxyFilterChain();
                filterChain.addFilter(new LoggingFilter());
                filterChain.addFilter(new BlacklistFilter());
                filterChain.addFilter(new RelayFilter(new GoogleRoutingStrategy()));

                // 엔진 가동
                // 조립된 체인에 첫 번째 필터를 실행하도록 신호 전달
                filterChain.handleNext(clientSocket, requestData);

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { clientSocket.close(); } catch (IOException e) { e.printStackTrace();}
        }
    }

    private void handleHttpsConnect(Socket clientSocket, String firstLine) {
        Socket targetSocket = null;
        try{
            // 목직지 호스트 포트 추출 (google.com:443)
            String targetStr = firstLine.split(" ")[1];
            String host = targetStr.split(":")[0];
            int port = Integer.parseInt(targetStr.split(":")[1]);

            //  실제 서버 연결
            targetSocket = new Socket(host, port);

            // 브라우저에게 "통로 뚫렸다"고 신호 보내기 (HTTP 약속)
            OutputStream out = clientSocket.getOutputStream();
            out.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
            out.flush();

            // 양방향 릴레이 시작 (암호화된 데이터 셔틀)
            Socket finalTargetSocket = targetSocket;

            // 한쪽 방향(브라우저->서버)은 스레드 풀 일꾼에게 맡김
            threadPool.execute(() -> relay(clientSocket, finalTargetSocket));

            // 다른 방향(서버->브라우저)은 현재 스레드에서 직접 처리
            relay(finalTargetSocket, clientSocket);


        } catch (IOException e){
            System.err.println("HTTPS 터널링 실패: " + e.getMessage());
        } finally {
            // 통신이 완전히 끝나면(relay가 종료되면) 양쪽 다 닫기
            closeQuietly(targetSocket);
            closeQuietly(clientSocket);
        }
    }

    // 데이터를 읽어서 그대로 반대편에 쏴주는 단순 배달원
    private void relay(Socket source, Socket dest) {
        try (InputStream in = source.getInputStream();
             OutputStream out = dest.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                out.flush();
            }
        } catch (IOException e) {
            // 연결이 끊기면 자연스럽게 루프 탈출
        }
    }

    private void closeQuietly(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
