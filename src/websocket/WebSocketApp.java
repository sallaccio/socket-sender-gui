package websocket;

import com.google.gson.*;
import com.sun.grizzly.tcp.Request;
import com.sun.grizzly.websockets.*;

import utility.JsonMinify;
//import utility.PsxUtil;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.io.*;

public class WebSocketApp extends WebSocketApplication {

	private String messagesFolder, sequencesFolder, currentActionFolder;
	private Boolean websocketOpened = false;
	private WebSocket mainSocket = null;
	private WebSocket slaveSocket = null;
	
	public WebSocketApp(String messagesPath, String sequencesPath, String currentActionPath)
	{
		messagesFolder = messagesPath;
		sequencesFolder = sequencesPath;
		currentActionFolder = currentActionPath;
	}
	
	//================================================================================
    // Connection objects
    //================================================================================
	
//	public WebSocket createWebSocket(ProtocolHandler protocolHandler, WebSocketListener... listeners) {
//		
//		System.out.print("Create Socket = ");
//		for (WebSocketListener listener: listeners)	
//			System.out.print(listener + "; ");
//				
//		return new WebSocketApp(protocolHandler, listeners);
//	}

	public boolean isApplicationRequest(Request request) {
		System.out.println("request = " + request + "              uri=" + request.requestURI().toString());
//		String[] servletInfo = request.requestURI().toString().split("/");
		return true;
	}

	@Override
	public void onClose(WebSocket socket, DataFrame frame) {

		System.out.println("Close application on socket=" + socket);
		super.onClose(socket, frame);

	}
	
	@Override
	public void onConnect(WebSocket socket) {
		super.onConnect(socket);
		System.out.println("======> socket.isConnected  " + socket);
		
		sleep(2000);
		executeAction();
	}

	/* One can add some code to react to onMessage events 
	 * and thus transform the websocket-sender into a responder.*/
	@Override
	public void onMessage(WebSocket socket, String text) {
					
		try {
			System.out.println("=====> ON MESSAGE ...." + text + "     socketInfo=" + socket.toString());
			
			// convert JSON into java object
			/* 
			JsonParser jsonParser = new JsonParser();
			JsonElement jsonInElement = jsonParser.parse(text);
			*/
			// Do something depending on the object

		} catch (Exception e) {
			System.out.println("Exception-----------" + e);
		}
	}
	
	public Boolean isWebsocketOpened() {
		return websocketOpened;
	}

	public void sendOnPrimarySocket(String msgOut) throws IOException {
		System.out.println("=====> sendOnPrimarySocket ...." + msgOut);
		try {
			mainSocket.send(msgOut);
		} catch (WebSocketException e) {
			e.printStackTrace();
			mainSocket.close();
		}
	}

	public void sendOnSlaveSocket(String msgOut) throws IOException {
		System.out.println("=====> sendOnSlaveSocket ...." + msgOut);
		if (slaveSocket != null) {
			try {
				slaveSocket.send(msgOut);
			} catch (WebSocketException e) {
				e.printStackTrace();
				slaveSocket.close();
			}
		}
	}
	
