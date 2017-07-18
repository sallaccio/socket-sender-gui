
var topBar;
var mainSection;
var bottomBar;

var websocketUrl = "ws://localhost:5150/websocketConnection";

$(document).ready(function() {

    console.log("Executing main.js");

    $.getJSON("package.json", function(json) {
		console.log("Load package.json");
        document.title = json.name;
    });

    $("#topBar").append("<div id='topBarTitle'>TopBar Title</div>");
    $("#mainSection").append("<div id='mainSectionText'>Main section</div>");
    $("#bottomBar").append("<div id='mainSectionText'>Bottom Bar</div>");

    $("#topBar").show();
    $("#mainSection").show();
    $("#bottomBar").show();

    msgManager = new MessageManager();

    bottomBar = new BottomBar();

})

MessageManager = function MessageManager() {
	var webSocket = null;
	var retryCount = 0;
	var pendingMessages = [];
	var self = this;

    function connectToWebSocket() {
		console.log("WebsocketUrl = " + websocketUrl);
		// the websocket try to connect to target url

		webSocket = new WebSocket(websocketUrl);

        webSocket.onopen = function onopen() {
			console.log(' ===== WEBSOCKET OPENED =====');

			// send established to client
			setTimeout(function() {
				webSocket.send(JSON.stringify({
					Type : 'ConnectionEstablished',
					Number : 0
				}));
			}, 2000);

			// clear the retry count
			retryCount = 0;
			if (pendingMessages.length > 0) {
				for (var i = 0; i < pendingMessages.length; i++) {
					var message = pendingMessages[i];
					webSocket.send(message);
					console.log('Sent pending message: ' + message);
				}
				pendingMessages = [];
			}
		};

        webSocket.onerror = function onerror(event) {
			try {
				websocket.close();
				console.log(' ===== WEBSOCKET ERROR =====');
			} catch (ex) {
				console.log('WebSocket exception =' + ex);
			}
		};

        webSocket.onclose = function onclose(event) {
			try {
				console.log('===== WEBSOCKET CLOSE - retried ' + retryCount + ' time(s) =====');
				websocket.close();
			} catch (ex) {
				console.log('WebSocket exception =' + ex);
			}

			// retry the connection
			if (retryCount == 0) {
				connectToWebSocket();
				retryCount++;
			} else {
				setTimeout(function() {
					connectToWebSocket();
				}, parseInt(reconnectInterval));
			}
		};

        webSocket.onmessage = function onmessage(event) {

            if (!event.data) {
				console.log("Error: Event object has no data.");
				return;
			}

			console.log("<= Received server message: " + event.data);

            var json;
            try {
				json = JSON.parse(event.data);
				event.data = "";
			}
            catch (ex) {
				console.log("Error: The message is an invalid JSON message.");
				return;
			}

            if (!json.Type) {
                console.log("Error: The message is of invalid type. Message ignored.");
				return;
            }

            switch (json.Type) {
                case 'form':
                    console.log("A form! Ask me anything!");
                    bottomBar.forForm();
                    break;
                case 'display':
                    console.log("I can only tell you what you wanna hear.");
                    bottomBar.forDisplay();
                    break;
                case 'popup':
                    console.log("Attention please. I should be a popup.");
                    break;
                default:
                    console.log("You don't exist.");
            }

        };
    }

    this.sendMessage = function sendMessage(messageObject) {
		var message = JSON.stringify(messageObject);
        if (webSocket.readyState == WebSocket.CONNECTING) {
			pendingMessages.push(message);
			console.log('Pushed message in queue: ' + message);
		} else {
			webSocket.send(message);
			console.log('=> Sent message: ' + message);
		}
	};

    this.closeWs = function closeWs() {
		console.log("************* FORCE CLOSE WEBSOCKET ***********************");
		webSocket.close();
	};

    connectToWebSocket();

}

BottomBar = function BottomBar() {

    this.forDisplay = function forDisplay() {
        var menuBottomBar = "";
		menuBottomBar += "<div id='bottomBarButtons'><button id='bb_Ok'>OK</button></div>";
        $("#bottomBar").html(menuBottomBar).stop(true, true).fadeIn();
		menuBottomBar = "";
    }

    this.forForm = function forForm() {
        var menuBottomBar = "";
		menuBottomBar += "<div id='bottomBarButtons'><button id='bb_Ok'>OK</button><button id='bb_Cancel'>Cancel</button></div>";
        $("#bottomBar").html(menuBottomBar).stop(true, true).fadeIn();
		menuBottomBar = "";
    }

    this.clearBar = function clearBar() {
        var menuBottomBar = "";
        $("#bottomBar").html(menuBottomBar).stop(true, true).fadeIn();
    }

}
