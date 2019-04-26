package com.example.demo;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateListener;
import org.jxmpp.jid.EntityBareJid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatStateListenerImpl implements ChatStateListener {
	private ConnectionManager connectionManager;
	private static final Logger LOGGER = LoggerFactory.getLogger(ChatStateListenerImpl.class);

	public ChatStateListenerImpl(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@Override
	public void stateChanged(Chat chat, ChatState state, Message message) {
		EntityBareJid xmppAddressOfChatPartner = chat.getXmppAddressOfChatPartner();
		LOGGER.info("xmpp address of chat partner: {}", xmppAddressOfChatPartner);
		LOGGER.info("chat state: {}", state);
		LOGGER.info("message: {}", message);

		connectionManager.notifyChatState(chat.getXmppAddressOfChatPartner().asBareJid().toString(),
				message.getTo().asBareJid().toString(), state);
	}
}
