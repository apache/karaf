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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.framework.Felix;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
public class FelixLauncher {

    private Map props;
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
        
		bundles=new ArrayList();
		packages=new ArrayList();

        props = new HashMap();
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
	public void start() throws BundleException {
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

        framework = new Felix(props, null);
        framework.start();
	}
	public void blockingStart() throws Exception{
		this.start();
        int to=0;
        while(framework.getState()!=Bundle.ACTIVE) {
            Thread.sleep(10);
            to++;
            if(to>100) throw new Exception("timeout");
        }

	}
	public void shutdown(){
		framework.stopAndWait();
		framework = null;
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
	public String getFelixBundleUrl(String artifactId) throws IOException{
		return getM2Url("org.apache.felix", artifactId, "0.8.0-SNAPSHOT");
	}
	public String getM2Url(String groupId, String artifactId, String version)throws IOException{
		File userHome=new File(System.getProperty("user.home"));
    	String M2_REPO="file:/"+userHome.getCanonicalPath()+"/.m2/repository";
    	String PROJECT_HOME=M2_REPO+"/"+groupId.replace('.', '/');
    	String VERSION="0.8.0-SNAPSHOT";
    	String u=PROJECT_HOME+"/"+artifactId+"/"+VERSION+"/"+artifactId+"-"+VERSION+".jar";
    	return "\""+u+"\"";

	}

    /**
     * @param args
     * 
     */
    public static void main(String[] args) throws Exception{
    	FelixLauncher launcher=new FelixLauncher();
    	String jmood=launcher.getFelixBundleUrl("org.apache.felix.jmood");
    	String mishell=launcher.getFelixBundleUrl("org.apache.felix.mishell");
    	String jruby=launcher.getM2Url("org.jruby", "jruby-bundle", "0.8.0-SNAPSHOT");
    	launcher.addBundle(jruby);
        launcher.addBundle(jmood);
    	launcher.addBundle(mishell);
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
        launcher.addPackage("javax.management.openmbean");  
        launcher.addPackage("javax.script");
    	launcher.start();
    }
	public Felix getFramework() {
		return framework;
	}
}
