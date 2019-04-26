package com.example.demo;

import org.apache.commons.validator.routines.EmailValidator;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.example.demo.pojo.LoginRequest;
import com.example.demo.pojo.LoginResponse;
import com.example.demo.pojo.OutgoingMessage;

@Controller
public class XmppController {
	@Autowired
	private ConnectionManager connectionManager;
	private static final Logger LOGGER = LoggerFactory.getLogger(XmppController.class);

	@MessageMapping("/outgoingMessage")
	public void sendOutgoingMessage(OutgoingMessage message, @Header("simpSessionId") String sessionId)
			throws Exception {
		connectionManager.sendXmppMessage(sessionId, message);
	}

	@MessageMapping("/login")
	public void login(LoginRequest loginRequest, @Header("simpSessionId") String sessionId) throws NotLoggedInException, NotConnectedException, InterruptedException {
		LoginResponse loginResponse = new LoginResponse();
		loginResponse.setSuccess(false); // guilty until proven innocent

		try {
			// validation--these scenarios should be caught by the browser, but
			// just to be safe...
			if (loginRequest.getJid() == null) {
				loginResponse.setErrorMessage("no jid");
				return;
			}
			String jid = loginRequest.getJid().trim().toLowerCase();
			if (!EmailValidator.getInstance().isValid(jid)) {
				loginResponse.setErrorMessage("invalid jid");
				return;
			}

			String user = jid.substring(0, jid.indexOf("@"));
			String domain = jid.substring(jid.indexOf("@") + 1);

			final XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
					.setUsernameAndPassword(user, loginRequest.getPassword()).setXmppDomain(domain)
					.setCompressionEnabled(true)
					// .setSecurityMode(SecurityMode.required)
					.build();

			final AbstractXMPPConnection connection = new XMPPTCPConnection(config);

			connectionManager.addConnection(sessionId, jid, connection);
			loginResponse.setSuccess(true);
			// connectionManager.sendRoster(sessionId);
		} catch (Exception e) {
			LOGGER.error("error setting up XMPP connection", e);
			connectionManager.removeSession(sessionId);
			loginResponse.setErrorMessage(e.getMessage());
		} finally {
			connectionManager.sendLoginResponse(sessionId, loginResponse);
		}

		if (loginResponse.isSuccess()) {
			connectionManager.sendRoster(sessionId);
		}
	}

	@MessageMapping("/logout")
	public void logout(@Header("simpSessionId") String sessionId) {
		connectionManager.removeSession(sessionId);
	}
}