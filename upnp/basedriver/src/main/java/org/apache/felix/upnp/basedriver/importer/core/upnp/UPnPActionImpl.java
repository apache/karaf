/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.upnp.basedriver.importer.core.upnp;


import java.util.Dictionary;
import java.util.Properties;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.UPnPStatus;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPException;
import org.osgi.service.upnp.UPnPStateVariable;

import org.apache.felix.upnp.basedriver.util.Converter;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class UPnPActionImpl implements UPnPAction {

	private Action act;	
	private UPnPServiceImpl actsFather;
	/**
	 * @param act
	 */
	public UPnPActionImpl(Action act,UPnPServiceImpl ser) {
		// TODO to check
		this.act=act;
		actsFather=ser;	
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getName()
	 */
	public String getName() {
		// TODO to check
		return act.getName();
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getReturnArgumentName()
	 */
	public String getReturnArgumentName() {
	/*	 TODO should I to parse again the xml?
		 */
		
		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getInputArgumentNames()
	 */
	public String[] getInputArgumentNames() {
		// TODO to check
		ArgumentList argsList=act.getInputArgumentList();
		if(argsList.size()==0){
			return null;
		}
		String [] inputArgs=new String[argsList.size()]; 
		for(int i=0;i<argsList.size();i++){
			inputArgs[i]=argsList.getArgument(i).getName();
		}
		return inputArgs;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getOutputArgumentNames()
	 */
	public String[] getOutputArgumentNames() {
		// TODO to check
		ArgumentList argsList=act.getOutputArgumentList();
		if(argsList.size()==0){
			return null;
		}
		String [] outArgs=new String[argsList.size()]; 
		for(int i=0;i<argsList.size();i++){
			outArgs[i]=argsList.getArgument(i).getName();
		}
		return outArgs;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getStateVariable(java.lang.String)
	 */
	public UPnPStateVariable getStateVariable(String argumentName) {
		/*
		
		check if the way to obtain the UPnPStateVariabile is not too bad
		
		*/
		Argument arg=act.getArgument(argumentName);
		if(arg==null){
			return null;
		}
		String varName=arg.getRelatedStateVariableName();
		return actsFather.getStateVariable(varName);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#invoke(java.util.Dictionary)
	 */
	public Dictionary invoke(Dictionary args) throws Exception {
		/*TODO 
		check if I have understood wath this method should do
		*/
		/*
		 * I look for argument and value and then I add them to ArgumentList
		 */
		ArgumentList argsList=new ArgumentList();
		argsList= act.getInputArgumentList();
	
		for(int i=0;i<argsList.size();i++){
			/*			
			 * I assert that .getArgument(i) will return to me an Argument with only the name of the
			 * Argument and not it's value. I'll set the associated value by myself and
			 * Also I assert that the Argument are ordered
			 */
			Argument argument=argsList.getArgument(i);
			String argumentName=argument.getName();
			//String relateVar=argument.getRelatedStateVariableName();
			UPnPStateVariable stateVar=this.getStateVariable(argumentName);
			String upnpType=stateVar.getUPnPDataType();
			/*Class javaClass=stateVar.getJavaDataType();*/
			//setting the value related to the input argument
			argument.setValue(Converter.toString(args.get(argumentName),upnpType));
		}
		act.setInArgumentValues(argsList); 
		if(act.postControlAction()==true){
			//TODO check what happen if I don't have any output argument
			Properties outDic=new Properties();
			ArgumentList outArgs=act.getOutputArgumentList();
			if(outArgs.size()==0){
				return null;
			}
			for(int i=0;i<outArgs.size();i++){
				Argument argument=outArgs.getArgument(i);
				String argumentName=outArgs.getArgument(i).getName();
				//String relateVar=argument.getRelatedStateVariableName();
				UPnPStateVariable stateVar=getStateVariable(argumentName);
				//String javaType=stateVar.getJavaDataType().getName(); 
				//TODO rember to catch number exception
				String upnpType=stateVar.getUPnPDataType();
				outDic.put(argumentName,Converter.parseString(argument.getValue(),upnpType));
			}
			return outDic;
		}else{
            UPnPStatus controlStatus = act.getControlStatus();
            throw new UPnPException(controlStatus.getCode(),controlStatus.getDescription());
		}

	}

}
