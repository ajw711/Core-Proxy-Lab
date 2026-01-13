package com.proxy.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class BlacklistFilter implements HttpFilter{

    // 차단할 키워드 목록
    private final List<String> blockedSites = Arrays.asList("naver.com", "daum.net");

    @Override
    public void doFilter(Socket clientSocket, byte[] requestData, ProxyFilterChain chain) throws IOException {
        String requestString = new String(requestData, StandardCharsets.UTF_8);

        // 요청 헤더에서 목적지(host) 확인
        boolean isBlocked = false;
        for (String site : blockedSites) {
            if (requestString.contains(site)) {
                isBlocked = true;
                break;
            }
        }

        if(isBlocked){
            // 차단된 경우 가짜 응답을 보내고 체인을 중단
            System.out.println("[Blacklist] 차단된 사이트 접속 감지: " + requestString.split("\r\n")[0]);
            sendForbiddenResponse(clientSocket.getOutputStream());
        } else {
            // 다음 필터로 넘김
            chain.handleNext(clientSocket, requestData);
        }
    }

    private void sendForbiddenResponse(OutputStream out) throws IOException {
        String html = "<html><body><h1 style='color:red;'>Access Denied</h1><p>This site is blocked by Jinwon Proxy Server.</p></body></html>";
        String response = "HTTP/1.1 403 Forbidden\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + html.getBytes().length + "\r\n" +
                "Connection: close\r\n\r\n" + html;
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
