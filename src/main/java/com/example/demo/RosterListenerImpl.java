package com.example.demo;

import java.util.Collection;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.RosterListener;
import org.jxmpp.jid.Jid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RosterListenerImpl implements RosterListener {
	private ConnectionManager connectionManager;
	private String sessionId;
	private static final Logger LOGGER = LoggerFactory.getLogger(RosterListenerImpl.class);

	public RosterListenerImpl(ConnectionManager connectionManager, String sessionId) {
		this.connectionManager = connectionManager;
		this.sessionId = sessionId;
	}

	@Override
	public void entriesAdded(Collection<Jid> addresses) {
		LOGGER.info("entries added: {}", addresses);
		connectionManager.notifyRosterEntriesAdded(sessionId, addresses);
	}

	@Override
	public void entriesUpdated(Collection<Jid> addresses) {
		LOGGER.info("entries updated: {}", addresses);
		// TODO
	}

	@Override
	public void entriesDeleted(Collection<Jid> addresses) {
		LOGGER.info("entries deleted: {}", addresses);
		// TODO
	}

	@Override
	public void presenceChanged(Presence presence) {
		LOGGER.info("presence changed: {}", presence);
		connectionManager.notifyPresenceChange(sessionId, presence);
	}
}