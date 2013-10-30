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
package org.apache.karaf.wrapper.commands;

import java.io.File;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;
import org.apache.karaf.wrapper.WrapperService;
import org.apache.karaf.wrapper.internal.WrapperServiceImpl;
import org.fusesource.jansi.Ansi;

/**
 * Installs the Karaf instance as a service in your operating system.
 */
@Command(scope = "wrapper", name = "install", description = "Install the container as a system service in the OS.")
public class Install extends AbstractAction {

	@Option(name = "-n", aliases = { "--name" }, description = "The service name that will be used when installing the service. (Default: karaf)", required = false, multiValued = false)
	private String name = "karaf";

	@Option(name = "-d", aliases = { "--display" }, description = "The display name of the service.", required = false, multiValued = false)
	private String displayName;

	@Option(name = "-D", aliases = { "--description" }, description = "The description of the service.", required = false, multiValued = false)
	private String description = "";

	@Option(name = "-s", aliases = { "--start-type" }, description = "Mode in which the service is installed. AUTO_START or DEMAND_START (Default: AUTO_START)", required = false, multiValued = false)
	private String startType = "AUTO_START";

	private WrapperService wrapperService = new WrapperServiceImpl();

	public void setWrapperService(WrapperService wrapperService) {
		this.wrapperService = wrapperService;
	}

