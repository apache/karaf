/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.runtime.gshell.wrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.geronimo.gshell.clp.Option;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;

/**
 * Installs this ServiceMix instance as a service in your operating systems. 
 *
 * @version $Rev$ $Date$
 */
@CommandComponent(id="wrapper:install", description="Installs this ServiceMix instance as a service in your operating systems.")
public class InstallCommand
    extends OsgiCommandSupport
{
	
    @Option(name="-n", aliases={"--name"}, description="The service name that will be used when installing the service.  Defaults to the directory name of the instance.")
    private String name;
    @Option(name="-d", aliases={"--display"}, description="The display name of the service.")
    private String displayName;
    @Option(name="-D", aliases={"--description"}, description="The description of the service.")
    private String description="";
    @Option(name="-s", aliases={"--start-type"}, description="Mode in which the service is installed.  AUTO_START or DEMAND_START")
    private String startType="AUTO_START";

    protected Object doExecute() throws Exception {
    	
    	try {
    		String name = getName();    		
    		File base = new File(System.getProperty("servicemix.base"));
    		File bin = new File(base, "bin");
    		File etc = new File(base, "etc");
    		File lib = new File(base, "lib");
    		
			HashMap<String, String> props = new HashMap<String, String>();
			props.put("${servicemix.home}", System.getProperty("servicemix.home"));
			props.put("${servicemix.base}", base.getPath());
			props.put("${name}", name);
			props.put("${displayName}", getDisplayName());
			props.put("${description}", getDescription());
			props.put("${startType}", getStartType());
			
			String os = System.getProperty("os.name", "Unknown");
			File serviceFile=null;
			if( os.startsWith("Win") ) {
				mkdir(bin);
				copyResourceToDir(bin, "windows/servicemix-wrapper.exe", false);
				serviceFile = new File(bin,"servicemix-service.bat");
				copyFilteredResourceTo(serviceFile, "windows/servicemix-service.bat", props);
				mkdir(lib);
				copyResourceToDir(lib, "windows/wrapper.dll", false);								
			} else if( os.startsWith("Mac OS X") ) {
				mkdir(bin);
				copyResourceToDir(bin, "macosx/servicemix-wrapper", false);
				serviceFile = new File(bin,"servicemix-service");
				copyFilteredResourceTo(serviceFile, "unix/servicemix-service", props);
				mkdir(lib);
				copyResourceToDir(lib, "macosx/libwrapper.jnilib", false);
				
				// TODO: figure out how to hook in the service that it starts up
				// when the machine boots up.
			} else if( os.startsWith("Linux") ) {
				mkdir(bin);
				copyResourceToDir(bin, "linux/servicemix-wrapper", false);
				serviceFile = new File(bin,"servicemix-service");
				copyFilteredResourceTo(serviceFile, "unix/servicemix-service", props);
				mkdir(lib);
				copyResourceToDir(lib, "linux/libwrapper.jnilib", false);
				
				// TODO: figure out how to hook in the service that it starts up
				// when the machine boots up.
			} else {
		        io.out.println("Your operating system '"+os+"' is not currently supported.");
		        return 1;
			}

    		// Install the wrapper jar to the lib directory..
			mkdir(lib);
			copyResourceToDir(lib, "all/servicemix-wrapper.jar", false);
			mkdir(etc);
			File wrapperConf = new File(etc,"servicemix-wrapper.conf");
			copyFilteredResourceTo(wrapperConf, "all/servicemix-wrapper.conf", props);

			io.out.println("Setup complete.  You may want to tweak the JVM properties in the wrapper configuration file: "+wrapperConf.getPath());
			io.out.println("before installing and starting the service.");
			io.out.println("");
			if( os.startsWith("Win") ) {
				io.out.println("To install the service, run: ");
				io.out.println("  C:> "+serviceFile.getPath()+" install");
				io.out.println("");
				io.out.println("Once installed, to start the service run: ");
				io.out.println("  C:> net start \""+name+"\"");
				io.out.println("");
				io.out.println("Once running, to stop the service run: ");
				io.out.println("  C:> net stop \""+name+"\"");
				io.out.println("");
				io.out.println("Once stopped, to remove the installed the service run: ");
				io.out.println("  C:> "+serviceFile.getPath()+" remove");
				io.out.println("");
			} else if( os.startsWith("Mac OS X") ) {
			} else if( os.startsWith("Linux") ) {
				io.out.println("The way the service is installed depends upon your flavor of Linux. ");
				io.out.println("On Redhat Systems you run:");
				io.out.println("  ln -s "+serviceFile.getPath()+" /etc/init.d/");
				io.out.println("  service add "+serviceFile.getName());
				io.out.println("");
			}

			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

        return 0;
    }

	private void copyResourceToDir(File target, String resource, boolean text) throws Exception {
		File outFile = new File(target, new File(resource).getName());
		if( !outFile.exists() ) {
	        io.out.println("Creating file: "+outFile.getPath()+"");
			InputStream is = InstallCommand.class.getResourceAsStream(resource);
			try {
				if( text ) {
					// Read it line at a time so that we can use the platform line ending when we write it out.
					PrintStream out = new PrintStream(new FileOutputStream(outFile));
					try { 
						Scanner scanner = new Scanner(is);
						while (scanner.hasNextLine() ) {
							String line = scanner.nextLine();
							io.out.println("writing: "+line);
							out.println(line);
						}
					} finally {
						safeClose(out);
					}
				} else {
					// Binary so just write it out the way it came in.
					FileOutputStream out = new FileOutputStream(outFile);
					try {
						int c=0;
						while((c=is.read())>=0) {
							out.write(c);
						}
					} finally {
						safeClose(out);
					}
				}
			} finally {
				safeClose(is);
			}
		}
	}
	
	private void copyFilteredResourceTo(File outFile, String resource, HashMap<String, String> props) throws Exception {
		if( !outFile.exists() ) {
	        io.out.println("Creating file: "+outFile.getPath()+"");
			InputStream is = InstallCommand.class.getResourceAsStream(resource);
			try {
				// Read it line at a time so that we can use the platform line ending when we write it out.
				PrintStream out = new PrintStream(new FileOutputStream(outFile));
				try { 
					Scanner scanner = new Scanner(is);
					while (scanner.hasNextLine() ) {
						String line = scanner.nextLine();
						line = filter(line, props);
						out.println(line);
					}
				} finally {
					safeClose(out);
				}
			} finally {
				safeClose(is);
			}
		}
	}

	private void safeClose(InputStream is) throws IOException {
		if( is==null)
			return;
		try {
			is.close();
		} catch (Throwable ignore) {
		}
	}
	
	private void safeClose(OutputStream is) throws IOException {
		if( is==null)
			return;
		try {
			is.close();
		} catch (Throwable ignore) {
		}
	}

	private String filter(String line, HashMap<String, String> props) {
		for (Map.Entry<String, String> i : props.entrySet()) {
			int p1 = line.indexOf(i.getKey());
			if( p1 >= 0 ) {
				String l1 = line.substring(0, p1);
				String l2 = line.substring(p1+i.getKey().length());
				line = l1+i.getValue()+l2;
			}
		}
		return line;
	}

	private void mkdir(File file) {
		if( !file.exists() ) {
	        io.out.println("Creating dir:@|bold "+file.getPath()+"|");
			file.mkdirs();
		}
	}

	public String getName() {
		if( name ==  null ) {
    		File base = new File(System.getProperty("servicemix.base"));
    		name = base.getName();
		}
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		if( displayName == null ) {
			displayName = getName();
		}
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getStartType() {
		return startType;
	}

	public void setStartType(String startType) {
		this.startType = startType;
	}
}
