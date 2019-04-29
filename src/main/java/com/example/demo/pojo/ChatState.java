package com.example.demo.pojo;

/**
 * used to represent by incoming and outgoing chat state changes 
 */
public class ChatState {
	private String jid;
	private org.jivesoftware.smackx.chatstates.ChatState state;

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

	public org.jivesoftware.smackx.chatstates.ChatState getState() {
		return state;
	}

	public void setState(org.jivesoftware.smackx.chatstates.ChatState state) {
		this.state = state;
	}
}