	private void executeAction() 
	{
		
		JsonParser parser = new JsonParser();
		JsonElement jsonFile = null;
		JsonObject jsonAction = null;
		try 
		{
			jsonFile = parser.parse(new FileReader(currentActionFolder + "\\current.json"));

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		String type, name = "";

		if (jsonFile != null)
			jsonAction = jsonFile.getAsJsonObject();
		if (jsonAction != null)
		{
			type = jsonAction.get("Type").getAsString();
			
			if (type.equals("Message") || type.equals("Sequence"))
			{
				name = jsonAction.get("Name").getAsString();
			}
			
			if (type.equals("Message"))
			{
				broadcastMessage(name);
			}
			else if (type.equals("CustomMessage"))
			{
				broadcastCustomMessage();
			}
			else if (type.equals("Sequence"))
			{
				Command[] sequence = extractSequence(sequencesFolder + "\\" + name + ".json");
				executeSequence(sequence);
			}
			else if (type.equals("CustomSequence"))
			{
				Command[] sequence = extractSequence(currentActionFolder + "\\custom.json");
				executeSequence(sequence);
			}
		}
	}
	
	private void executeSequence(Command[] sequence)
	{
		for (int i = 0; i < sequence.length; i++)
		{
			Command command = sequence[i];
			if (command.type == CommandType.Message)
				broadcastMessage(command.value);
			else if (command.type == CommandType.Wait)
				sleep(Integer.parseInt(command.value));
		}
	}
	
	//================================================================================
    // Sending methods
    //================================================================================
	
	/*
	 * Send msgOut as is to each WebSocket
	 */
	private void broadcast(String msgOut) {
		System.out.println("=====> broadcasting ...." + msgOut);
		for (WebSocket webSocket : getWebSockets()) {
			try {
				webSocket.send(msgOut);
			} catch (WebSocketException e) {
				//e.printStackTrace();
				//webSocket.close();
			} 
		}
	}
	
	/*
	 * Send message from file (identified by name without extension .json)
	 */
	private void broadcastMessage(String messageName)
	{
		broadcast(message(messagesFolder + "\\"+messageName+".json"));
	}
	
	/* 
	 * Send message from custom message file "custom.json"
	 */
	private void broadcastCustomMessage()
	{
		broadcast(message(currentActionFolder+"\\custom.json"));
	}
	
	//================================================================================
    // Private helpers
    //================================================================================
	
	/*
	 * Pause between messages for rendering time.
	 * Can be used as part of a sequence.
	 */
	private void sleep(int length)
	{
		try
		{
			Thread.sleep(length);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/* 
	 * Reads message from file in minified format
	 */
	private String message(String filepath)
	{
		String content = "";
		try {
			content = new Scanner(new File(filepath)).useDelimiter("\\Z").next();
			content = content.replaceAll(System.getProperty("line.separator"), "");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return JsonMinify.minify(content);
	}
	
	/*
	 * Reads sequence from file and extracts the sequential series of actions.
	 */
	private Command[] extractSequence(String filepath)
	{
		Command[] sequence = new Command[0];
		
		JsonParser parser = new JsonParser();
		JsonElement jsonFile = null;
		JsonArray jsonSequence = null;
		try 
		{
			jsonFile = parser.parse(new FileReader(filepath));

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		if (jsonFile != null)
			jsonSequence = jsonFile.getAsJsonObject().getAsJsonArray("Sequence");
		if (jsonSequence != null)
		{
			int sequenceLength = jsonSequence.size();
			sequence = new Command[2*sequenceLength];
			for (int i = 0; i < sequenceLength; i++)
			{
				String commandType = jsonSequence.get(i).getAsJsonObject().get("Type").getAsString();
				String value = jsonSequence.get(i).getAsJsonObject().get("Value").getAsString();
				if (commandType.toLowerCase().equals("message"))
				{				
					sequence[2*i] = new Command(CommandType.Message, value);
				}
				else if (commandType.toLowerCase().equals("wait"))
				{
					sequence[2*i] = new Command(CommandType.Wait, value);
				}
				else if (commandType.toLowerCase().equals("function"))
				{
					sequence[2*i] = new Command(CommandType.Function, value);
				}		
				
				sequence[2*i+1] = new Command(CommandType.Wait, "1000");
				
			}
		}
		
		return sequence;
	}
	
	/* 
	 * Commands can be of various types (message, wait).
	 * Future implementation: define functions as commands.
	 * If 'type' is Message then 'value' is the name of the message file (without extension .json);
	 * If 'type' is Wait then 'value' is the time in milliseconds wished for the corresponding pause.
	 */
	private class Command
	{
		CommandType type;
		String value;
		
		public Command(CommandType aType, String aValue)
		{
			type = aType;
			value = aValue;
		}
	}

	/*
	 * Types of commands: 
	 * - Message to be sent
	 * - Wait for a certain amount of time before executing another command
	 * - Functions are not used in this project but are a natural extension
	 */
	public enum CommandType
	{
		Message,
		Wait,
		Function
	}
	

}


