package com.task.service;

import com.task.common.JsonConverter;
import com.task.model.BrowserTask;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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
    void init() {
        socketConnect();
    }


    @SneakyThrows
    public void socketConnect() {
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
        StompSessionHandler sessionHandler = new MyStompSessionHandler(destination, taskQueue, stompClient, socketUrl);
        // Connect to the SockJS server
        log.log(Level.INFO, "browser-task >> WebSocketService >> socketConnect >> url: {0} >> destination: {1}", new Object[]{socketUrl, socketDestination});
        stompClient.connectAsync(socketUrl, new WebSocketHttpHeaders(), sessionHandler).get();
    }

    @Log
    @AllArgsConstructor
    static class MyStompSessionHandler extends StompSessionHandlerAdapter {
        private String socketDestination;
        private TaskQueue taskQueue;
        private WebSocketStompClient stompClient;
        private String socketUrl;


        @Override
        public void handleFrame(@NonNull StompHeaders headers, Object payload) {
            log.log(Level.INFO, "browser-task >> MyStompSessionHandler >> receivedMessage: {0}", payload);
            var json = String.valueOf(payload);
            var wrapper = JsonConverter.convertToObject(json, MessageMapper.class)
                    .orElse(new MessageMapper());
            var message = wrapper.getMessage();
            var browserTasks = JsonConverter.convertToObject(message, BrowserTask.class).orElse(new BrowserTask());
            log.log(Level.INFO, "browser-task >> MyStompSessionHandler >> browserTasks: {0}", JsonConverter.convertListToJson(browserTasks));
            if (StringUtils.isNoneBlank(browserTasks.getTaskId())) {
                taskQueue.pushTask(browserTasks);
            }
        }

        @Override
        public void afterConnected(StompSession session, @NonNull StompHeaders connectedHeaders) {
            log.log(Level.INFO, "browser-task >> MyStompSessionHandler >> afterConnected >> Connected");
            session.subscribe(socketDestination, this);
        }

        @Override
        public void handleException(@NonNull StompSession session, StompCommand command, @NonNull StompHeaders headers, @NonNull byte[] payload, @NonNull Throwable exception) {
            log.log(Level.SEVERE, "browser-task >> MyStompSessionHandler >> handleException >> Throwable:", exception);
        }

        @Override
        public void handleTransportError(@NonNull StompSession session, @NonNull Throwable exception) {
            log.log(Level.SEVERE, "browser-task >> MyStompSessionHandler >> handleTransportError >> Throwable:", exception);

            // Attempt to reconnect on connection loss
            if (exception instanceof ConnectionLostException) {
                try {
                    log.log(Level.WARNING, "browser-task >> MyStompSessionHandler >> Connection lost, attempting to reconnect...");
                    Thread.sleep(TimeUnit.SECONDS.toMillis(3));  // Wait before reconnecting
                    stompClient.connectAsync(socketUrl, new WebSocketHttpHeaders(), this).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.log(Level.SEVERE, "browser-task >> MyStompSessionHandler >> Reconnection failed:", e);
                }
            }
        }
    }

    @Data
    @NoArgsConstructor
    public static class MessageMapper {
        private String roomId;
        private String message = "[]";
    }
}
