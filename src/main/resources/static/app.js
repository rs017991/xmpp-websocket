var stompClient = null;
var myJid = null; // only set after successful login

function setConnected(connected) {
}

function setLoggedIn(loggedOn) {
	if (loggedOn) {
		myJid = document.getElementById("inputEmail").value.trim().toLowerCase();

		document.getElementById("sidebar").classList.remove("invisible");
		document.getElementById("form-signin").classList.add("d-none");
		document.getElementById("form-logout").classList.remove("d-none");
		document.getElementById("loggedInJid").innerText = myJid;
	} else {
		myJid = null;

		document.getElementById("sidebar").classList.add("invisible");
		document.getElementById("form-signin").classList.remove("d-none");
		document.getElementById("form-logout").classList.add("d-none");
		document.getElementById("loggedInJid").innerText = "";

		// clear roster
		var roster = document.getElementById("roster");
		while (roster.firstChild) {
			roster.removeChild(roster.firstChild);
		}
		// clear chat windows
		var chatWindows = document.getElementById("chatWindows");
		while (chatWindows.firstChild) {
			chatWindows.removeChild(chatWindows.firstChild);
		}
	}
}

function connect() {
	var socket = new SockJS('/websocket');
	stompClient = Stomp.over(socket);
	//stompClient.debug = () => {}; // comment out to re-enable debug messages
	stompClient.connect({}, function(frame) {
		setConnected(true);
		console.log('Connected: ' + frame);
		stompClient.subscribe('/user/queue/login', (loginResponse) => {
			let parsed = JSON.parse(loginResponse.body);
			let success = parsed.success;
			let errorMessage = parsed.errorMessage;
			let loginError = document.getElementById("loginError");

			document.getElementById("spinner").classList.add("d-none");

			setLoggedIn(success);
			if (success) {
				loginError.classList.add("invisible");
				loginError.innerText = ".";
			} else if (errorMessage) {
				loginError.innerText = errorMessage;
				loginError.classList.remove("invisible");
				document.getElementById("form-signin").classList.remove("d-none");
			}
		});
		stompClient.subscribe('/user/queue/incomingMessage', (incomingMessage) => {
			let parsed = JSON.parse(incomingMessage.body);
			let from = parsed.from;
			addMessageToChatContent(new Date(parsed.timestamp), false, from, parsed.message, showChatWindow(from));
		});
		stompClient.subscribe('/user/queue/roster', function(roster) {
			var rosterChange = JSON.parse(roster.body);

			// entriesAdded
			var entriesAdded = rosterChange.entriesAdded;
			if (entriesAdded) {
				for ( let user of entriesAdded) {
					var existingLi = findRosterEntryByJid(user.jid); // avoid duplicates if it already exists
					if (!existingLi) {
						var li = document.createElement("li");
						li.className = "entry";
						li.setAttribute('data-jid', user.jid);
						li.setAttribute('data-name', user.name);
						li.setAttribute('title', user.name === user.jid ? user.jid : (user.name + " (" + user.jid + ")"));
						var divName = document.createElement("div");
						divName.className = "jid";
						divName.innerText = user.name;
						var divPresence = document.createElement("div");
						divPresence.className = "presence";
						divPresence.innerText = user.presence;
						li.appendChild(divName);
						li.appendChild(divPresence);
						var roster = document.getElementById("roster");
						roster.appendChild(li);
					}
				}
			}

			// presenceChanged
			var presenceChanged = rosterChange.presenceChanged;
			for ( var jid in presenceChanged) {
				var jidEntry = Array.from(document.querySelectorAll("#roster .entry")).find(el => el.dataset.jid === jid);
				if (jidEntry) {
					jidEntry.querySelector(".presence").innerText = presenceChanged[jid];
				}
			}
		});
	});
}

// function disconnect() {
// console.log("disconnect()");
// if (stompClient !== null) {
// stompClient.disconnect();
// }
// setConnected(false);
// console.log("Disconnected");
// }

function sendMessage(event) {
	var chatWindow = event.target.closest(".chatWindow");
	var jid = chatWindow.dataset.jid;
	var chatInput = event.target.chatInput;
	var inputValue = chatInput.value;
	// console.log("sendMessage: " + jid + ", " + inputValue);

	// send to server
	stompClient.send("/app/outgoingMessage", {}, JSON.stringify({
		'message' : inputValue,
		'to' : jid
	}));

	// add message to chat content
	addMessageToChatContent(new Date(), true, myJid, inputValue, chatWindow);
	
	// clear input
	chatInput.value = "";
}

