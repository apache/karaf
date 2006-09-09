JMX introspector is a small library bundle that 
uses the Javassist library, the reflection API and 
the metadata provided by JMX on the managed objects 
to dynamically proxy remote MBeans, without having 
the classes in your classpath. 
It can be used to create management consoles that do not
need to have the remote mbeans classes in its classpath to 
use dynamic proxies.

It is used by the org.apache.felix.mishell project to create management clients 
for felix jmood, but it can be used to manage any JMX agent.

It currently uses Javassist version 3.3 for the generation
of the interface classes, which is part of JBoss and 
can be downloaded from http://www.jboss.org
or directly from https://sourceforge.net/project/showfiles.php?group_id=22866&package_id=80766
Javassist is licensed under the Mozilla Public License and the LGPL. 
IMPORTANT: 
You need to install Javassist manually to your local maven repository
in order to build this bundle, as it is not available at the repositories yet:

mvn install:install-file -Dfile=<path-to-file> -DgroupId=javassist \
    -DartifactId=javassist -Dversion=3.3 -Dpackaging=jar

//TODO


