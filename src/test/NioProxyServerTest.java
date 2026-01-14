package test;

import com.proxy.core.NioProxyServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;


public class NioProxyServerTest {

    private static ExecutorService serverExecutor;

    @BeforeAll
    static void setup() {
        // 서버를 별도 스레드에서 실행 (테스트 스레드가 차단되면 안 되니까)
        serverExecutor = Executors.newSingleThreadExecutor();
        serverExecutor.submit(() -> {
            try {
                NioProxyServer.main(new String[]{});
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    @DisplayName("CONNECT 요청 시 200 Connection Established 응답이 오는지 테스트")
    void testConnectRequest() throws Exception {
        // 서버가 뜰 때까지 잠시 대기
        Thread.sleep(1000);

        // 클라이언트 소켓으로 우리 프록시(8080) 접속
        try (Socket socket = new Socket("localhost", 8080);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            // CONNECT 요청 전송 (구글 443 포트 대상)
            String request = "CONNECT google.com:443 HTTP/1.1\r\n" +
                    "Host: google.com:443\r\n\r\n";
            out.write(request.getBytes());
            out.flush();

            //  200 OK 응답 대기
            byte[] buffer = new byte[4096];
            int n = in.read(buffer);
            String response = new String(buffer, 0, n);
            System.out.println("1단계 응답 (Proxy): " + response);
            assertTrue(response.contains("200 Connection Established"));

            // 이제 릴레이(Relay)가 되는지 확인
            // 구글(443)은 암호화 통신을 원하므로 쌩 텍스트를 보내면 연결을 끊겠지만
            // 일단 뭐라도 던져서 'relay' 메서드가 호출되는지 확인
            out.write("Hello Google!".getBytes());
            out.flush();

            // 구글이 연결을 끊거나 응답을 줄 때까지 대기
            System.out.println("2단계: 데이터를 보냈습니다. 서버 로그를 확인하세요.");

            // 구글이 보내는 응답(혹은 연결 끊김)이 프록시를 타고 나에게 오는지 확인
            int n2 = in.read(buffer);
            if (n2 != -1) {
                System.out.println("3단계: 구글로부터 (프록시를 거쳐) 돌아온 응답: " + new String(buffer, 0, n2));
            }

            // 잠시 대기하며 릴레이가 일어날 시간을 줌
            Thread.sleep(500);
        }
    }


    @Test
    @DisplayName("성능 테스트: 1,000개의 동시 연결 처리 확인")
    void testLoadPerformance() throws Exception {
        int clientCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(100); // 클라이언트를 쏘는 일꾼들
        CountDownLatch latch = new CountDownLatch(clientCount); // 1,000명이 다 끝날 때까지 대기용
        AtomicInteger successCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < clientCount; i++) {
            executor.submit(() -> {
                try (Socket socket = new Socket("localhost", 8080);
                     OutputStream out = socket.getOutputStream();
                     InputStream in = socket.getInputStream()) {

                    // 각 손님이 구글 연결 요청
                    String request = "CONNECT google.com:443 HTTP/1.1\r\nHost: google.com:443\r\n\r\n";
                    out.write(request.getBytes());
                    out.flush();

                    // 200 OK 응답 확인
                    byte[] buffer = new byte[1024];
                    int n = in.read(buffer);
                    if (n != -1 && new String(buffer, 0, n).contains("200")) {
                        successCount.incrementAndGet();
                    }
                } catch (IOException e) {
                    System.err.println("접속 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });

            if (i % 10 == 0) {
                Thread.sleep(50);
            }
        }

        latch.await(30, TimeUnit.SECONDS); // 최대 30초 대기
        long endTime = System.currentTimeMillis();

        System.out.println("======================================");
        System.out.println("성능 테스트 결과");
        System.out.println("총 요청 수: " + clientCount);
        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("소요 시간: " + (endTime - startTime) + "ms");
        System.out.println("======================================");

        executor.shutdown();
        assertEquals(clientCount, successCount.get(), "1,000명 모두 성공해야 함!");
    }

    @AfterAll
    static void tearDown() {
        // while(true) 때문에 테스트가 끝나도 서버 스레드가 안 죽을 경우를 대비해
        // 강제 종료
        serverExecutor.shutdownNow();
    }
}
