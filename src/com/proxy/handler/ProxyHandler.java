package com.proxy.handler;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public interface ProxyHandler {
    // 엔진이 부를 수 있는 유일한 공개 통로
    void handle(SelectionKey key, Selector selector) throws IOException;
}
