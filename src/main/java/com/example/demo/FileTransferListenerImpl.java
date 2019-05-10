package com.example.demo;

import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;

public class FileTransferListenerImpl implements FileTransferListener {
	private ConnectionManager connectionManager;
	private String sessionId;

	public FileTransferListenerImpl(ConnectionManager connectionManager, String sessionId) {
		this.connectionManager = connectionManager;
		this.sessionId = sessionId;
	}

	@Override
	public void fileTransferRequest(FileTransferRequest request) {
		connectionManager.handleFileTransferRequest(sessionId, request);
	}
}
