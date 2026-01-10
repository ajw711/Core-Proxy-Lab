package com.proxy.filter;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ProxyFilterChain {

    private List<HttpFilter> filters = new ArrayList<>();
    private int index = 0;

    public void addFilter(HttpFilter filter) {
        filters.add(filter);
    }

    public void handleNext(Socket socket, byte[] data){
        if(index < filters.size()){
            HttpFilter filter = filters.get(index++);
            filter.doFilter(socket, data, this); // 다음 필터 실행
        }
    }
}
