package com.proxy.core;

import java.nio.channels.SocketChannel;

public class SessionContext {

    // 데이터 캐시
    public final StringBuilder requestBuffer = new StringBuilder();

    // 브라우저와 연결된 통로 (OS에서 받은 FD 정보를 가지고 있음)
    public SocketChannel clientChannel;

    // 나중에 연결될 구글/네이버 서버와의 통로
    public SocketChannel targetChannel;

    // 현재 이 세션의 상태 (헤더 읽는 중인지, 터널링 중인지)
    public State state = State.READING_HEADER;

    public enum State {
        READING_HEADER, // HTTP 헤더 읽는 중
        CONNECTING,     // 타겟 서버에 연결 시도 중
        TUNNELING       // 양방향 데이터 릴레이 중
    }
}
