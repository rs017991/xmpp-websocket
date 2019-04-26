package com.example.demo.pojo;

import java.util.Date;

public class IncomingMessage {
	private String message;
	private String from;
	// we need to track the timestamp of the message in case of delayed
	// delivery/offline messages
	private Date timestamp;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
}
