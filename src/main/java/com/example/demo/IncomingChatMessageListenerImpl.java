package com.example.demo;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.EntityBareJid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncomingChatMessageListenerImpl implements IncomingChatMessageListener {
	private ConnectionManager connectionManager;
	private static final Logger LOGGER = LoggerFactory.getLogger(IncomingChatMessageListenerImpl.class);

	public IncomingChatMessageListenerImpl(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@Override
	public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
		LOGGER.info("new incoming message: {} {} {}", from, message, chat);
		LOGGER.info("message was: {}", message.getBody());
		connectionManager.notifyIncomingMessage(message);
	}
}
