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

package org.apache.felix.sigil.ui.eclipse.ui.preferences;


import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.preferences.PromptablePreference;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


public class SigilPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage
{

    @Override
    protected void createFieldEditors()
    {
        RadioGroupFieldEditor impExpField = new RadioGroupFieldEditor( SigilCore.PREFERENCES_ADD_IMPORT_FOR_EXPORT,
            "Add Imports for New Exports", 1, new String[][]
                { new String[]
                    { "Always (Recommended)", PromptablePreference.Always.toString() }, new String[]
                    { "Prompt", PromptablePreference.Prompt.toString() }, new String[]
                    { "Never", PromptablePreference.Never.toString() } }, getFieldEditorParent(), true );

        addField( impExpField );

        RadioGroupFieldEditor rebuildExpField = new RadioGroupFieldEditor( SigilCore.PREFERENCES_REBUILD_PROJECTS,
            "Rebuild Projects On Install Change", 1, new String[][]
                { new String[]
                    { "Always (Recommended)", PromptablePreference.Always.toString() }, new String[]
                    { "Prompt", PromptablePreference.Prompt.toString() }, new String[]
                    { "Never", PromptablePreference.Never.toString() } }, getFieldEditorParent(), true );

        addField( rebuildExpField );
    }


    @Override
    protected IPreferenceStore doGetPreferenceStore()
    {
        return SigilCore.getDefault().getPreferenceStore();
    }


    public void init( IWorkbench workbench )
    {
    }

}
