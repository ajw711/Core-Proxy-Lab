package com.proxy.core;

import com.proxy.handler.HttpProxyHandler;
import com.proxy.handler.ProxyHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioProxyServer {
    public static void main(String[] args) throws IOException {

        // Selector 관리소 열기
        Selector selector = Selector.open();

        // 서버 통로(channel) 열기
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8080), 1000);

        // 비차단 모드로 설정 데이터 없어도 스레드가 움직임
        serverSocketChannel.configureBlocking(false);

        // 관리소에 누가 접속하면 알려달라고 등록
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Step 9: NIO 비동기 프록시 시작 (Port: 8080)");

        ProxyHandler proxyHandler = new HttpProxyHandler();

        while (true) {

            // 누군가 벨을 누를 때까지 대기 (1명만 대기)
            selector.select();

            // 벨 누른 소켓들의 명창 꺼내기
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectionKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove(); // 처리 시잦ㄱ하면 목록에서 지움 (중복 처리 방지)

                proxyHandler.handle(key, selector);
            }

        }
    }

}