	protected Object doExecute() throws Exception {
        File[] wrapperPaths = wrapperService.install(name, displayName, description, startType);

        String os = System.getProperty("os.name", "Unknown");
        File wrapperConf = wrapperPaths[0];
        File serviceFile = wrapperPaths[1];

        System.out.println("");
        System.out.println("Setup complete.  You may wish to tweak the JVM properties in the wrapper configuration file:");
        System.out.println("\t" + wrapperConf.getPath());
        System.out.println("before installing and starting the service.");
        System.out.println("");
        if (os.startsWith("Win")) {
            System.out.println("");
            System.out.println("To install the service, run: ");
            System.out.println("  C:> " + serviceFile.getPath() + " install");
            System.out.println("");
            System.out.println("Once installed, to start the service run: ");
            System.out.println("  C:> net start \"" + name + "\"");
            System.out.println("");
            System.out.println("Once running, to stop the service run: ");
            System.out.println("  C:> net stop \"" + name + "\"");
            System.out.println("");
            System.out.println("Once stopped, to remove the installed the service run: ");
            System.out.println("  C:> " + serviceFile.getPath() + " remove");
            System.out.println("");
        } else if (os.startsWith("Mac OS X")) {
            System.out.println("");
            System.out.println("to add bin/org.apache.karaf.KARAF as user service move this file into ~/Library/LaunchAgents/");  
            System.out.println("> mv bin/org.apache.karaf.KARAF.plist ~/Library/LaunchAgents/");
            System.out.println("");
            System.out.println("to add org.apache.karaf.KARAF as system service move this into /Library/LaunchDaemons");  
            System.out.println("> sudo mv bin/org.apache.karaf.KARAF.plist /Library/LaunchDaemons/");  
            System.out.println("change owner and rights");  
            System.out.println("> sudo chown root:wheel /Library/LaunchDaemons/org.apache.karaf.KARAF.plist");  
            System.out.println("> sudo chmod u=rw,g=r,o=r /Library/LaunchDaemons/org.apache.karaf.KARAF.plist");  
            System.out.println(""); 
            System.out.println("test your service");  
            System.out.println("> launchctl load ~/Library/LaunchAgents/org.apache.karaf.KARAF.plist");  
            System.out.println("> launchctl start org.apache.karaf.KARAF");  
            System.out.println("> launchctl stop org.apache.karaf.KARAF");  
            System.out.println("");  
            System.out.println("after restart your session or system");  
            System.out.println("you can use launchctl command to start and stop your service");  
            System.out.println("");  
            System.out.println("for removing the service call");  
            System.out.println("> launchctl remove org.apache.karaf.KARAF");  
            System.out.println("");
        } else if (os.startsWith("Linux")) {

            File debianVersion = new File("/etc/debian_version");
            File redhatRelease = new File("/etc/redhat-release");

            if (redhatRelease.exists()) {
                System.out.println("");
                System.out.println("  To install the service:");
                System.out.println("    $ ln -s " + serviceFile.getPath() + " /etc/init.d/");
                System.out.println("    $ chkconfig " + serviceFile.getName() + " --add");
                System.out.println("");
                System.out.println("  To start the service when the machine is rebooted:");
                System.out.println("    $ chkconfig " + serviceFile.getName() + " on");
                System.out.println("");
                System.out.println("  To disable starting the service when the machine is rebooted:");
                System.out.println("    $ chkconfig " + serviceFile.getName() + " off");
                System.out.println("");
                System.out.println("  To start the service:");
                System.out.println("    $ service " + serviceFile.getName() + " start");
                System.out.println("");
                System.out.println("  To stop the service:");
                System.out.println("    $ service " + serviceFile.getName() + " stop");
                System.out.println("");
                System.out.println("  To uninstall the service :");
                System.out.println("    $ chkconfig " + serviceFile.getName() + " --del");
                System.out.println("    $ rm /etc/init.d/" + serviceFile.getPath());
            } else if (debianVersion.exists()) {
                System.out.println("");
                System.out.println("  To install the service:");
                System.out.println("    $ ln -s " + serviceFile.getPath() + " /etc/init.d/");
                System.out.println("");
                System.out.println("  To start the service when the machine is rebooted:");
                System.out.println("    $ update-rc.d " + serviceFile.getName() + " defaults");
                System.out.println("");
                System.out.println("  To disable starting the service when the machine is rebooted:");
                System.out.println("    $ update-rc.d -f " + serviceFile.getName() + " remove");
                System.out.println("");
                System.out.println("  To start the service:");
                System.out.println("    $ /etc/init.d/" + serviceFile.getName() + " start");
                System.out.println("");
                System.out.println("  To stop the service:");
                System.out.println("    $ /etc/init.d/" + serviceFile.getName() + " stop");
                System.out.println("");
                System.out.println("  To uninstall the service :");
                System.out.println("    $ rm /etc/init.d/" + serviceFile.getName());
            } else {
				System.out.println("");
                System.out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("On Redhat/Fedora/CentOS Systems:").a(Ansi.Attribute.RESET).toString());
				System.out.println("  To install the service:");
				System.out.println("    $ ln -s "+serviceFile.getPath()+" /etc/init.d/");
				System.out.println("    $ chkconfig "+serviceFile.getName()+" --add");
				System.out.println("");
				System.out.println("  To start the service when the machine is rebooted:");
				System.out.println("    $ chkconfig "+serviceFile.getName()+" on");
				System.out.println("");
				System.out.println("  To disable starting the service when the machine is rebooted:");
				System.out.println("    $ chkconfig "+serviceFile.getName()+" off");
				System.out.println("");
				System.out.println("  To start the service:");
				System.out.println("    $ service "+serviceFile.getName()+" start");
				System.out.println("");
				System.out.println("  To stop the service:");
				System.out.println("    $ service "+serviceFile.getName()+" stop");
				System.out.println("");
				System.out.println("  To uninstall the service :");
				System.out.println("    $ chkconfig "+serviceFile.getName()+" --del");
				System.out.println("    $ rm /etc/init.d/"+serviceFile.getName());

				System.out.println("");
                System.out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("On Ubuntu/Debian Systems:").a(Ansi.Attribute.RESET).toString());
				System.out.println("  To install the service:");
				System.out.println("    $ ln -s "+serviceFile.getPath()+" /etc/init.d/");
				System.out.println("");
				System.out.println("  To start the service when the machine is rebooted:");
				System.out.println("    $ update-rc.d "+serviceFile.getName()+" defaults");
				System.out.println("");
				System.out.println("  To disable starting the service when the machine is rebooted:");
				System.out.println("    $ update-rc.d -f "+serviceFile.getName()+" remove");
				System.out.println("");
				System.out.println("  To start the service:");
				System.out.println("    $ /etc/init.d/"+serviceFile.getName()+" start");
				System.out.println("");
				System.out.println("  To stop the service:");
				System.out.println("    $ /etc/init.d/"+serviceFile.getName()+" stop");
				System.out.println("");
				System.out.println("  To uninstall the service :");
				System.out.println("    $ rm /etc/init.d/"+serviceFile.getName());
            }

        }

        return null;
    }
}
