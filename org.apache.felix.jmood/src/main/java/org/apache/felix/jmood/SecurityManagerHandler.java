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
import java.io.FileOutputStream;
import java.io.InputStream;

public class SecurityManagerHandler {
	private AgentContext ac;
	private boolean policyEmbedded;
	private String policyPath;
    public static final String IS_POLICY_EMBEDDED="policy.embedded";
    public static final String JAVA_SECURITY_POLICY="java.security.policy";

	public SecurityManagerHandler(AgentContext ac, boolean policyEmbedded, String policyPath){
		this.ac=ac;
		this.policyEmbedded=policyEmbedded;
		this.policyPath=policyPath;
	}
    private void setSecurityManager() throws Exception{
        //TODO check this when we add permission admin support to the bundle
        //It caused StackOverFlow the second time the framework was run(?)
    	
        if (System.getSecurityManager() != null) {
			return;
		}
		try {
			this.ac.debug("Security manager does not exist");

            if (policyEmbedded){
                this.ac.debug("Policy is embedded, copying it to filesystem...");
                //The policy is in the file system and should be copied...
                File file=this.ac.getBundleContext().getDataFile(policyPath);
                if (file.exists()) {
                    this.ac.debug("trying to delete file...");
                    boolean deleted=file.delete();
                    if(!deleted) {
						this.ac.error("Could not delete existing policy file");
					} else {
						this.ac.debug("successfully deleted");
					}
                    file=this.ac.getBundleContext().getDataFile(policyPath);
                    file.createNewFile();
                    this.ac.debug("new file created");
                }

                FileOutputStream o=new FileOutputStream (file);
                InputStream i=this.ac.getBundleContext().getBundle().getResource("/"+policyPath).openStream();
                byte [] buffer=new byte [1024];
                while (i.read(buffer)!=-1){
                   o.write(buffer);
                }
                i.close();
                o.flush();
                o.close();
                
                System.setProperty(JAVA_SECURITY_POLICY, file.getAbsolutePath());
            }
            else{
         System.setProperty(JAVA_SECURITY_POLICY, policyPath);
            }
         System.setSecurityManager(new SecurityManager());

        }catch(Exception e){
            this.ac.error("Unexpected exception", e);
            }
        this.ac.debug("Security policy: "+System.getProperty(JAVA_SECURITY_POLICY));
        this.ac.debug("Security manager toString(): "+System.getSecurityManager().toString());

        }


}
