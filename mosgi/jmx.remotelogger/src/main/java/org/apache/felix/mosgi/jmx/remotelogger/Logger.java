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
package org.apache.felix.mosgi.jmx.remotelogger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Constants;

import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.service.log.LogEntry;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.NotificationBroadcasterSupport;
import javax.management.AttributeChangeNotification;
import javax.management.MalformedObjectNameException;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

public class Logger extends NotificationBroadcasterSupport implements LogListener,BundleActivator,ServiceListener, LoggerMBean, Serializable{

  private static final String REMOTE_LOGGER_ON_STRING="OSGI:name=Remote Logger";
  private ServiceRegistration mbean_sr=null;

  private String version=null;
  
  private LogReaderService lrs=null;
  private BundleContext bc=null;

  private Object logMutex=new Object();

  private static final String[] LOG_LVL=new String[] {"","ERROR  ","WARNING ","INFO   ","DEBUG  "};
  private Integer logLvl=new Integer(4);

  private ObjectName remoteLoggerON=null;

  /////////////////////////////////
  //     LogerMBean Interface    //
  /////////////////////////////////
  public void sendOldLog(){
    System.out.println("[remoteLogger.Logger] send old log");
    Enumeration oldLog = this.lrs.getLog();
    Vector invert=new Vector();
    while(oldLog.hasMoreElements()) {
      LogEntry le=(LogEntry) (oldLog.nextElement());
      invert.insertElementAt(le,0);
    }
    for (int i=0 ; i<invert.size() ; i++) {
      logged( (LogEntry) invert.elementAt(i), true );
    }
  }

  public void setLogLvl(Integer lvl) {
    this.logLvl=lvl;
    String logLvlToString=(new String[] {"Error", "Warning", "Info", "Debug"})[lvl.intValue()-1];
    this.log(LogService.LOG_INFO, "Log level is now \""+logLvlToString+"\".");
    System.out.println("[Logger] : modification of log lvl : logLvl="+logLvl+" ("+logLvlToString+")");
  }

  public Integer getLogLvl() {
    return logLvl;
  }

  ///////////////////////////////////////
  //      ServiceListener Interface    //
  ///////////////////////////////////////
  public void serviceChanged(ServiceEvent serviceevent) {
    ServiceReference servicereference= serviceevent.getServiceReference();
    String as[]=(String[])servicereference.getProperty("objectClass");
    switch (serviceevent.getType()) {
      case ServiceEvent.REGISTERED :
        if (as[0].equals(LogReaderService.class.getName())){
          this.registerLogReaderService(servicereference);
        }
	break;
      case ServiceEvent.UNREGISTERING :
        if (as[0].equals(LogReaderService.class.getName())){
	  this.unRegisterLogReaderService(servicereference);
        }
	break;
    }
  }

  /////////////////////////////////////////
  //      LogListener Interface          //
  /////////////////////////////////////////
  public void logged(LogEntry log){
    logged(log, false);
  }

  public void logged(LogEntry log, boolean oldLog){
    synchronized (logMutex){
      if (log.getLevel() <= logLvl.intValue() ) {
        String reg=new String("*");
        StringBuffer message=new StringBuffer();
        try{
          long id=log.getBundle().getBundleId();
	  message.append( ((id<10)?" ":"")+id );
        } catch(NullPointerException e){
          message.append("Unknown source");
        }
     
        String lSymbolicName=log.getBundle().getSymbolicName();
        message.append( reg+ ((lSymbolicName!=null)?lSymbolicName:"\"null\"") );

        message.append(reg+log.getBundle().getState());

	message.append(reg+LOG_LVL[log.getLevel()]);
	 
        // into log.getMessage() replaceAll regex char by an other
        String msg=log.getMessage();
        message.append( reg+ ((msg!=null)?msg.replace('*','X'):"\"null\"") );

        this.sendNotification(new  AttributeChangeNotification(this.remoteLoggerON, 0, (oldLog)?0:System.currentTimeMillis(), message.toString(), null, "Log", null, null));
      }
    }
  }

  //////////////////////////////////
  //  BundleActivator Interface   //
  //////////////////////////////////
  public void start(BundleContext bc) throws Exception{
    this.version=(String)bc.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
    this.bc=bc;
    this.log(LogService.LOG_INFO, "Remote Logger starting "+version);

    java.util.Properties p = new java.util.Properties();
    try {
      this.remoteLoggerON = new ObjectName(Logger.REMOTE_LOGGER_ON_STRING);
    } catch (MalformedObjectNameException mne) {
      throw new BundleException("Logger.Logger:objectName invalid", mne);
    }
    p.put(org.apache.felix.mosgi.jmx.agent.Constants.OBJECTNAME, REMOTE_LOGGER_ON_STRING);
    this.mbean_sr = this.bc.registerService(LoggerMBean.class.getName(), this, p);

    try{
      bc.addServiceListener(this,"(|(objectClass="+LogReaderService.class.getName()+")"+
                                   "(objectClass="+MBeanServer.class.getName()+"))");
    }catch(InvalidSyntaxException e){
      throw new BundleException("Logger.Logger:filtre LDAP", e);
    }
    ServiceReference sr=bc.getServiceReference(LogReaderService.class.getName());
    if (sr!=null){
      this.registerLogReaderService(sr);
    }

    this.log(LogService.LOG_INFO, "Remote Logger started (logLvl="+logLvl+")"+version);
  }
     
  public void stop(BundleContext bc) throws Exception{
    this.log(LogService.LOG_INFO, "Stopping remote Logger "+version);
    if (this.lrs==null){
       System.out.println("ERROR : Logger.stop : there is no logger or reader to stop");
    } else {
      this.lrs.removeLogListener(this);
      this.bc.removeServiceListener(this);
    }
    this.lrs=null; 
    this.log(LogService.LOG_INFO, "Remote Logger stopped"+version);
    this.mbean_sr.unregister();
    this.mbean_sr=null;
    this.bc=null;
  }

  //////////////////////////////
  //     private methods      //
  //////////////////////////////
  private void registerLogReaderService(ServiceReference sr) {
    //System.out.println("mosgi.jmx.remoteLogger.Logger.registerLogReaderService("+sr.toString()+") : oldLog=");
    this.lrs=(LogReaderService)this.bc.getService(sr);
    this.lrs.addLogListener(this);
  }
  
  private void unRegisterLogReaderService(ServiceReference sr) {
    if (sr!=null){
      this.lrs.removeLogListener(this);
      this.lrs=null;
    }
  }

  private void log (int level, String message){
    ServiceReference lsn=bc.getServiceReference(LogService.class.getName());
    if (lsn!=null){
      LogService ls=(LogService)bc.getService(lsn);
      ls.log(level, message);
    }else{
      System.out.println("ERROR : Logger.start : No service "+LogService.class.getName()+" is present");
    }
  }

} 
