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


import org.apache.felix.sigil.eclipse.preferences.PromptablePreference;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;


public class OptionalPrompt
{
    public static boolean optionallyPrompt( IPreferenceStore prefStore, String prefName, String title, String text,
        Shell parentShell )
    {
        boolean result = false;

        PromptablePreference value = PromptablePreference.valueOf( prefStore.getString( prefName ) );
        switch ( value )
        {
            case Always:
                result = true;
                break;
            case Never:
                result = false;
                break;
            case Prompt:
                MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion( parentShell, title, text,
                    "Do not ask this again", false, null, null );
                result = ( dialog.getReturnCode() == IDialogConstants.YES_ID );
                if ( dialog.getToggleState() )
                {
                    // User said don't ask again... take the current answer as the new preference
                    prefStore.setValue( prefName, result ? PromptablePreference.Always.name()
                        : PromptablePreference.Never.name() );
                }
        }

        return result;
    }


    public static int optionallyPromptWithCancel( IPreferenceStore prefStore, String prefName, String title,
        String text, Shell parentShell )
    {
        int result = IDialogConstants.NO_ID;

        PromptablePreference value = PromptablePreference.valueOf( prefStore.getString( prefName ) );
        switch ( value )
        {
            case Always:
                result = IDialogConstants.YES_ID;
                break;
            case Never:
                result = IDialogConstants.NO_ID;
                break;
            case Prompt:
                MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoCancelQuestion( parentShell, title,
                    text, "Do not ask this again", false, null, null );
                result = dialog.getReturnCode();
                if ( result != IDialogConstants.CANCEL_ID )
                {
                    if ( dialog.getToggleState() )
                    {
                        // User said don't ask again... take the current answer as the new preference
                        prefStore.setValue( prefName,
                            ( result == IDialogConstants.YES_ID ) ? PromptablePreference.Always.name()
                                : PromptablePreference.Never.name() );
                    }
                }
        }

        return result;
    }
}
