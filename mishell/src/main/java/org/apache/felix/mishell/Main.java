package org.apache.felix.mishell;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
//	private static final String log_props="target/classes/logging.properties";
	private static final String log_props="logging.properties";

	public static void main(String[] args) throws Exception{
		Logger.getLogger(Console.class.getCanonicalName()).setLevel(Level.FINEST);
		InputStream is=Main.class.getResourceAsStream(log_props);
		LogManager.getLogManager().readConfiguration(is);
		is.close();
		String language=args.length==0?null:args[0];
		String scriptPath=args.length<2?null:args[1];
		Console m = new Console(language, scriptPath);
		Thread t=new Thread(m);
		t.setName("ConsoleThread");
		t.start();
	}
}
