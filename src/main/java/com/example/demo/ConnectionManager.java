package com.example.demo;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.ByteArrayInputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smackx.chatstates.ChatStateManager;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.demo.pojo.ChatState;
import com.example.demo.pojo.IncomingMessage;
import com.example.demo.pojo.LoginResponse;
import com.example.demo.pojo.OutgoingMessage;
import com.example.demo.pojo.RosterChange;
import com.example.demo.pojo.User;

/**
 * keeps track of all of the XMPP connections, the jids associated with them,
 * and the sessions associated with the jids
 */
public class ConnectionManager implements DisposableBean {
	private Map<String, AbstractXMPPConnection> jidToConnectionMap = new HashMap<String, AbstractXMPPConnection>();
	private Map<String, String> sessionIdToJidMap = new HashMap<String, String>();
	private Map<String, Collection<String>> jidToSessionIdsMap = new HashMap<String, Collection<String>>();
	private Map<String, Collection<OutgoingFileTransfer>> sessionIdToOutgoingFileTransfers = new HashMap<String, Collection<OutgoingFileTransfer>>();
	private SimpMessagingTemplate template;
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);

	public ConnectionManager(SimpMessagingTemplate template) {
		this.template = template;
	}

	public void addConnection(String sessionId, String jid, AbstractXMPPConnection connection) throws Exception {
		sessionIdToJidMap.put(sessionId, jid);
		Collection<String> sessionIds = jidToSessionIdsMap.get(jid);
		if (isEmpty(sessionIds)) {
			sessionIds = new HashSet<String>();
			jidToSessionIdsMap.put(jid, sessionIds);
		}
		sessionIds.add(sessionId);

		if (jidToConnectionMap.containsKey(jid)) {
			LOGGER.info("already in connection manager");
			return;
		}

		jidToConnectionMap.put(jid, connection);

		LOGGER.info("Connecting to server for domain: {}", connection.getConfiguration().getXMPPServiceDomain());
		connection.connect();

		LOGGER.info("Connection established.  Connecting as {}", jid);
		connection.login();

		// reconnect
		ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(connection);
		reconnectionManager.enableAutomaticReconnection();

		// roster
		Roster roster = Roster.getInstanceFor(connection);
		roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
		roster.addRosterListener(new RosterListenerImpl(this, jid));

		// chat
		ChatManager chatManager = ChatManager.getInstanceFor(connection);
		chatManager.addIncomingListener(new IncomingChatMessageListenerImpl(this));

		// chat state
		ChatStateManager chatStateManager = ChatStateManager.getInstance(connection);
		chatStateManager.addChatStateListener(new ChatStateListenerImpl(this));
	}

	/**
	 * notifies the WebSocket client(s) of a new incoming message
	 */
	public void notifyIncomingMessage(Message message) {
		IncomingMessage incomingMessage = new IncomingMessage();
		incomingMessage.setFrom(message.getFrom().asBareJid().toString());
		incomingMessage.setMessage(message.getBody());
		// if the message was delayed, e.g., an offline message, then the delay
		// information will contain a timestamp of the original send
		DelayInformation delayInformation = (DelayInformation) message.getExtension(DelayInformation.ELEMENT,
				DelayInformation.NAMESPACE);
		incomingMessage.setTimestamp(delayInformation == null ? new Date() : delayInformation.getStamp());
		sendToJid(message.getTo().asBareJid().toString(), "/queue/incomingMessage", incomingMessage);
	}

	/**
	 * sends an outgoing message over the XMPP connection
	 */
	public void sendXmppMessage(String sessionId, OutgoingMessage message)
			throws XmppStringprepException, NotConnectedException, InterruptedException {
		String jid = sessionIdToJidMap.get(sessionId);
		AbstractXMPPConnection connection = jidToConnectionMap.get(jid);
		ChatManager chatManager = ChatManager.getInstanceFor(connection);
		EntityBareJid entityBareJid = JidCreate.entityBareFrom(message.getTo());
		chatManager.chatWith(entityBareJid).send(message.getMessage());
	}

	/**
	 * attempt to gracefully disconnect all known XMPP connections--otherwise
	 * our users might still appear as available/online to other users
	 */
	@Override
	public void destroy() throws Exception {
		jidToConnectionMap.forEach((jid, connection) -> {
			LOGGER.info("disconnecting {}", jid);
			connection.disconnect();
		});
	}

	public void removeSession(String sessionId) {
		String jid = sessionIdToJidMap.get(sessionId);
		if (jid != null) {
			sessionIdToJidMap.remove(sessionId);
			Collection<String> sessionIds = jidToSessionIdsMap.get(jid);
			if (!isEmpty(sessionIds)) {
				LOGGER.info("removing WebSocket session ID {}, which was being used by jid {}", sessionId, jid);
				sessionIds.remove(sessionId);
			}

			if (isEmpty(sessionIds)) {
				AbstractXMPPConnection connection = jidToConnectionMap.get(jid);
				if (connection != null) {
					jidToConnectionMap.remove(jid);

					if (connection.isConnected()) {
						// TODO should we reuse this connection?
						LOGGER.info("disconnecting XMPP connection for jid {}, used by WebSocket session Id {}", jid,
								sessionId);
						connection.disconnect();
					}
				}
			}
		}
	}

	protected User rosterEntryToUser(Roster roster, RosterEntry entry) {
		BareJid bareJid = entry.getJid().asBareJid();
		if (entry.canSeeHisPresence()) {
			// System.out.println(entry.getName());
			Presence presence = roster.getPresence(bareJid);
			User user = new User();
			user.setJid(entry.getJid().toString());
			user.setName(entry.getName());
			user.setPresence(getPresenceDisplay(presence));
			return user;
		} else {
			LOGGER.info("can't see presence for {}", bareJid.toString());
			return null;
		}
	}

	public void notifyRosterEntriesAdded(String jid, Collection<Jid> addresses) {
		AbstractXMPPConnection connection = jidToConnectionMap.get(jid);
		Roster roster = Roster.getInstanceFor(connection);

		// using a TreeMap to sort by jid
		Set<User> users = new TreeSet<User>();
		for (Jid address : addresses) {
			BareJid bareJid = address.asBareJid();
			RosterEntry entry = roster.getEntry(bareJid);
			User user = rosterEntryToUser(roster, entry);
			if (user != null) {
				users.add(user);
			}
		}
		if (!users.isEmpty()) {
			RosterChange rosterChange = new RosterChange();
			rosterChange.setEntriesAdded(users);
			sendToJid(jid, "/queue/roster", rosterChange);
		}
	}

	public void notifyPresenceChange(String jid, Presence presence) {
		Map<String, String> jidToPresence = Collections.singletonMap(presence.getFrom().asBareJid().toString(),
				getPresenceDisplay(presence));
		RosterChange rosterChange = new RosterChange();
		rosterChange.setPresenceChanged(jidToPresence);
		sendToJid(jid, "/queue/roster", rosterChange);
	}

	/**
	 * sends the WebSocket payload to all sessions associated with the given jid
	 */
	private void sendToJid(String jid, String destination, Object payload) {
		jidToSessionIdsMap.get(jid).stream().forEach(sessionId -> {
			sendToSession(sessionId, destination, payload);
		});
	}

	/**
	 * sends the WebSocket payload to the given session
	 */
	private void sendToSession(String sessionId, String destination, Object payload) {
		// message headers with the session ID must be used to send to a
		// specific session
		// see: https://stackoverflow.com/a/54936221
		// see: https://stackoverflow.com/q/42327780
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		headerAccessor.setSessionId(sessionId);
		headerAccessor.setLeaveMutable(true);
		MessageHeaders messageHeaders = headerAccessor.getMessageHeaders();
		template.convertAndSendToUser(sessionId, destination, payload, messageHeaders);
	}

	public String getPresenceDisplay(Presence presence) {
		if (presence == null)
			return "Unknown";

		if (presence.getStatus() != null)
			return presence.getStatus();

		switch (presence.getType()) {
		case available: {
			switch (presence.getMode()) {
			case available:
			case chat:
				return "Available";
			case away:
				return "Away";
			case xa:
				return "Away for extended time";
			case dnd:
				return "Do no disturb";

			}

			return "Available";
		}
		default:
			return "Unavailable";
		}
	}

	public void sendLoginResponse(String sessionId, LoginResponse loginResponse) {
		sendToSession(sessionId, "/queue/login", loginResponse);
	}

	public void sendRoster(String sessionId) throws NotLoggedInException, NotConnectedException, InterruptedException {
		String jid = sessionIdToJidMap.get(sessionId);
		if (jid != null) {
			AbstractXMPPConnection connection = jidToConnectionMap.get(jid);
			if (connection != null && connection.isAuthenticated()) {
				Roster roster = Roster.getInstanceFor(connection);
				if (!roster.isLoaded()) {
					roster.reloadAndWait();
				}

				Set<User> users = roster.getEntries().stream().map(entry -> {
					return rosterEntryToUser(roster, entry);
				}).filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));

				if (!users.isEmpty()) {
					RosterChange rosterChange = new RosterChange();
					rosterChange.setEntriesAdded(users);
					sendToSession(sessionId, "/queue/roster", rosterChange);
				}
			}
		}
	}

	public void notifyChatState(String fromJid, String toJid, org.jivesoftware.smackx.chatstates.ChatState state) {
		ChatState chatState = new ChatState();
		chatState.setState(state);
		chatState.setJid(fromJid);

		sendToJid(toJid, "/queue/chatState", chatState);
	}

	public void sendChatState(String sessionId, String toJid, org.jivesoftware.smackx.chatstates.ChatState state)
			throws XmppStringprepException, NotConnectedException, InterruptedException {
		String jid = sessionIdToJidMap.get(sessionId);
		if (jid != null) {
			AbstractXMPPConnection connection = jidToConnectionMap.get(jid);

			ChatStateManager chatStateManager = ChatStateManager.getInstance(connection);

			ChatManager chatManager = ChatManager.getInstanceFor(connection);
			EntityBareJid entityBareJid = JidCreate.entityBareFrom(toJid);
			Chat chat = chatManager.chatWith(entityBareJid);

			chatStateManager.setCurrentState(state, chat);
		}
	}

	public void sendFile(String sessionId, byte[] payload, String filename, String contentType, String streamId) {
		Collection<OutgoingFileTransfer> outgoingFileTransfers = sessionIdToOutgoingFileTransfers.get(sessionId);
		if (outgoingFileTransfers != null) {
			Predicate<? super OutgoingFileTransfer> streamIdPredicate = transfer -> {
				return transfer.getStreamID().equals(streamId);
			};
			outgoingFileTransfers.stream().filter(streamIdPredicate).findFirst().ifPresent(transfer -> {
				// Send the file
				transfer.sendStream(new ByteArrayInputStream(payload), filename, payload.length, "");

				Status currentStatus = null;
				while (!transfer.isDone()) {
					if (transfer.getStatus() != currentStatus) {
						currentStatus = transfer.getStatus();
						LOGGER.info("Transfer status: {}", transfer.getStatus());
						sendToSession(sessionId, "/queue/outgoingFileTransfer", transfer);
					}
					if (Status.in_progress == transfer.getStatus()) {
						if (LOGGER.isDebugEnabled()) {
							final DecimalFormat df = new DecimalFormat("#.#");
							String percent = df.format((transfer.getProgress() * 100.0d));
							LOGGER.debug("Transfer progress: {}%", percent);
						}
						sendToSession(sessionId, "/queue/outgoingFileTransfer", transfer);
					}

					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				LOGGER.info("Final transfer status: " + transfer.getStatus());
				if (Status.error == transfer.getStatus()) {
					LOGGER.error("Transfer errored out with exception", transfer.getException());
				}
				sendToSession(sessionId, "/queue/outgoingFileTransfer", transfer);
			});
			outgoingFileTransfers.removeIf(streamIdPredicate);
		}
	}

	public void initiateOutgoingFile(String sessionId, String recipientJid) throws XmppStringprepException {
		String jid = sessionIdToJidMap.get(sessionId);
		if (jid != null) {
			AbstractXMPPConnection connection = jidToConnectionMap.get(jid);

			// get the full jid from the bare jid
			// TODO we probably shouldn't need to do this if we were managing
			// the resources properly
			Roster roster = Roster.getInstanceFor(connection);
			final Presence presense = roster.getPresence(JidCreate.entityBareFrom(recipientJid));
			final Jid presenceJid = presense.getFrom();
			if (presenceJid == null || !(presenceJid instanceof EntityFullJid)) {
				LOGGER.error("No resource found for contact.  Contact may not be online");
				// TODO how do we let the client know that it failed here?
				return;
			}
			final EntityFullJid fullJid = (EntityFullJid) presenceJid;

			// Create the file transfer manager
			FileTransferManager manager = FileTransferManager.getInstanceFor(connection);
			// Create the outgoing file transfer
			OutgoingFileTransfer transfer = manager.createOutgoingFileTransfer(fullJid);
			Collection<OutgoingFileTransfer> outgoingFileTransfers = sessionIdToOutgoingFileTransfers.get(sessionId);
			if (outgoingFileTransfers == null) {
				outgoingFileTransfers = new ArrayList<OutgoingFileTransfer>();
				sessionIdToOutgoingFileTransfers.put(sessionId, outgoingFileTransfers);
			}
			outgoingFileTransfers.add(transfer);

			sendToSession(sessionId, "/queue/outgoingFileTransfer", transfer);
		}
	}
}
