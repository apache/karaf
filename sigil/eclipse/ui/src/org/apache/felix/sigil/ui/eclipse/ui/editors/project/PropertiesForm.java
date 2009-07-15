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

package org.apache.felix.sigil.ui.eclipse.ui.editors.project;


import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.IPropertiesFilePartitions;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.preference.IPreferenceStore;

@SuppressWarnings("restriction")
public class PropertiesForm extends SigilSourcePage {

    public static final String PAGE_ID = "properties";
    
	public PropertiesForm(SigilProjectEditorPart editor, ISigilProjectModel project) {
		super(PAGE_ID);
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		setPreferenceStore(store);
		setSourceViewerConfiguration(new PropertiesFileSourceViewerConfiguration(textTools.getColorManager(), store, this, IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING));
        /*IFileEditorInput fileInput = (IFileEditorInput) editor.getEditorInput();
		this.setDocumentProvider(fileInput);*/
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}	
}
