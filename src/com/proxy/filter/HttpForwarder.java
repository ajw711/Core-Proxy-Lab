package com.proxy.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * [개념: Separation of Concerns (관심사의 분리)]
 * - 필터 로직과 네트워크 전송 기술을 분리하여 유지보수성을 높임.
 *
 * [기술적 해결: Connection Control]
 * - 요청 헤더의 'keep-alive'를 'close'로 강제 변경함.
 * - 상대 서버가 데이터를 다 보낸 즉시 소켓을 끊게 유도하여,
 * 자바의 read()가 -1을 즉시 반환하게 만듦 (Read Timeout 해결).
 */
public class HttpForwarder {

    /**
     *  실제 서버와 통신을 담당하는 심부름꾼 메서드
     */
    public void forward(byte[] requestData,
                        RoutingStrategy routingStrategy,
                        OutputStream clientOut) throws IOException {
        // 여기서 targetSocket을 열고 구글이랑 대화를 하지만
        // 여기서 가짜 응답을 조립해서 던져주기
        // 보안 게이트웨이


//        String fakeHtml = "<html><body style='text-align:center; padding-top:100px; font-family: sans-serif;'>" +
//                "<h1 style='color: #4285F4;'> PROXY SERVER</h1>" +
//                "<p>방금 당신의 요청은 프록시 서버에 의해 가로채졌습니다.</p>" +
//                "<div style='font-size: 50px;'>🛑</div>" +
//                "</body></html>";
//
//        String response = "HTTP/1.1 200 OK\r\n" +
//                "Content-Type: text/html; charset=UTF-8\r\n" +
//                "Content-Length: " + fakeHtml.getBytes().length + "\r\n" +
//                "Connection: close\r\n" +
//                "\r\n" +
//                fakeHtml;
//
//        clientOut.write(response.getBytes());
//        clientOut.flush();

        // 헤더 수정
        byte[] fixedData = fixHeader(requestData);

        try(Socket targetSocket = routingStrategy.getTargetSocket();
            OutputStream targetOut = targetSocket.getOutputStream();
            InputStream targetIn = targetSocket.getInputStream()) {


            targetOut.write(fixedData);
            targetOut.flush();

            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[8192];
            int len;
            // 데이터를 읽어서 브라우저 소켓에 넣어주기
            while((len = targetIn.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
            }

            // 구글이 보낸 HTML 내용 "Google"을 "ProxyServer"로 바꾸기
            String original = sb.toString();
            String modified = original.replace("Google", "ProxyServer");


            // 브라우저에게 전달
            clientOut.write(modified.getBytes(StandardCharsets.UTF_8));
            clientOut.flush();
            System.out.println("RelayFilter 모든 데이터 전송 완료");

        } catch (IOException e){
            System.err.println("Relay 에러: " + e.getMessage());
        }
    }

    private byte[] fixHeader(byte[] data) {
        String s = new String(data);
        // keep-alive를 close로 변경
        s = s.replaceAll("(?i)Connection: keep-alive", "Connection: close");

        if (!s.contains("Connection: close")) {
            s = s.replaceFirst("\r\n", "\r\nConnection: close\r\n");
        }
        return s.getBytes();
    }
}
