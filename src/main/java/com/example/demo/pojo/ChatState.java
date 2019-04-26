package com.example.demo.pojo;

public class ChatState {
	private String from;
	private org.jivesoftware.smackx.chatstates.ChatState chatState;

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public org.jivesoftware.smackx.chatstates.ChatState getChatState() {
		return chatState;
	}

	public void setChatState(org.jivesoftware.smackx.chatstates.ChatState chatState) {
		this.chatState = chatState;
	}
}
