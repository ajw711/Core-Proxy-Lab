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

        byte[] fixedData = fixHeader(requestData);

        try(Socket targetSocket = routingStrategy.getTargetSocket();
            OutputStream targetOut = targetSocket.getOutputStream();
            InputStream targetIn = targetSocket.getInputStream()) {

            targetOut.write(fixedData);
            targetOut.flush();

            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[8192];
            int len;
            while((len = targetIn.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
            }

            // 변경 포인트 변조 로직을 별도 메서드(modifyBody)로 호출
            String modified = modifyBody(sb.toString());

            clientOut.write(modified.getBytes(StandardCharsets.UTF_8));
            clientOut.flush();
            System.out.println("RelayFilter 모든 데이터 전송 완료");

        } catch (IOException e){
            System.err.println("Relay 에러: " + e.getMessage());
        }
    }

    /**
     * [테스트 타겟] 데이터를 변조하는 순수 로직만 따로 분리
     */
    public String modifyBody(String original) {
        if (original == null) return "";
        return original.replace("Google", "ProxyServer");
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
