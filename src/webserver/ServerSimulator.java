package webserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import utility.Logger;
import utility.MessagePicker;

public class ServerSimulator {

	private static String settingsFile = "settings.json";
	private static String customer, webPath, messagesPath, sequencesPath, currentActionPath;
	//private static WebServer server = new WebServer();	
	private static MessagePicker messagePicker;
	
	public static void main(String[] args) {
		Logger.Init();
		readSettings();
		logSettings();
		createMessagePicker();
	}
	
	private static void logSettings()
	{
		Logger.Info("Settings...");
		Logger.Info("Customer: " + customer);
		Logger.Info("WebPath: " + webPath);
	    if (!messagesPath.isEmpty())
	    	Logger.Info("MessagesPath: " + messagesPath);
	    if (!sequencesPath.isEmpty())
	    	Logger.Info("SequencesPath: " + sequencesPath);
	}

	private static void readSettings()
	{		
		JsonParser parser = new JsonParser();
		JsonElement jsonFile = null;
		JsonObject settings = null;
		JsonObject customerParams = null;
		
		File settingsF = new File(settingsFile);
		if (!settingsF.exists())
			generateSettingsFile();
		
			
		try {
			jsonFile = parser.parse(new FileReader(settingsFile));
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		if (jsonFile != null)
		{
			settings = jsonFile.getAsJsonObject();
			customer = settings.get("Customer").getAsString();
			customerParams = settings.get("Customers").getAsJsonObject().get(customer).getAsJsonObject();
		}
		if (customerParams != null)
		{
			webPath = customerParams.get("HtmlWebPath").getAsString();
			messagesPath = customerParams.get("MessagesPath").getAsString();
			sequencesPath = customerParams.get("SequencesPath").getAsString();
			currentActionPath = customerParams.get("CurrentActionPath").getAsString();
		}
		else //defaults
		{
			webPath = "";
			if (messagesPath.isEmpty())
				messagesPath = "action\\messages";
			if (sequencesPath.isEmpty())
				sequencesPath = "action\\sequences";
			if (currentActionPath.isEmpty())
				currentActionPath = "action\\current";
		}
		
		// Verify that paths exist
		File folder = new File(messagesPath);
		if (!folder.exists() || !folder.isDirectory())
			folder.mkdir();
		folder = new File(sequencesPath);
		if (!folder.exists() || !folder.isDirectory())
			folder.mkdir();
		folder = new File(currentActionPath);
		if (!folder.exists() || !folder.isDirectory())
			folder.mkdir();
			
	}

	private static void createMessagePicker()
	{
		System.out.println("Start Message Picker GUI");
		messagePicker = new MessagePicker();
		(messagePicker).CreateWindow(settingsFile, webPath, messagesPath, sequencesPath, currentActionPath);
	}

//	private static void startWebServer()
//	{
//		System.out.println("Before Start service");
//		server.startService(webPath, messagesPath, sequencesPath, currentActionPath);
//	}
	
	private static void generateSettingsFile()
	{
		String newSettings = "{\n" 
				+ "\t\"Customer\": \"Default\",\n"
				+ "\t\"Customers\" : {\n"
				+ "\t\t\"Default\": {\n"		
				+ "\t\t\t\"HtmlWebPath\" : \"\",\n"
				+ "\t\t\t\"MessagesPath\" : \"actions\\\\messages\",\n"
				+ "\t\t\t\"SequencesPath\" : \"actions\\\\sequences\",\n"
				+ "\t\t\t\"CurrentActionPath\" : \"actions\\\\current\"\n"			
				+ "\t\t}\n"
				+ "\t}\n"
				+ "}";
		
		BufferedWriter file;
		try {
			file = new BufferedWriter(new FileWriter(settingsFile));
			file.write(newSettings);
			file.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}	
	}
	
	public static void ResetSettings()
	{
		readSettings();
		messagePicker.SetPaths(settingsFile, webPath, messagesPath, sequencesPath, currentActionPath);
	}
	
	public static void FireWarning(String message)
	{
		messagePicker.Warning(message);
	}
	
	public static void FireError(String message)
	{
		messagePicker.Error(message);
	}
	
}
