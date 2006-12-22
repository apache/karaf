To start MOSGi framework, 
launch core.sh and jmxconsole.sh in two separate terminals.

---------------
Properties List
---------------
** Core profile Properties **

-> mosgi.jmxconsole.tab.url.<mbeanname>tab : indicates an URL where the jmx console tab should be found
      example : mosgi.jmxconsole.tab.url.osgiprobestab=file:../org.apache.felix.mosgi.managedelements.osgiprobes.tab-0.8.0-SNAPSHOT.jar
-> mosgi.jmxconsole.rmiport.<profile> : indicates a registry port for a specif profile (defaults to 1099)
      example : mosgi.jmxconsole.rmiport.t1=1100


** JmxConsole Properties **
-> mosgi.jmxconsole.rmiport.<profile> : indicates the registry port to contact for a specif profile (defaults to 1099)
      example : mosgi.jmxconsole.rmiport.t1=1100
-> mosgi.jmxconsole.ip<x> : indicates the ip address for the X th. gateway in the list
      example : mosgi.jmxconsole.ip1=127.0.0.1
-> mosgi.jmxconsole.profile<x> : indicates the profile name for the X th.  gateway in the list
      example : mosgi.jmxconsole.profile1=test

** Remark : if someone wants to administer 3 gateways, he needs to specify the following properties.

mosgi.jmxconsole.ip1=192.168.0.10
mosgi.jmxconsole.profile1=test1
mosgi.jmxconsole.ip2=192.168.0.11
mosgi.jmxconsole.profile1=test2
mosgi.jmxconsole.ip3=192.168.0.12
mosgi.jmxconsole.profile1=test3






