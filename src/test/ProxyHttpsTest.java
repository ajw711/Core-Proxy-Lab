package test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
public class ProxyHttpsTest {

    private final String PROXY_HOST = "127.0.0.1";
    private final int PROXY_PORT = 8080;

    @Test
    @DisplayName("HTTPS CONNECT 요청 시 200 응답과 터널이 생성되어야 한다")
    void testConnectTunneling(){
        // given 프록시 서버에 연결된 소켓과 입출력 스트림 준비
        try(Socket socket = new Socket()){
            socket.connect(new InetSocketAddress(PROXY_HOST, PROXY_PORT), 2000);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // when CONNECT 요청 메시지 전송
            String target = "google.com:443";
            out.print("CONNECT " + target + " HTTP/1.1\r\n");
            out.print("Host: " + target + "\r\n");
            out.print("\r\n"); // HTTP 헤더의 끝을 알리는 빈 줄
            out.flush();

            // then 서버로부터 '200 Connection' 응답이 오는지 확인
            String responseLine = in.readLine();
            System.out.println("프록시 서버 응답 : " + responseLine);

            assertAll(
                    () -> assertNotNull(responseLine, "응답이 null입니다."),
                    () -> assertTrue(responseLine.contains("200 Connection Established"),
                            "응답에 '200 Connection Established'가 포함되어야 합니다.")
            );
        } catch (IOException e){
            fail("테스트 도중 예외 발생: " + e.getMessage());
        }

    }
}
