package test;


import com.proxy.filter.HttpForwarder;
import com.proxy.filter.RoutingStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.*;


import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdvancedProxyServerTest {

    @Test
    @DisplayName("스레드 풀은 설정된 최대 개수(10개)만큼만 스레드를 생성해야 한다")
    public void threadPoolShouldLimitThreadCount() throws InterruptedException {

        // 1. Given: 환경 준비
        int maxThreadCount = 10;
        int totalRequests = 20;
        ExecutorService testPool = Executors.newFixedThreadPool(maxThreadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        // 스레드 이름을 안전하게 담을 Set
        Set<String> threadNames = ConcurrentHashMap.newKeySet();

        // 2. When: 20개 요청 실행
        for (int i = 0; i < totalRequests; i++) {
            testPool.execute(() -> {
                try {
                    threadNames.add(Thread.currentThread().getName());
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        // 3. Then: 검증
        System.out.println("사용된 총 스레드 개수: " + threadNames.size());
        System.out.println("사용된 스레드 명단: " + threadNames);

        assertTrue(threadNames.size() <= maxThreadCount,
                "스레드 개수가 설정값(" + maxThreadCount + ")을 초과했습니다!");

        testPool.shutdown();
    }

    @Test
    @DisplayName("네트워크 없이 MockSocket을 이용한 응답 변조 테스트")
    public void testForwardLogicWithMock() throws IOException {

        // given
        HttpForwarder httpForwarder = new HttpForwarder();
        String mockServerData = "HTTP/1.1 200 OK\r\n\r\nSearch on Google!";
        // when
        // 2. 진짜 구글 대신 가짜 데이터를 줄 '가짜 전략' 생성
        RoutingStrategy mockStrategy = () -> new MockSocket(mockServerData);

        // 3. 브라우저로 나가는 데이터를 가로챌 바구니(OutputStream)
        ByteArrayOutputStream clientOut = new ByteArrayOutputStream();
        httpForwarder.forward("GET / HTTP/1.1".getBytes(), mockStrategy, clientOut);

        // then
        String result = clientOut.toString(StandardCharsets.UTF_8);
        System.out.println("가공된 최종 응답: " + result);

        assertTrue(result.contains("ProxyServer"), "데이터가 변조되지 않았습니다!");
    }


    public static class MockSocket extends Socket {
        private final String content;

        public MockSocket(String content) {
            this.content = content;
        }

        // 진짜 네트워크로 나가는 대신, 우리가 넣어준 문자열을 읽게 함
        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        // 진짜 서버로 보내는 대신, 그냥 아무 데도 안 쌓이는 가짜 스트림 반환
        @Override
        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }

        // 소켓이 닫힐 때 아무 일도 안 일어나게 함
        @Override
        public void close() { }

    }
}