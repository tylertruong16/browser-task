package com.task.service;

import com.task.common.JsonConverter;
import com.task.model.BrowserTask;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

@Service
@Log
public class WebSocketService {

    @Value("${system.socket-url}")
    private String socketUrl;

    @Value("${system.socket-destination}")
    private String socketDestination;

    @Autowired
    private TaskQueue taskQueue;


    @PostConstruct
    public void socketConnect() throws Exception {
        // Create WebSocket client
        WebSocketClient webSocketClient = new StandardWebSocketClient();

        // Use SockJsClient for WebSocket client
        var sockJsClient = new SockJsClient(
                List.of(new WebSocketTransport(webSocketClient))
        );
        // Create Stomp Client
        var stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new StringMessageConverter());

        // Connect to SockJS endpoint
        var destination = StringUtils.defaultIfBlank(socketDestination, "").trim();
        StompSessionHandler sessionHandler = new MyStompSessionHandler(destination, taskQueue);
        // Connect to the SockJS server
        log.log(Level.INFO, "browser-task >> WebSocketService >> socketConnect >> url: {0} >> destination: {1}", new Object[]{socketUrl, socketDestination});
        stompClient.connectAsync(socketUrl, new WebSocketHttpHeaders(), sessionHandler).get();

        // Keep the application running to listen for messages indefinitely
        synchronized (WebSocketService.class) {
            WebSocketService.class.wait(); // This keeps the thread alive indefinitely
        }
    }

    @Log
    @AllArgsConstructor
    static class MyStompSessionHandler extends StompSessionHandlerAdapter {
        private String socketDestination;
        private TaskQueue taskQueue;


        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            log.log(Level.INFO, "browser-task >> MyStompSessionHandler >> receivedMessage: {0}", payload);
            var json = String.valueOf(payload);
            var wrapper = JsonConverter.convertToObject(json, MessageMapper.class)
                    .orElse(new MessageMapper());
            var message = wrapper.getMessage();
            var browserTasks = Arrays.stream(JsonConverter.convertToObject(message, BrowserTask[].class).orElse(new BrowserTask[]{})).toList();
            log.log(Level.INFO, "browser-task >> MyStompSessionHandler >> browserTasks: {0}", JsonConverter.convertListToJson(browserTasks));
            browserTasks.forEach(it -> taskQueue.pushTask(it));
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            log.log(Level.INFO, "browser-task >> MyStompSessionHandler >> afterConnected >> Connected");
            session.subscribe(socketDestination, this);
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            log.log(Level.SEVERE, "browser-task >> MyStompSessionHandler >> handleException >> Throwable:", exception);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            log.log(Level.SEVERE, "browser-task >> MyStompSessionHandler >> handleTransportError >> Throwable:", exception);
        }
    }

    @Data
    @NoArgsConstructor
    public static class MessageMapper {
        private String roomId;
        private String message = "[]";
    }
}
