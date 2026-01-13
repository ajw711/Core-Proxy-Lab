package com.proxy.filter;

import java.io.IOException;
import java.net.Socket;

/*
HttpFilter: 각 작업대(로깅, 검수, 전송)의 규격
ProxyFilterChain: 컨베이어 벨트 그 자체
데이터: 벨트 위를 지나가는 물건
 */

public interface HttpFilter {
    //필터가 할 일과, 다음 필터로 넘기기 위한 chain 객체를 받기
    void doFilter(Socket clientSocket, byte[] requestData, ProxyFilterChain chain) throws IOException;
}