function addMessageToChatContent(timestamp, mine, fromJid, messageBody, chatWindow) {
	var message = document.createElement("div");
	message.classList.add("message", mine ? "mine" : "theirs");
	var timestampSpan = document.createElement("span");
	timestampSpan.className = "timestamp";
	timestampSpan.innerText = timestamp.toLocaleTimeString();
	var from = document.createElement("span");
	from.className = "from";
	from.innerText = jidToName(fromJid);
	var body = document.createElement("span");
	body.className = "body";
	body.innerText = messageBody;
	message.appendChild(timestampSpan);
	message.appendChild(from);
	message.appendChild(body);
	var chatContent = chatWindow.querySelector(".chatContent");
	chatContent.appendChild(message);

	// scroll to bottom
	chatContent.scrollTop = chatContent.scrollHeight;
}

function findRosterEntryByJid(jid) {
	return Array.from(document.querySelectorAll("#roster .entry")).find(el => el.dataset.jid === jid);
}

function jidToName(jid) {
	let jidEntry = findRosterEntryByJid(jid);
	return jidEntry ? jidEntry.dataset.name : jid;
}

function jidToFullDisplay(jid) {
	let name = jidToName(jid);
	return (name === jid) ? jid : (name + " (" + jid + ")");
}

function showChatWindow(jid) {
	var chatWindows = document.getElementById("chatWindows");

	// check if a chat with this jid is already open
	var jidChatWindow = Array.from(chatWindows.getElementsByClassName("chatWindow")).find(el => el.dataset.jid === jid);
	if (!jidChatWindow) {
		var chatWindow = document.createElement("div");
		chatWindow.className = "chatWindow";
		chatWindow.setAttribute("data-jid", jid);

		var chatTitle = document.createElement("div");
		chatTitle.className = "chatTitle";

		var name = document.createElement("span");
		name.className = "name";
		name.innerText = jidToName(jid);
		name.setAttribute("title", jidToFullDisplay(jid));

		var close = document.createElement("span");
		close.className = "close";

		var chatContent = document.createElement("div");
		chatContent.className = "chatContent";

		// wrap input in form so we can deal with submit event instead of
		// keypress event, etc.
		var form = document.createElement("form");

		var chatInput = document.createElement("input");
		chatInput.className = "chatInput";
		chatInput.setAttribute("name", "chatInput");
		chatInput.setAttribute("autocomplete", "off");

		chatTitle.appendChild(name);
		chatTitle.appendChild(close);
		chatWindow.appendChild(chatTitle);
		chatWindow.appendChild(chatContent);
		form.appendChild(chatInput);
		chatWindow.appendChild(form);
		chatWindows.prepend(chatWindow);

		jidChatWindow = chatWindow;
	}
	return jidChatWindow;
}

(function() {
	// event handlers
	document.getElementById("form-signin").addEventListener('submit', (event) => {
		event.preventDefault();

		document.getElementById("loginError").classList.add("invisible");
		document.getElementById("loginError").innerText = ".";
		document.getElementById("form-signin").classList.add("d-none");
		document.getElementById("spinner").classList.remove("d-none");

		// send to server
		stompClient.send("/app/login", {}, JSON.stringify({
			'jid' : event.target.inputEmail.value,
			'password' : event.target.inputPassword.value
		}));
	});
	document.getElementById("form-logout").addEventListener('submit', (event) => {
		event.preventDefault();
		
		setLoggedIn(false);

		// send to server
		stompClient.send("/app/logout");
	});
	document.getElementById("roster").addEventListener('click', (event) => {
		var parent = event.target.parentNode;
		if (parent.classList.contains('entry')) {
			let chatWindow = showChatWindow(parent.dataset.jid);
			chatWindow.querySelector(".chatInput").focus();
		}
	});
	document.getElementById("chatWindows").addEventListener('click', (event) => {
		if (event.target.classList.contains('close')) {
			event.target.closest(".chatWindow").remove();
		}
	});
	document.getElementById("chatWindows").addEventListener('submit', (event) => {
		event.preventDefault();
		sendMessage(event);
	});

	connect();
})();