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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.wrapper.WrapperService;

import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_BOLD;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_NORMAL;

/**
 * Installs the Karaf instance as a service in your operating system.
 */
@Command(scope = "wrapper", name = "install", description = "Install the container as a system service in the OS.")
@Service
public class Install implements Action {

	@Option(name = "-n", aliases = { "--name" }, description = "The service name that will be used when installing the service. (Default: karaf)", required = false, multiValued = false)
	private String name = "karaf";

	@Option(name = "-d", aliases = { "--display" }, description = "The display name of the service.", required = false, multiValued = false)
	private String displayName = "karaf";

	@Option(name = "-D", aliases = { "--description" }, description = "The description of the service.", required = false, multiValued = false)
	private String description = "";

	@Option(name = "-s", aliases = { "--start-type" }, description = "Mode in which the service is installed. AUTO_START or DEMAND_START (Default: AUTO_START)", required = false, multiValued = false)
	private String startType = "AUTO_START";
	
	@Option(name = "-e", aliases = {"--env"}, description = "Specify environment variable and values. To specify multiple environment variable and values, specify this flag multiple times.", required = false, multiValued = true)
	private String[] envs;
	    
	@Option(name = "-i", aliases = {"--include"}, description = "Specify include statement for JSW wrapper conf. To specify multiple include statement, specify this flag multiple times.", required = false, multiValued = true)
	private String[] includes;

    @Reference
	private WrapperService wrapperService;

