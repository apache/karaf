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
package org.apache.geronimo.gshell.spring;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.common.io.PumpStreamHandler;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;

/**
 * Creates a new servicemix instance 
 *
 * @version $Rev$ $Date$
 */
@CommandComponent(id="smx:create", description="Create a new ServiceMix instance")
public class CreateCommand
    extends OsgiCommandSupport
{
    @Argument(index=0, required=true, description="Where to create the new ServiceMix instance")
    private String instance = null;

    protected Object doExecute() throws Exception {
    	
    	try {
			File serviceMixBase = new File(instance).getCanonicalFile();
			io.out.println("Creating new instance at:@|bold "+serviceMixBase+"|");
			
			mkdir(serviceMixBase, "bin");
			mkdir(serviceMixBase, "etc");
			mkdir(serviceMixBase, "system");
			mkdir(serviceMixBase, "deploy");
			mkdir(serviceMixBase, "data");
			
			copyResourceToDir(serviceMixBase, "etc/config.properties", true);
			copyResourceToDir(serviceMixBase, "etc/login.conf", true);
			copyResourceToDir(serviceMixBase, "etc/org.apache.servicemix.management.cfg", true);
			copyResourceToDir(serviceMixBase, "etc/org.apache.servicemix.shell.cfg", true);
			copyResourceToDir(serviceMixBase, "etc/org.ops4j.pax.logging.cfg", true);
			copyResourceToDir(serviceMixBase, "etc/system.properties", true);

			HashMap<String, String> props = new HashMap<String, String>();
			props.put("${servicemix.home}", System.getProperty("servicemix.home"));
			props.put("${servicemix.base}", serviceMixBase.getPath());
			if( System.getProperty("os.name").startsWith("Win") ) {
			    copyFilteredResourceToDir(serviceMixBase, "bin/servicemix.bat", props);
			} else {
			    copyFilteredResourceToDir(serviceMixBase, "bin/servicemix", props);
			    chmod(new File(serviceMixBase, "bin/servicemix"), "a+x");
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

        return 0;
    }

	private void copyResourceToDir(File target, String resource, boolean text) throws Exception {
		File outFile = new File(target, resource);
		if( !outFile.exists() ) {
	        io.out.println("Creating file:@|bold "+outFile.getPath()+"|");
			InputStream is = CreateCommand.class.getResourceAsStream(resource);
			try {
				if( text ) {
					// Read it line at a time so that we can use the platform line ending when we write it out.
					PrintStream out = new PrintStream(new FileOutputStream(outFile));
					try { 
						Scanner scanner = new Scanner(is);
						while (scanner.hasNextLine() ) {
							String line = scanner.nextLine();
							out.println(line);
						}
					} finally {
						safeClose(out);
					}
				} else {
					// Binary so just write it out the way it came in.
					FileOutputStream out = new FileOutputStream(new File(target, resource));
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
	
	private void copyFilteredResourceToDir(File target, String resource, HashMap<String, String> props) throws Exception {
		File outFile = new File(target, resource);
		if( !outFile.exists() ) {
	        io.out.println("Creating file:@|bold "+outFile.getPath()+"|");
			InputStream is = CreateCommand.class.getResourceAsStream(resource);
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

	private void mkdir(File serviceMixBase, String path) {
		File file = new File(serviceMixBase, path);
		if( !file.exists() ) {
	        io.out.println("Creating dir:@|bold "+file.getPath()+"|");
			file.mkdirs();
		}
		
	}
	
	private int chmod(File serviceFile, String mode) throws Exception {
		ProcessBuilder builder = new ProcessBuilder();
		builder.command("chmod", mode, serviceFile.getCanonicalPath());
        Process p = builder.start();

        PumpStreamHandler handler = new PumpStreamHandler(io.inputStream, io.outputStream, io.errorStream);
        handler.attach(p);
        handler.start();
        int status = p.waitFor();
        handler.stop();
        return status;
	}

}
