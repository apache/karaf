package org.apache.felix.mishell;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import jline.ConsoleReader;
import jline.ConsoleReaderInputStream;

public class Console implements Runnable{
	public static final String DEFAULT_LANGUAGE = "javascript";
Logger log = Logger.getLogger(this.getClass().getCanonicalName());
	Level l=Level.FINEST;
	private String language=DEFAULT_LANGUAGE;
	private String prompt;
	BufferedReader in;
	PrintStream out; 
	private Commander commander;
	private boolean stop = false;
	private JMXEngineContext engineContext;
	private String scriptPath;

	public Console(String language, String scriptPath) throws IOException{
		if (language != null) this.language=language;
		prompt="mishell."+language+"$ ";
		/*
		 * Not used for the moment. It does not work inside Eclipse, and presents 
		problems from the command line
		*/
		//useJline();
		in=new BufferedReader(new InputStreamReader(System.in));
		out=System.out;
		commander= new Commander();
		this.scriptPath=scriptPath;
		addBuiltInCmds();
		stop=false;	
	}
	private void useJline() throws IOException{
		ConsoleReader cr=new ConsoleReader();
		ConsoleReaderInputStream.setIn(cr);

	}
	private void initLanguage() throws Exception{
		initLanguage(null);
	}
	private void initLanguage(String name) throws Exception{
		if(name!=null) {
			engineContext = new JMXEngineContext(name);
		}
		else {
			engineContext=new JMXEngineContext(language);
		}
		language=engineContext.getEngine().getFactory().getLanguageName();
		prompt="mishell."+language+"$ ";
	}
	public void run() {
				try {
					initLanguage();
					if (scriptPath==null)
					runConsole();
					else engineContext.getEngine().eval(new FileReader(scriptPath));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}


	private void runConsole() throws Exception {
		out.println("Welcome to Apache Mishell!!");
		out.println("For getting help type 'help' ");
		out.print(prompt);
		while (!stop) {
			try {
				String cmd = in.readLine();
				executeCommand(cmd);
				out.print(prompt);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void stop() {
		stop = true;
	}

	public void executeCommand(String cmd) {
		try {
			commander.executeCommand(cmd, out);
		} catch (CommandNotFoundException e) {
			try {
				Object result=engineContext.getEngine().eval(cmd);
				if(result==null)return;
				else out.println(language+": "+result);
			} catch (ScriptException se) {
				out.println(se.getMessage());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void addBuiltInCmds(){
		commander.add(new Command(){
			public void executeCommand(String cmd, PrintStream out) throws Exception {
				Set<Entry<String, Object>> bindings= engineContext.getEngine().getContext().getBindings(ScriptContext.ENGINE_SCOPE).entrySet();
				for (Entry<String, Object> entry : bindings) {
					out.println(entry.getKey()+" ["+entry.getValue().getClass().getName()+"]\n");
				}
			}
			public String getName() {
				return "browse";
			}
			public String getHelp() {
				return "prints the bindings for current engine";
			}
			
		});
		commander.add(new Command(){
			public void executeCommand(String cmd, PrintStream out) throws Exception {
				out.println("exiting console.");
				stop=true;
			}
			public String getName() {
				return "exit";
			}
			public String getHelp() {
				return "exit this console";
			}
		});
		commander.add(new Command(){
			public void executeCommand(String cmd, PrintStream out) throws Exception {
				String[] args=cmd.split(" ");//TODO implement scape seqs
				if(args.length>1)initLanguage(args[1]);
				else for (ScriptEngineFactory factory: engineContext.getEngineManager().getEngineFactories()) {
					out.print(factory.getLanguageName()+"; version "+factory.getLanguageVersion());
					out.print("; AKA: ");
					for(String alias: factory.getNames()) out.print(alias+" ");
					out.print("\n");
				
				}
			}
			public String getName() {
				return "language";
			}
			public String getHelp() {
				return "language [languageName]. Changes current language, or prints available ones";
			}
		});
		commander.add(new Command(){
			public void executeCommand(String cmd, PrintStream out) throws Exception {
				for (Command c : commander) {
					out.println(c.getName()+": "+c.getHelp());
				}
			}
			public String getName() {
				return "help";
			}
			public String getHelp() {
				return "Prints this help";
			}
		});
		commander.add(new Command(){
			public void executeCommand(String cmd, PrintStream out) throws Exception {
					String[] args=cmd.split(" ");
					if(args.length<2 || args.length>3) {
						out.println(this.getHelp());
						return;
					}
					Reader reader;
					if (args.length==2) {
					try {
						reader=new FileReader(args[1]);
					} catch(FileNotFoundException fne) {
						
						out.println("Invalid path: "+args[1]);
						return;
					}
					if(args.length==3 && (args[1].equals("-r")||args[1].equals("--remote"))) {
						try {
							URL url=new URL(args[2]);
							reader=new InputStreamReader(url.openStream());
						} catch (MalformedURLException mue) {
							out.println("bad url");
							return;
						}
					}
					try {
					engineContext.getEngine().eval(reader);
					} catch (ScriptException se) {
						out.println(se.getMessage());
					}
					}
			}
			public String getName() {
				return "load";
			}
			public String getHelp() {
				return "load [-r, --remote] scriptName. Loads the script. If remote, uses url instead of path";
			}
		});
	}


}
