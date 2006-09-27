JMX introspector is a small library bundle that 
uses the CGLIB library, the reflection API and 
the metadata provided by JMX on the managed objects 
to dynamically proxy remote MBeans, without having 
the classes in your classpath. 
It can be used to create management consoles that do not
need to have the remote mbeans classes in its classpath to 
use dynamic proxies.

It is used by the org.apache.felix.mishell project to create management clients 
for felix jmood, but it can be used to manage any JMX agent.

It currently uses CGLIB 2.1_3 for the generation
of the interface classes. CGLIB is released under the ASL and can be downloaded
from: 
http://cglib.sourceforge.net/


