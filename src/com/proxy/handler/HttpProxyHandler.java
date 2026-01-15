package com.proxy.handler;

import com.proxy.core.SessionContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class HttpProxyHandler implements ProxyHandler {

    @Override
    public void handle(SelectionKey key, Selector selector) throws IOException {
        // 엔진은 이것만 호출 내부 로직은 여기서 분기 처리
        if(key.isAcceptable()) {
            // 새로운 손님이 온 경우
            handleAccept(key, selector);
        } else if(key.isConnectable()) {
            // 구글 서버와 연결이 성공 했을 때 벨이 울림
            handleConnect(key);
        } else if(key.isReadable()) {
            // 손님이 말을 건 경우
            handleRead(key);
        }

    }


    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false); // 클라이언트 통로도 비차단으로

        // 이 손님 전용 데이터 저장소(바구니) 생성해서 전달
        SessionContext context = new SessionContext();
        context.clientChannel = client; // 바구니에 브라우저 정보 저장

        // 손님이 말을 하면(Read) 알려달라고 관리소에 등록 그리고 바구니 주소를 매달아둠(Attachment)
        client.register(selector, SelectionKey.OP_READ, context);
        System.out.println("NIO 손님 접속: " + client.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) throws IOException {

        // 명찰에 매달아뒀던 바구니꺼냄
        SessionContext context = (SessionContext) key.attachment();
        // 벨소리가 울린 통로(channel)를 꺼냄
        SocketChannel client = (SocketChannel) key.channel();


        // 터널링 중이면 분석하지 말고 바로 던지기
        if (context.state == SessionContext.State.TUNNELING) {
            relay(key);
            return;
        }

        // 바구니 안에 있는 버퍼꺼냄
        StringBuilder requestCache = context.requestBuffer;
        // 데이터를 담을 바구니(Buffer) 준비
        ByteBuffer buffer = ByteBuffer.allocate(8192);

        // 통로에서 바구니로 데이터를 담음
        int read = client.read(buffer);

        if (read == -1) {
            // 손님이 연결을 끊었을 때
            client.close();
            System.out.println("손님이 나갔습니다");
            return;
        }

        if (read > 0) {
            // 바구니에 데이터가 담겼으니 내용을 확인
            buffer.flip(); // 읽기 모드로 진행

            //디코딩하여 캐시에 추가 (이 과정에서 buffer의 position이 끝으로 이동함)
            String newData = StandardCharsets.UTF_8.decode(buffer).toString();
            // 새로운 들어온 조각을 기존 바구니에 합침
            requestCache.append(newData);


            String currentFullText = requestCache.toString();

            // HTTP 요청의 끝(\r\n\r\n)이 왔는지 확인
            if (currentFullText.contains("\r\n\r\n")) {
                System.out.println("--- [데이터 수신 완료] ---");
                System.out.println(currentFullText);

                // 여기서 파싱 후 타겟 서버(구글) 연결
                handleTargetConnection(key, currentFullText);

            } else {
                System.out.println("데이터 조립 중... 현재 크기: " + currentFullText.length());
            }

            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            String message = new String(data, StandardCharsets.UTF_8);
            System.out.println("받은 데이터: " + message);
        }
    }

    private void handleTargetConnection(SelectionKey key, String request) throws IOException {

        // 바구니 꺼내기
        SessionContext context = (SessionContext) key.attachment();

        // 요청 라인 분석 (예: CONNECT google.com:443 HTTP/1.1)
        String[] lines = request.split("\r\n");
        String firstLine = lines[0];
        String[] parts = firstLine.split(" ");

        if (parts.length < 2 || !parts[0].equalsIgnoreCase("CONNECT")) {
            // 일단 CONNECT(HTTPS)만 처리한다고 가정
            return;
        }

        String hostPort = parts[1]; // google.com:443
        String[] hostPortParts = hostPort.split(":");
        String host = hostPortParts[0];
        int port = hostPortParts.length > 1 ? Integer.parseInt(hostPortParts[1]) : 80;
        System.out.println("타겟 서버 연결 시도: " + host + ":" + port);

        // 타겟 서버(구글)용 채널 오픈
        SocketChannel targetChannel = SocketChannel.open();
        targetChannel.configureBlocking(false); // 구글 채널도 비차단!

        // 비동기 연결 시작 (연결이 바로 안 끝날 수 있음)
        targetChannel.connect(new InetSocketAddress(host, port));

        // 바구니에 구글 채널 정보 저장(서로 알게 함)
        context.targetChannel = targetChannel;

        // 구글 채널도 같은 관리소(Selector)에 등록
        // 연결이 완료되면(OP_CONNECT) 알려달라고 함
        targetChannel.register(key.selector(), SelectionKey.OP_CONNECT, context);
    }

    private void handleConnect(SelectionKey key) throws IOException {
        SocketChannel targetChannel = (SocketChannel) key.channel();
        SessionContext context = (SessionContext) key.attachment();

        // 비동기 연결 마무리
        if (targetChannel.finishConnect()) {
            System.out.println("타켓 서버 연결 성공!");

            // 브라우저에게 연결 성공했으니 200 응답 쏴주기
            String response = "HTTP/1.1 200 Connection Established\r\n\r\n";
            context.clientChannel.write(ByteBuffer.wrap(response.getBytes()));

            // 구글 서버에서도 데이터가 오면(Read) 알려달라고 상태 변경
            key.interestOps(SelectionKey.OP_READ);

            // 세션 상태를 '터널링'으로 변경
            context.state = SessionContext.State.TUNNELING;
        }
    }

    private void relay(SelectionKey key) throws IOException {

        System.out.println("reply 도달!");

        // 1. 벨이 울린 쪽(Source)을 꺼냄
        SocketChannel source = (SocketChannel) key.channel();
        SessionContext context = (SessionContext) key.attachment();

        // 2. 릴레이용 바구니 준비 (크게 잡을수록 대용량 처리에 유리함)
        ByteBuffer buffer = ByteBuffer.allocate(32768); // 32KB

        // 보내는 쪽에서 데이터를 읽음
        int read = source.read(buffer);

        if (read == -1) {
            // 한쪽이 연결을 끊으면 세션을 종료함
            closeSession(context);
            return;
        }

        if (read > 0) {
            buffer.flip(); // 읽기 모드 전환

            String direction = (source == context.clientChannel) ? ">>> [클라이언트 -> 구글]" : "<<< [구글 -> 클라이언트]";
            System.out.println(direction + " " + read + " bytes 전달 중...");

            // 바구니(Context)을 열어서 반대편 통로(Destination)를 찾음
            // 내가 브라우저면 구글을, 내가 구글이면 브라우저를 선택!
            SocketChannel destination = (source == context.clientChannel)
                    ? context.targetChannel : context.clientChannel;

            // 반대편으로 데이터 전달 (다 써질 때까지 반복)
            while (buffer.hasRemaining()) {
                destination.write(buffer);
            }
        }


    }

    private void closeSession(SessionContext context) {
        try {
            if (context.clientChannel != null) context.clientChannel.close();
            if (context.targetChannel != null) context.targetChannel.close();
            System.out.println("세션 종료: 브라우저와 타겟 서버 연결을 모두 닫았습니다.");
        } catch (IOException e) {
            //닫기 에러
        }
    }

}
