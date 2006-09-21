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

public class Logger extends NotificationBroadcasterSupport implements LogListener,BundleActivator,ServiceListener, LoggerMBean, Serializable{

  private static final String REMOTE_LOGGER_ON_STRING="OSGI:name=Remote Logger";

  private String version=null;
  
  private LogReaderService lrs=null;
  private BundleContext bc=null;

  private Object logMutex=new Object();

  private boolean debugLogFlag=true;
  private boolean errorLogFlag=true;
  private boolean infoLogFlag=true;
  private boolean warningLogFlag=true;

  private MBeanServer agent=null;
  private ObjectName remoteLoggerON=null;

//ServiceListener Interface
  public void serviceChanged(ServiceEvent serviceevent) {
    ServiceReference servicereference= serviceevent.getServiceReference();
    String as[]=(String[])servicereference.getProperty("objectClass");
    switch (serviceevent.getType()) {
      case ServiceEvent.REGISTERED :
        if (as[0].equals(LogReaderService.class.getName())){
          this.registerLogReaderService(servicereference);
        }else if (as[0].equals(MBeanServer.class.getName())){
          this.registerToAgent(servicereference);
        }
	break;
      case ServiceEvent.UNREGISTERING :
        if (as[0].equals(LogReaderService.class.getName())){
				  this.unRegisterLogReaderService(servicereference);
        }else if (as[0].equals(MBeanServer.class.getName())){
          this.unRegisterFromAgent();
        }
	break;
    }
  }


//LogListener Interface
  public void logged(LogEntry log){
    String reg=new String(":");
    StringBuffer message=new StringBuffer();
    //System.out.print("mosgi.jmx.remotelogger : new log : ");

    synchronized (logMutex){
      try{
        message.append(""+log.getBundle().getBundleId());
      }catch(NullPointerException e){
        message.append("Unknown source");
      }
     
      String lSymbolicName=log.getBundle().getSymbolicName();
      if(lSymbolicName != null){
        message.append(reg+lSymbolicName);
      }else {
        message.append(reg+"\"null\"");
      }

      message.append(reg+log.getBundle().getState());

      int lLevel=log.getLevel();
      if(debugLogFlag && lLevel==LogService.LOG_DEBUG){
        message.append(reg+"DEBUG  ");
      }else if (errorLogFlag && lLevel==LogService.LOG_ERROR){
        message.append(reg+"ERROR  ");
      }else if(infoLogFlag && lLevel==LogService.LOG_INFO){
        message.append(reg+"INFO   ");
      }else if(warningLogFlag && lLevel==LogService.LOG_WARNING){
        message.append(reg+"WARNING");
      }else {
        message.append(reg+"NOLEVEL");
      }

      message.append(reg+log.getMessage());
    }
    //System.out.println(message.toString());
    if (this.agent!=null){ // On envoie tous les logs a un MBeanServer
      //System.out.println("this.agent != null => remoteLogger.Logger.sendNotifiaction(...."+message.toString());
      this.sendNotification(new  AttributeChangeNotification(this.remoteLoggerON, 0, 
						             System.currentTimeMillis(),
						             message.toString(), null, "Log", null, null));
    }
  }

//BundleActivator Interface
  public void start(BundleContext bc) throws Exception{
		this.version=(String)bc.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
    this.bc=bc;
		this.log(LogService.LOG_INFO, "Remote Logger starting "+version);
    try{
      this.remoteLoggerON=new ObjectName(Logger.REMOTE_LOGGER_ON_STRING);
    }catch(MalformedObjectNameException e){
      throw new BundleException("Logger.Logger:objectName invalid", e);
    }
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

    ServiceReference sr2=bc.getServiceReference(MBeanServer.class.getName());
    if (sr2!=null){
      this.registerToAgent(sr2);
    }
    this.log(LogService.LOG_INFO, "Remote Logger started "+version);

  }
     
  public void stop(BundleContext bc) throws Exception{
	this.log(LogService.LOG_INFO, "Stopping remote Logger "+version);
   if (this.lrs==null){
      System.out.println("ERROR : Logger.stop : there is no logger or reader to stop");
   } else {
     this.lrs.removeLogListener(this);
     this.bc.removeServiceListener(this);
   }
   if (this.agent!=null){
     this.unRegisterFromAgent();
   }
   this.agent=null;
   this.lrs=null; 
   this.log(LogService.LOG_INFO, "Remote Logger stopped"+version);
   this.bc=null;
 }

//private methods 
  private void registerLogReaderService(ServiceReference sr) {
    //System.out.println("mosgi.jmx.remoteLogger.Logger.registerLogReaderService("+sr.toString()+") : oldLog=");
    this.lrs=(LogReaderService)this.bc.getService(sr);
    this.lrs.addLogListener(this);
   
    /*// old log :
    System.out.println("oldLog=");
    java.util.Enumeration oldLog = this.lrs.getLog();
    int i=0;
    while(oldLog.hasMoreElements()) {
      LogEntry oldLogEntry = ((LogEntry) oldLog.nextElement());
      System.out.println("   -"+(i++)+" : "+oldLogEntry.getMessage());
      //logged(oldLogEntry);
    }*/
  }
  
  private void unRegisterLogReaderService(ServiceReference sr) {
    if (sr!=null){
      this.lrs.removeLogListener(this);
      this.lrs=null;
    }
  }

  private void registerToAgent(ServiceReference sr){
    this.agent=(MBeanServer)bc.getService(sr);
    try{   
      this.agent.registerMBean(this, this.remoteLoggerON); 
      /*// old log :
      System.out.println("oldLog=");
      java.util.Enumeration oldLog = this.lrs.getLog();
      int i=0;
      while(oldLog.hasMoreElements()) {
        LogEntry oldLogEntry = ((LogEntry) oldLog.nextElement());
        System.out.println("   -"+(i++)+" : "+oldLogEntry.getMessage());
        //logged(oldLogEntry);
      }*/
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  private void unRegisterFromAgent(){
    try{
      this.agent.unregisterMBean(this.remoteLoggerON);
    }catch(Exception e){
      //e.printStackTrace();
    }
  }

  private void log (int level, String message){
    ServiceReference lsn=bc.getServiceReference(LogService.class.getName());
    if (lsn!=null){
      LogService ls=(LogService)bc.getService(lsn);
      ls.log(LogService.LOG_INFO, message);
    }else{
      System.out.println("ERROR : Logger.start : No service "+LogService.class.getName()+" is present");
    }
  }


} 
