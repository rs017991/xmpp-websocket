package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {
	private ConnectionManager connectionManager;
	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEventListener.class);

	public WebSocketEventListener(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@EventListener
	private void handleSessionConnected(SessionConnectEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		String sessionId = accessor.getSessionId();
		LOGGER.info("session connected {}", sessionId);
	}

	@EventListener
	private void handleSessionDisconnected(SessionDisconnectEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		String sessionId = accessor.getSessionId();
		LOGGER.info("session disconnected {}", sessionId);

		connectionManager.removeSession(sessionId);
	}
}