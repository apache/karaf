Mishell provides an interactive console
that executes scripts in different scripting languages.
Running mishell: 
JRE 6 is needed (because of javax.script). Mishell provides some built-in commands and interprets ruby, javascript or any other language that you
configure. You can also load scripts with the load command. Type 'help' for available commands. 
You can see an example of configuring Felix for launching both Jmood and mishell in the same OSGi platform in the FelixLauncher
class in the src/test/java dir. Remember to change the paths to match your installation. 

The initial object that is exported to the scripting engine is a JMoodProxyManager that extends 
the general-purpose MBeanProxyManager (from the jmxintrospector project) to simplify working with JMood. 
For example, when running on OSGi with JMood you can add the mbeans by typing:
$manager.addLocalServer(nil) #Ruby NOTE: This has been fixed in latest versions and should be fixed
or
manager.addLocalServer(null) //Javascript
And you issue commands like
$manager.objects.each{|mbean| puts mbean.objectName} #this lists the objectnames for all mbeans known to the shell




IMPORTANT: 
Dependencies that need to be manually installed:
1. It needs Java 6 to work (as it depends on javax.script API). 
Once that API is stable and released standalone, it should also work in Java 5.
2. It needs JMX introspector.
3. It needs classes from com.sun.jruby.* and from org.jruby. The easiest way is to bundle both together
and export both packages in order to run ruby. 
	- The binding is available at https://scripting.dev.java.net/ and licensed
	under the BSD license. Download the jsr223-engines.[zip|tar.gz]
	and install the  jruby-engine.jar
	- JRuby is available at http://dist.codehaus.org/jruby/ under a tri-license: CPL/LGPL/GPL
	located at engines/jruby/build/jruby-engine.jar. JRuby implementation
4. If you want to use any other language, you should: 
	- Create and install a bundle (or more) that contain the necessary classes and export the packages. 
	

