package com.proxy.filter;

import java.io.IOException;
import java.net.Socket;

/**
 * [개념: Dependency Injection (의존성 주입)]
 * - 구체적인 클래스(GoogleRoute)를 직접 생성하지 않고, 외부에서 주입받음.
 * - 결합도를 낮춰서 어떤 전략이 들어오든 필터는 자기 할 일만 하게 만듦.
 *
 * [개념: Delegation (위임)]
 * - 실제 데이터를 주고받는 복잡한 작업은 전문 객체(Forwarder)에게 맡김.
 */
public class RelayFilter implements HttpFilter {

    private final RoutingStrategy routingStrategy;
    private final HttpForwarder httpForwarder = new HttpForwarder(); // 비서 고용

    public RelayFilter(RoutingStrategy routingStrategy) {
        this.routingStrategy = routingStrategy;
    }

    @Override
    public void doFilter(Socket clientSocket, byte[] requestData, ProxyFilterChain chain) throws IOException {
        System.out.println("RelayFilter.doFilter: HttpForwarder에게 업무를 위임합니다.");

        try {
            // [핵심] 복잡한 소켓 연결, 헤더 수정, 데이터 전달은 비서가 전담
            // 비서에게 "무엇을(data), 어디로(strategy), 누구에게(out)" 줄지만 알려주기
            httpForwarder.forward(requestData, routingStrategy, clientSocket.getOutputStream());

        } catch (IOException e) {
            System.err.println("RelayFilter 실행 중 에러: " + e.getMessage());
        }

        // 비서가 일을 마쳤으니, 다음 필터가 있다면 실행
        chain.handleNext(clientSocket, requestData);
    }
}