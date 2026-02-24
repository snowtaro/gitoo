package com.example.gitoo.system.config;

import com.example.gitoo.game.dto.WordChainMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import com.example.gitoo.room.RoomService;
import com.example.gitoo.user.model.User;
import com.example.gitoo.user.repository.UserRepository;
import org.springframework.context.annotation.Lazy;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    @Lazy
    private RoomService roomService;

    @Autowired
    private UserRepository userRepository;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        logger.info("새로운 웹소켓 연결이 수립되었습니다.");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
            logger.info("사용자 연결 해제: " + username);

            WordChainMessage message = new WordChainMessage();
            message.setType(WordChainMessage.MessageType.LEAVE);
            message.setUsername(username);
            message.setMessage(username + "님이 게임을 떠났습니다.");

            messagingTemplate.convertAndSend("/topic/game", message);

            try {
                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    String sessionId = headerAccessor.getSessionId();
                    String key = "ws_rooms_" + sessionId;
                    @SuppressWarnings("unchecked")
                    java.util.Set<String> subscribedRooms = (java.util.Set<String>) headerAccessor
                            .getSessionAttributes().get(key);
                    if (subscribedRooms != null && !subscribedRooms.isEmpty()) {
                        for (String roomId : subscribedRooms) {
                            try {
                                roomService.leave(roomId, username);
                                logger.info("Removed ghost user from room based on subscription: " + roomId);
                            } catch (Exception ex) {
                                logger.error("Failed to leave room inside disconnect listener", ex);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error checking or leaving rooms on disconnect", e);
            }
        }
    }
}