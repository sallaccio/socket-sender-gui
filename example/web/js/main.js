
var topBar;
var mainSection;
var bottomBar;

$(document).ready(function() {

    console.log("Executing main.js");

    $.getJSON("app.json", function(json) {
		console.log("Load app.json");
        document.title = json.name;
    });

    $("#topBar").append("<div id='topBarTitle'>TopBar Title</div>");
    $("#mainSection").append("<div id='mainSectionText'>Main section</div>");
    $("#bottomBar").append("<div id='bottomBarButtons'><button id='bb_Ok'>OK</button><button id='bb_Cancel'>Cancel</button></div>");

    $("#topBar").show();
    $("#mainSection").show();
    $("#bottomBar").show();

})
