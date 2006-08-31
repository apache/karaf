package org.apache.felix.jmood;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.MutablePropertyResolver;
import org.apache.felix.framework.util.MutablePropertyResolverImpl;
import org.osgi.framework.Constants;
public class FelixLauncher {

    private MutablePropertyResolver props;
	private Felix framework;
	private List bundles;
	private List packages;
	private File cacheDir;
	public FelixLauncher() {
        super();
    	cacheDir = new File("./cache");
    	System.out.println(cacheDir.getAbsolutePath());
		clearCache(cacheDir);
    	cacheDir.mkdir();
        
        framework = new Felix();
		Map m=new HashMap();
		bundles=new ArrayList();
		packages=new ArrayList();

        props = new MutablePropertyResolverImpl(m);
        props.put("felix.cache.profiledir", cacheDir.getAbsolutePath());

    }
	public void addBundle(String url){
		if (!bundles.contains(url))
		bundles.add(url);
	}
	public void addPackage(String packageName){
		if(!packages.contains(packageName))
		packages.add(packageName);
	}
	public void start(){
		StringBuffer autostart=new StringBuffer();
		for (int i=0; i<bundles.size(); i++){
			String bundle=(String)bundles.get(i);
			autostart.append(bundle).append(" ");
		}
		props.put("felix.auto.start.1", autostart.toString());
		StringBuffer spkg=new StringBuffer((String)packages.get(0));
		packages.remove(0);
		for (int i=0; i<packages.size(); i++){
			String pkg=(String)packages.get(i);
			spkg.append(", "+pkg);
		}
		
		
        props.put(Constants.FRAMEWORK_SYSTEMPACKAGES, spkg.toString());

        framework.start(props,null);
	}
	public void blockingStart() throws Exception{
		this.start();
        int to=0;
        while(framework.getStatus()!=Felix.RUNNING_STATUS) {
            Thread.sleep(10);
            to++;
            if(to>100) throw new Exception("timeout");
        }

	}
	public void shutdown(){
		framework.shutdown();
		clearCache(cacheDir);
	}

	private void clearCache(File cacheDir) {
		if(!isCache(cacheDir)){
			System.out.println("not valid cache");
			return;
		}
		File[] files=cacheDir.listFiles();
		for(int i=0; i<files.length; i++){
			recursiveDelete(files[i]);
		}
	}
	private void recursiveDelete(File file){
	   if(file.isDirectory()){
			File[] files=file.listFiles();
			for(int i=0; i<files.length; i++){
				File f=files[i];	
				recursiveDelete(f);
		   }
	   }
	   file.delete();
	}
	private boolean isCache(File cacheDir){
		if(!cacheDir.exists()||!cacheDir.isDirectory()) return false;
		else{
			String[] names=cacheDir.list();
			for(int i=0;i<names.length;i++){
				String name=names[i];
				if(!name.startsWith("bundle")) return false;
			}
			return true;
		}
	}

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
    	FelixLauncher launcher=new FelixLauncher();
        String jmood="file:target/org.apache.felix.jmood-0.8.0-SNAPSHOT.jar";
        String jmxintrospector="file:../org.apache.felix.mishell/target/org.apache.felix.mishell-0.8.0-SNAPSHOT.jar";
    	launcher.addBundle(jmood);
    	launcher.addBundle(jmxintrospector);
        launcher.addPackage("org.osgi.framework");
        launcher.addPackage("org.osgi.util.tracker");
        launcher.addPackage("org.osgi.service.log");
        launcher.addPackage("org.osgi.service.packageadmin");
        launcher.addPackage("org.osgi.service.startlevel");
        launcher.addPackage("org.osgi.service.permissionadmin");
        launcher.addPackage("org.osgi.service.useradmin");
        launcher.addPackage("org.osgi.service.cm");
        launcher.addPackage("javax.management");
        launcher.addPackage("javax.management.remote");
        launcher.addPackage("javax.script");
    	launcher.start();
    }
	public Felix getFramework() {
		return framework;
	}
}
