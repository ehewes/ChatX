package com.chatx.core.handlers;

import com.chatx.core.entity.Message;
import com.chatx.core.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MessageHandler extends TextWebSocketHandler {

    @Autowired
    private MessageRepository messageRepository;

    private final List<WebSocketSession> webSocketSessions = Collections.synchronizedList(new ArrayList<>());
    private final Map<WebSocketSession, String> userNames = Collections.synchronizedMap(new HashMap<>());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        System.out.println(session.getId() + " Connected");
        webSocketSessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        System.out.println(session.getId() + " Disconnected");
        webSocketSessions.remove(session);
        userNames.remove(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        super.handleMessage(session, message);

        String payload = message.getPayload().toString();
        ModelMessage modelMessage = objectMapper.readValue(payload, ModelMessage.class);

        if (modelMessage.getName() != null) {
            userNames.put(session, modelMessage.getName());
        }

        String userName = userNames.get(session);
        if (userName != null) {
            modelMessage.setName(userName);
        }

        String updatedMessage = objectMapper.writeValueAsString(modelMessage);

        // Save message to database
        String route = session.getUri().getPath();
        Message dbMessage = new Message();
        dbMessage.setRoute(route);
        dbMessage.setUsername(userName);
        dbMessage.setContent(modelMessage.getMessage());
        messageRepository.save(dbMessage);

        for (WebSocketSession webSocketSession : webSocketSessions) {
            if (session == webSocketSession) continue;
            webSocketSession.sendMessage(new TextMessage(updatedMessage));
        }
    }
}