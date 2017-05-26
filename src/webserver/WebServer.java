package webserver; /**
 * Created by IntelliJ IDEA.
 * User: frengo
 * Date: 11/20/10
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */

import com.sun.grizzly.arp.DefaultAsyncHandler;
import com.sun.grizzly.http.embed.GrizzlyWebServer;
//import com.sun.grizzly.tcp.http11.GrizzlyAdapter;
//import com.sun.grizzly.tcp.http11.GrizzlyRequest;
//import com.sun.grizzly.tcp.http11.GrizzlyResponse;
import com.sun.grizzly.websockets.WebSocketAsyncFilter;
import com.sun.grizzly.websockets.WebSocketEngine;
import com.sun.grizzly.websockets.WebSocketApplication;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;


import utility.Logger;
import websocket.WebSocketApp;

import java.io.*;

public class WebServer {
  public static final int PORT = 5150;

  GrizzlyWebServer gws;
  //private transient Log log = LogFactory.getLog(getClass().getName());
  
  public void stopWebServer() 
  {
	if (gws != null)
	{
		Logger.Info("WebServer Stop");
		gws.stop();
	}
  }

  public Boolean startService(String webPath, String messagesPath, String sequencesPath, String currentActionPath) 
  {
    Logger.Info("WebServer Starting");
   
    gws = new GrizzlyWebServer(PORT,webPath);
    try {
    	
      gws.getSelectorThread().setAsyncHandler(new DefaultAsyncHandler());
      gws.getSelectorThread().setEnableAsyncExecution(true);
      gws.getSelectorThread().getAsyncHandler().addAsyncFilter(new WebSocketAsyncFilter());
      
      WebSocketApplication app = new WebSocketApp(messagesPath, sequencesPath, currentActionPath);

      WebSocketEngine.getEngine().register(app);
      gws.start();
      Logger.Info("WebServer Started");
      
      return true;  
      
    } catch (IOException e) {
    	
      e.printStackTrace();
      Logger.Info("WebServer Stop");
      gws.stop();
      
      return false;
    }

  }

}
