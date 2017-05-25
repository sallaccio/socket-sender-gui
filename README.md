# socket-sender-gui
Simulate sending messages or sequences of messages from a websocket server, with a GUI to manage them.

If you're working on an application that communicates with a browser through websockets 
(can be adapted to HTTP requests as well) 
and wish to work on given actions without having to start long sequences of actions again and again,
you might want to simulate the sending of a given message.
Or of a given sequence of messages.

This tool is basically a GUI around a minimal websocket server that automatically send whatever message you selected on a connection event.

It was originally created to deal with Json messages, but can be easily adapted to treat messages in any language (or none in particular)
by just setting the text-editor to the desired language.  
