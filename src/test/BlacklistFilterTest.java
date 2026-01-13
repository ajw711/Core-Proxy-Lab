package test;

import com.proxy.filter.BlacklistFilter;
import com.proxy.filter.ProxyFilterChain;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class BlacklistFilterTest {

    private final String TEST_FILE = "blacklist.txt";


    @BeforeEach
    void setUp() throws IOException {
        // 테스트 시작 전 파일을 깨끗하게 비우거나 새로 생성
        Files.write(Paths.get(TEST_FILE), "".getBytes());
    }

    @AfterEach
    void tearDown() throws IOException {
        // 테스트가 끝나면 파일 삭제 (내 흔적 지우기)
        Files.deleteIfExists(Paths.get(TEST_FILE));
    }

    @Test
    @DisplayName("차단된 사이트 요청 시 403 응답을 보내고 다음 필터로 가지 않아야 한다")
    public void testBlacklistBlocking() throws IOException {
        // given 블랙리스트 필터와 차단된 사이트 요청 준비
        BlacklistFilter blacklistFilter = new BlacklistFilter();
        byte[] requestData = "GET http://naver.com HTTP/1.1\r\nHost: naver.com\r\n\r\n".getBytes();

        // 가짜 소켓과 체인 준비
        Socket mockSocket = mock(Socket.class);
        ProxyFilterChain mockChain = mock(ProxyFilterChain.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 소켓이 getOutputStream()을 호출하면 우리 바구니(out)를 주도록 설정
        when(mockSocket.getOutputStream()).thenReturn(out);
        // when
        blacklistFilter.doFilter(mockSocket, requestData, mockChain);
        // then
        String response = out.toString();
        assertTrue(response.contains("403 Forbidden"), "차단 메시지가 응답에 포함되어야 함");

        verify(mockChain, never()).handleNext(any(), any());
    }

    @Test
    @DisplayName("블랙리스트 파일 리스트를 활용한 필터 작용")
    public void testDynamicReloading() throws IOException, InterruptedException {

        // given 1. 초기 상태: 아무것도 차단 안 함
        BlacklistFilter filter = new BlacklistFilter();
        byte[] requestData = "GET http://naver.com HTTP/1.1\r\n".getBytes();

        Socket mockSocket = mock(Socket.class);
        ProxyFilterChain mockChain = mock(ProxyFilterChain.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(out);

        // 첫 접속 시도 (허용되어야 함)
        filter.doFilter(mockSocket, requestData, mockChain);
        verify(mockChain, times(1)).handleNext(any(), any());

        // when 2. 실시간으로 파일에 naver.com 추가
        Files.write(Paths.get(TEST_FILE), "naver.com".getBytes());

        // 중요: 파일 수정 시간 감지를 위해 아주 잠깐 대기
        Thread.sleep(100);

        // then 3. 다시 접속 시도 (이제는 차단되어야 함)
        filter.doFilter(mockSocket, requestData, mockChain);

        assertTrue(out.toString().contains("403 Forbidden"), "파일 수정 후에는 차단되어야 합니다.");
    }

}
