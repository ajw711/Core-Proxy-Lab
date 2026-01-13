package test;

import com.proxy.filter.BlacklistFilter;
import com.proxy.filter.ProxyFilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class BlacklistFilterTest {


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

}
