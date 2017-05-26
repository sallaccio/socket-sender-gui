package utility;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Calendar;

import webserver.ServerSimulator;

public class Logger {

	private static String logFilePath = "log.txt";
	
	public static void Init()
	{
		try {
			PrintStream out = new PrintStream(logFilePath);
			System.setOut(out);
			System.setErr(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static void Log(String message)
	{	
		System.out.println(Calendar.getInstance().getTime().toString() + " " + message);
	}
	
	public static void Info(String message)
	{
		System.out.println(Calendar.getInstance().getTime().toString() + "    [Info]: " + message);
	}
	
	public static void Warning(String message)
	{
		System.out.println(Calendar.getInstance().getTime().toString()  + " -- [Warning]: " + message);
		ServerSimulator.FireWarning(message);
	}
	
	public static void Error(String message)
	{
		System.out.println(Calendar.getInstance().getTime().toString()  + " == [Error]: " + message);
		ServerSimulator.FireError(message);
	}
	
}
