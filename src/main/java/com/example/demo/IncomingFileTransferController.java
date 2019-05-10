package com.example.demo;

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IncomingFileTransferController {
	@Autowired
	private ConnectionManager connectionManager;

	@RequestMapping("/downloadIncomingFile")
	public InputStreamResource acceptIncomingFile(@RequestParam String streamId, @RequestParam String sessionId, HttpServletResponse response)
			throws XMPPErrorException, SmackException, InterruptedException {
		IncomingFileTransfer incomingFileTransfer = connectionManager.acceptIncomingFile(streamId);
		if (incomingFileTransfer == null) {
			throw new ResourceNotFoundException();
		}

		response.setContentType(incomingFileTransfer.getFileName());
		response.setHeader("Content-Disposition", "attachment; filename=" + incomingFileTransfer.getFileName());
		response.setHeader("Content-Length", String.valueOf(incomingFileTransfer.getFileSize()));
		InputStream inputStream = incomingFileTransfer.receiveFile();
		connectionManager.setDownloadingStatus(sessionId, streamId);
		return new InputStreamResource(inputStream);
	}

	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	public class ResourceNotFoundException extends RuntimeException {
		private static final long serialVersionUID = -978264684289992994L;
	}
}
