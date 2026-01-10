package com.proxy.filter;

import java.io.IOException;
import java.net.Socket;

/**
 * [개념: Strategy Pattern (전략 패턴)]
 * - "어디로 보낼 것인가"에 대한 알고리즘을 인터페이스로 분리함.
 * - 새로운 목적지(네이버 등)가 추가되어도 기존 코드를 수정하지 않고
 * 이 인터페이스만 구현하면 되는 확장성(OCP)을 확보함.
 */
public interface RoutingStrategy {
    Socket getTargetSocket() throws IOException;
}