    @Override
	public Object execute() throws Exception {
        File[] wrapperPaths = wrapperService.install(name, displayName, description, startType, envs, includes);

        String os = System.getProperty("os.name", "Unknown");
        File wrapperConf = wrapperPaths[0];
        File serviceFile = wrapperPaths[1];
        File systemdFile = wrapperPaths[2];

        System.out.println();
        System.out.println("Setup complete.  You may wish to tweak the JVM properties in the wrapper configuration file:");
        System.out.println("\t" + wrapperConf.getPath());
        System.out.println("before installing and starting the service.");
        System.out.println();
        if (os.startsWith("Win")) {
            System.out.println();
            System.out.println(INTENSITY_BOLD + "MS Windows system detected:" + INTENSITY_NORMAL);
            System.out.println("To install the service, run: ");
            System.out.println("  C:> " + serviceFile.getPath() + " install");
            System.out.println();
            System.out.println("Once installed, to start the service run: ");
            System.out.println("  C:> net start \"" + name + "\"");
            System.out.println();
            System.out.println("Once running, to stop the service run: ");
            System.out.println("  C:> net stop \"" + name + "\"");
            System.out.println();
            System.out.println("Once stopped, to remove the installed the service run: ");
            System.out.println("  C:> " + serviceFile.getPath() + " remove");
            System.out.println();
        } else if (os.startsWith("Mac OS X")) {
            System.out.println();
            System.out.println(INTENSITY_BOLD + "Mac OS X system detected:" + INTENSITY_NORMAL);
            System.out.println("to add bin/org.apache.karaf.KARAF as user service move this file into ~/Library/LaunchAgents/");  
            System.out.println("> mv bin/org.apache.karaf.KARAF.plist ~/Library/LaunchAgents/");
            System.out.println();
            System.out.println("to add org.apache.karaf.KARAF as system service move this into /Library/LaunchDaemons");  
            System.out.println("> sudo mv bin/org.apache.karaf.KARAF.plist /Library/LaunchDaemons/");  
            System.out.println("change owner and rights");  
            System.out.println("> sudo chown root:wheel /Library/LaunchDaemons/org.apache.karaf.KARAF.plist");  
            System.out.println("> sudo chmod u=rw,g=r,o=r /Library/LaunchDaemons/org.apache.karaf.KARAF.plist");  
            System.out.println();
            System.out.println("test your service");  
            System.out.println("> launchctl load ~/Library/LaunchAgents/org.apache.karaf.KARAF.plist");  
            System.out.println("> launchctl start org.apache.karaf.KARAF");  
            System.out.println("> launchctl stop org.apache.karaf.KARAF");  
            System.out.println();
            System.out.println("after restart your session or system");  
            System.out.println("you can use launchctl command to start and stop your service");  
            System.out.println();
            System.out.println("for removing the service call");  
            System.out.println("> launchctl remove org.apache.karaf.KARAF");  
            System.out.println();
        } else if (os.startsWith("Linux")) {

            File debianVersion = new File("/etc/debian_version");
            File redhatRelease = new File("/etc/redhat-release");

            if (redhatRelease.exists()) {
                System.out.println();
                System.out.println(INTENSITY_BOLD + "RedHat/Fedora/CentOS Linux system detected (SystemV):" + INTENSITY_NORMAL);
                System.out.println("  To install the service:");
                System.out.println("    $ ln -s " + serviceFile.getPath() + " /etc/init.d/");
                System.out.println("    $ chkconfig " + serviceFile.getName() + " --add");
                System.out.println();
                System.out.println("  To start the service when the machine is rebooted:");
                System.out.println("    $ chkconfig " + serviceFile.getName() + " on");
                System.out.println();
                System.out.println("  To disable starting the service when the machine is rebooted:");
                System.out.println("    $ chkconfig " + serviceFile.getName() + " off");
                System.out.println();
                System.out.println("  To start the service:");
                System.out.println("    $ service " + serviceFile.getName() + " start");
                System.out.println();
                System.out.println("  To stop the service:");
                System.out.println("    $ service " + serviceFile.getName() + " stop");
                System.out.println();
                System.out.println("  To uninstall the service :");
                System.out.println("    $ chkconfig " + serviceFile.getName() + " --del");
                System.out.println("    $ rm /etc/init.d/" + serviceFile.getName());
            } else if (debianVersion.exists()) {
                System.out.println();
                System.out.println(INTENSITY_BOLD + "Ubuntu/Debian Linux system detected (SystemV):" + INTENSITY_NORMAL);
                System.out.println("  To install the service:");
                System.out.println("    $ ln -s " + serviceFile.getPath() + " /etc/init.d/");
                System.out.println();
                System.out.println("  To start the service when the machine is rebooted:");
                System.out.println("    $ update-rc.d " + serviceFile.getName() + " defaults");
                System.out.println();
                System.out.println("  To disable starting the service when the machine is rebooted:");
                System.out.println("    $ update-rc.d -f " + serviceFile.getName() + " remove");
                System.out.println();
                System.out.println("  To start the service:");
                System.out.println("    $ /etc/init.d/" + serviceFile.getName() + " start");
                System.out.println();
                System.out.println("  To stop the service:");
                System.out.println("    $ /etc/init.d/" + serviceFile.getName() + " stop");
                System.out.println();
                System.out.println("  To uninstall the service :");
                System.out.println("    $ rm /etc/init.d/" + serviceFile.getName());
            } else {
				System.out.println();
                                System.out.println(INTENSITY_BOLD + "On Redhat/Fedora/CentOS Systems (SystemV):" + INTENSITY_NORMAL);
				System.out.println("  To install the service:");
				System.out.println("    $ ln -s "+serviceFile.getPath()+" /etc/init.d/");
				System.out.println("    $ chkconfig "+serviceFile.getName()+" --add");
				System.out.println();
				System.out.println("  To start the service when the machine is rebooted:");
				System.out.println("    $ chkconfig "+serviceFile.getName()+" on");
				System.out.println();
				System.out.println("  To disable starting the service when the machine is rebooted:");
				System.out.println("    $ chkconfig "+serviceFile.getName()+" off");
				System.out.println();
				System.out.println("  To start the service:");
				System.out.println("    $ service "+serviceFile.getName()+" start");
				System.out.println();
				System.out.println("  To stop the service:");
				System.out.println("    $ service "+serviceFile.getName()+" stop");
				System.out.println();
				System.out.println("  To uninstall the service :");
				System.out.println("    $ chkconfig "+serviceFile.getName()+" --del");
				System.out.println("    $ rm /etc/init.d/"+serviceFile.getName());

				System.out.println();
                                System.out.println(INTENSITY_BOLD + "On Ubuntu/Debian Systems (SystemV):" + INTENSITY_NORMAL);
				System.out.println("  To install the service:");
				System.out.println("    $ ln -s "+serviceFile.getPath()+" /etc/init.d/");
				System.out.println();
				System.out.println("  To start the service when the machine is rebooted:");
				System.out.println("    $ update-rc.d "+serviceFile.getName()+" defaults");
				System.out.println();
				System.out.println("  To disable starting the service when the machine is rebooted:");
				System.out.println("    $ update-rc.d -f "+serviceFile.getName()+" remove");
				System.out.println();
				System.out.println("  To start the service:");
				System.out.println("    $ /etc/init.d/"+serviceFile.getName()+" start");
				System.out.println();
				System.out.println("  To stop the service:");
				System.out.println("    $ /etc/init.d/"+serviceFile.getName()+" stop");
				System.out.println();
				System.out.println("  To uninstall the service :");
				System.out.println("    $ rm /etc/init.d/"+serviceFile.getName());
            }

            if (systemdFile != null) {
                System.out.println();
                System.out.println(INTENSITY_BOLD + "For systemd compliant Linux: " + INTENSITY_NORMAL);
                System.out.println("  To install the service (and enable at system boot):");
                System.out.println("   $ systemctl enable " + systemdFile.getPath());
                System.out.println();
                System.out.println("  To start the service:");
                System.out.println("   $ systemctl start " + name);
                System.out.println();
                System.out.println("  To stop the service:");
                System.out.println("   $ systemctl stop " + name);
                System.out.println();
                System.out.println("  To check the current service status:");
                System.out.println("   $ systemctl status " + name);
                System.out.println();
                System.out.println("  To see service activity journal:");
                System.out.println("   $ journalctl -u " + name);
                System.out.println();
                System.out.println("  To uninstall the service (and disable at system boot):");
                System.out.println("   $ systemctl disable " + name);
            }

        } else if (os.startsWith("Solaris") || os.startsWith("SunOS")) {
            System.out.println();
            System.out.println(INTENSITY_BOLD + "Solaris/SunOS system detected :" + INTENSITY_NORMAL);
            System.out.println("  To install the service (and enable at system boot):");
            System.out.println("    $ ln -s " + serviceFile.getPath() + " /etc/init.d/");
            System.out.println();
            System.out.println("  To start the service when the machine is rebooted for all multi-user run levels");
            System.out.println("  and stopped for the halt, single-user and reboot runlevels:");
            System.out.println("    $ ln -s /etc/init.d/" + serviceFile.getName() + " /etc/rc0.d/K20" + serviceFile.getName());
            System.out.println("    $ ln -s /etc/init.d/" + serviceFile.getName() + " /etc/rc1.d/K20" + serviceFile.getName());
            System.out.println("    $ ln -s /etc/init.d/" + serviceFile.getName() + " /etc/rc2.d/S20" + serviceFile.getName());
            System.out.println("    $ ln -s /etc/init.d/" + serviceFile.getName() + " /etc/rc3.d/S20" + serviceFile.getName());
            System.out.println();
            System.out.println("    If your application makes use of other services, then you will need to make");
            System.out.println("    sure that your application is started after, and then shutdown before. This");
            System.out.println("    is done by controlling the startup/shutdown order by setting the right order");
            System.out.println("    value, which in this example it set to 20."); 
            System.out.println();
            System.out.println("  To start the service:");
            System.out.println("    $ /etc/init.d/" + serviceFile.getName() + " start");
            System.out.println();
            System.out.println("  To stop the service:");
            System.out.println("    $ /etc/init.d/" + serviceFile.getName() + " stop");
            System.out.println();
            System.out.println("  To uninstall the service :");
            System.out.println("    $ rm /etc/init.d/" + serviceFile.getName());
            System.out.println("    $ rm /etc/rc0.d/K20" + serviceFile.getName());
            System.out.println("    $ rm /etc/rc1.d/K20" + serviceFile.getName());
            System.out.println("    $ rm /etc/rc2.d/S20" + serviceFile.getName());
            System.out.println("    $ rm /etc/rc3.d/S20" + serviceFile.getName());
        } else if (os.startsWith("AIX")) {
            System.out.println();
            System.out.println(INTENSITY_BOLD + "AIX system detected :" + INTENSITY_NORMAL);
            System.out.println("  To install the service (and enable at system boot):");
            System.out.println("    $ ln -s " + serviceFile.getPath() + " /etc/rc.d/init.d/");
            System.out.println();
            System.out.println("  To start the service when the machine is rebooted for all multi-user run levels");
            System.out.println("  and stopped for the halt, single-user and reboot runlevels:");
            System.out.println("    $ ln -s /etc/rc.d/init.d/" + serviceFile.getName() + " /etc/rc2.d/K20" + serviceFile.getName());
            System.out.println("    $ ln -s /etc/rc.d/init.d/" + serviceFile.getName() + " /etc/rc2.d/S20" + serviceFile.getName());
            System.out.println();
            System.out.println("    If your application makes use of other services, then you will need to make");
            System.out.println("    sure that your application is started after, and then shutdown before. This");
            System.out.println("    is done by controlling the startup/shutdown order by setting the right order");
            System.out.println("    value, which in this example it set to 20."); 
            System.out.println();
            System.out.println("  To start the service:");
            System.out.println("    $ /etc/rc.d/init.d/" + serviceFile.getName() + " start");
            System.out.println();
            System.out.println("  To stop the service:");
            System.out.println("    $ /etc/rc.d/init.d/" + serviceFile.getName() + " stop");
            System.out.println();
            System.out.println("  To uninstall the service :");
            System.out.println("    $ rm /etc/rc.d/init.d/" + serviceFile.getName());
            System.out.println("    $ rm /etc/rc2.d/K20" + serviceFile.getName());
            System.out.println("    $ rm /etc/rc2.d/S20" + serviceFile.getName());
        } else if (os.startsWith("HP-UX")) {
            System.out.println();
            System.out.println(INTENSITY_BOLD + "HP-UX system detected :" + INTENSITY_NORMAL);
            System.out.println("  To install the service (and enable at system boot):");
            System.out.println("    $ cp /sbin/init.d/template /sbin/init.d/" + serviceFile.getName());
            System.out.println("    $ cat /sbin/init.d/" + serviceFile.getName() +" | sed 's/<specific>/" + serviceFile.getName() + "/g' | \\ ");
            System.out.println("      awk '/# Execute the commands to/{print;print \"                set_return\";next}1' | \\ ");
            System.out.println("      sed 's/CONTROL_VARIABLE/CONTROL_VARIABLE_KARAF/g' | \\ ");
            System.out.println("      sed 's@# Execute the commands to start your subsystem@        " + serviceFile.getPath() + " start@g' | \\ ");
            System.out.println("      sed 's@# Execute the commands to stop your subsystem@        " + serviceFile.getPath() + " stop@g' | \\ ");
            System.out.println("      sed 's/^[ \t]*.://g' > /sbin/init.d/" + serviceFile.getName() +".tmp");
            System.out.println("    $ mv /sbin/init.d/" + serviceFile.getName() +".tmp /sbin/init.d/" + serviceFile.getName());
            System.out.println("    $ chmod +x /sbin/init.d/" + serviceFile.getName());
            System.out.println("    $ rm -f /sbin/init.d/" + serviceFile.getName() +".tmp");
            System.out.println("    $ echo CONTROL_VARIABLE_KARAF=1 > /etc/rc.config.d/" + serviceFile.getName());
            System.out.println();
            System.out.println("  To start the service when the machine is rebooted for multi-user run level");
            System.out.println("  and stopped for the halt, single-user and reboot runlevels:");
            System.out.println("    $ ln -s /sbin/init.d/" + serviceFile.getName() + " /sbin/rc2.d/K20" + serviceFile.getName());
            System.out.println("    $ ln -s /sbin/init.d/" + serviceFile.getName() + " /sbin/rc3.d/S20" + serviceFile.getName());
            System.out.println();
            System.out.println("    If your application makes use of other services, then you will need to make");
            System.out.println("    sure that your application is started after, and then shutdown before. This");
            System.out.println("    is done by controlling the startup/shutdown order by setting the right order");
            System.out.println("    value, which in this example it set to 20.");
            System.out.println();
            System.out.println("  To start the service:");
            System.out.println("    $ /sbin/init.d/" + serviceFile.getName() + " start");
            System.out.println();
            System.out.println("  To stop the service:");
            System.out.println("    $ /sbin/init.d/" + serviceFile.getName() + " stop");
            System.out.println();
            System.out.println("  To uninstall the service :");
            System.out.println("    $ rm /sbin/init.d/" + serviceFile.getName());
            System.out.println("    $ rm /sbin/rc2.d/K20" + serviceFile.getName());
            System.out.println("    $ rm /sbin/rc3.d/S20" + serviceFile.getName());
        }

        return null;
    }
}
