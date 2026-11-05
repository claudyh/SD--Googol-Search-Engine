var stompClient = null;


function connect() {
    var socket = new SockJS('/my-websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        // setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/messages', function (message) {
            console.log(message);
            showMessage(JSON.parse(message.body).content);
        });
    });
}

connect();


function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    // setConnected(false);
    console.log("Disconnected");
}

function showMessage(message) {
    $("#messages").innerHTML = "";
    $("#messages").append("<tr><td>" + message + "</td></tr>");
}

function sendMessage() {
    stompClient.send("/app/message", {}, JSON.stringify({'content': $("#message").val()}));
}

