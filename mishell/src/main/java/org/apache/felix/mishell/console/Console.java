/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.mishell.console;

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

import org.apache.felix.mishell.EngineNotFoundException;
import org.apache.felix.mishell.JMXEngineContext;


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
	public Console(JMXEngineContext engineContext) throws IOException{
		this.engineContext=engineContext;
		prompt="mishell."+engineContext.getLanguage()+"$ ";
		in=new BufferedReader(new InputStreamReader(System.in));
		out=System.out;
		commander= new Commander();
		addBuiltInCmds();
		stop=false;	
	}
	private void setLanguage(String name)throws EngineNotFoundException{
		engineContext.setLanguage(name);
		language=engineContext.getEngine().getFactory().getLanguageName();
		prompt="mishell."+language+"$ ";

	}
	public void run() {
				try {
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
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
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
	public void addCommand(Command cmd){
		commander.add(cmd);
	}
	/**
	 * This method is needed for non-trivial commands that could eventually be added, as 
	 * they will need to use the engineContext to do useful things
	 * @return
	 */
	public JMXEngineContext getEngineContext(){
		return engineContext;
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
				String[] args=cmd.split(" ");//TODO implement scape seqs, that is, if path contains spaces, for example.
				if(args.length>1){
					setLanguage(args[1]);
				} else{
					for (ScriptEngineFactory factory: engineContext.getEngineManager().getEngineFactories()) {
						out.print(factory.getLanguageName()+"; version "+factory.getLanguageVersion());
						out.print("; AKA: ");
						for(String alias: factory.getNames()) out.print(alias+" ");
							out.print("\n");
						}
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
