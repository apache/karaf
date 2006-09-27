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

package org.apache.felix.jmood;


import org.apache.felix.jmood.core.ServiceNotAvailableException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;


/**
 * 
 *
 */public class AgentContext {
    private BundleContext context;
    private ServiceTracker logTracker;
    private ServiceTracker logReaderTracker;
    private ServiceTracker startLevelTracker;
    private ServiceTracker packageAdminTracker;
    private ServiceTracker permissionAdminTracker;
    private ServiceTracker userAdminTracker;
    private ServiceTracker configAdminTracker;
    
    private int loglevel;
    public static final int DEBUG=0;
    public static final int INFO=1;
    public static final int WARNING=2;
    public static final int ERROR=3;
    public AgentContext(BundleContext context) throws Exception{
        super();
        this.context=context;
        this.setTrackers();
    }
    
    ///////////////////////////////////////////////////////////////
    //////////////////LOGGING/////////////////////////////////////
    ///////////////////////////////////////////////////////////////
    public void debug(String s){
        if(this.loglevel==DEBUG) System.out.println("DEBUG: JMOOD. "+s);
    }
    public void info(String s){
        if(this.loglevel<=INFO) System.out.println("INFO: JMOOD. "+s);
    }
    public void warning (String s){
        if(this.loglevel<=WARNING) System.out.println("WARNING: JMOOD. "+s);
    }
    public void error(String s){
        if(this.loglevel<=ERROR) System.out.println("ERROR: JMOOD. "+s);
    }
    public void error(String s, Exception e){
        if(this.loglevel<=ERROR) {
        	System.out.println("ERROR: JMOOD. "+s);
        	e.printStackTrace();
        }
    }
    public int getLoglevel() {
        return loglevel;
    }
    public void setLoglevel(int level) {
        this.loglevel=level;
    }
    //////////////////////////////////////////////////////////////
    ///////////////////CONTEXT AND SERVICES//////////////////////
    public BundleContext getBundleContext() {
        return context;
    }
    public LogService getLogservice() {
        int count = logTracker.getTrackingCount();
        switch (count) {
            case 0 :
                return null;
                //FUTURE WORK when there is more than one log service available, select "the best"
            case 1 :
            default :
                return (LogService) logTracker.getService();
        }
    }
    public StartLevel getStartLevel() throws ServiceNotAvailableException {
        int count = startLevelTracker.getTrackingCount();
        switch (count) {
            case 0 :
                throw new ServiceNotAvailableException("No start level service available");
            case 1 :
            default :
                return (StartLevel) startLevelTracker.getService();
        }
    }
    public PackageAdmin getPackageadmin() throws ServiceNotAvailableException{
        int count = packageAdminTracker.getTrackingCount();
        switch (count) {
            case 0 :
                throw new ServiceNotAvailableException("No package admin available");
            case 1 :
            default :
                return (PackageAdmin) packageAdminTracker.getService();
        }

    }
    public PermissionAdmin getPermissionadmin() {
        int count = permissionAdminTracker.getTrackingCount();
        switch (count) {
            case 0 :
                return null;
            case 1 :
            default :
                return (PermissionAdmin) permissionAdminTracker.getService();
        }
    }
    public UserAdmin getUserAdmin() {
        int count = userAdminTracker.getTrackingCount();
        switch (count) {
            case 0 :
                return null;
            case 1 :
            default :
                return (UserAdmin) userAdminTracker.getService();
        }
    }
    public ConfigurationAdmin getConfigurationAdmin() {
        int count = configAdminTracker.getTrackingCount();
        switch (count) {
            case 0 :
                return null;
            case 1 :
            default :
                return (ConfigurationAdmin) configAdminTracker.getService();
        }
    }
    //////////////////////////////////////////////////////////////////////
    //////////////////PRIVATE////////////////////////////////////////////
    private void setTrackers() {
        try {
            logTracker =
                new ServiceTracker(
                    context,
                    context.createFilter(
                        "(objectClass=" + LogService.class.getName() + ")"),
                    null);
        logReaderTracker =
            new ServiceTracker(
                context,
                context.createFilter(
                    "(objectClass=" + LogReaderService.class.getName() + ")"),
                null);
        startLevelTracker =
            new ServiceTracker(
                context,
                context.createFilter(
                    "(objectClass=" + StartLevel.class.getName() + ")"),
                null);
        packageAdminTracker =
            new ServiceTracker(
                context,
                context.createFilter(
                    "(objectClass=" + PackageAdmin.class.getName() + ")"),
                null);
        permissionAdminTracker =
            new ServiceTracker(
                context,
                context.createFilter(
                    "(objectClass=" + PermissionAdmin.class.getName() + ")"),
                null);
        userAdminTracker=new ServiceTracker(context, context.createFilter("(objectClass=" + UserAdmin.class.getName() + ")"), null);
        configAdminTracker=new ServiceTracker(context, context.createFilter("(objectClass=" + ConfigurationAdmin.class.getName() + ")"), null);

        } catch (InvalidSyntaxException e) {
            warning("INVALID FILTER ");
        }
        


        logTracker.open();
        logReaderTracker.open();
        startLevelTracker.open();
        packageAdminTracker.open();
        permissionAdminTracker.open();
        userAdminTracker.open();
        configAdminTracker.open();
    }
    void closeTrackers() {
        logTracker.close();
        logReaderTracker.close();
        startLevelTracker.close();
        packageAdminTracker.close();
        permissionAdminTracker.close();
        userAdminTracker.close();
        configAdminTracker.close();

        
    }
}
