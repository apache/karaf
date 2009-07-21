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

package org.apache.felix.sigil.eclipse.cheatsheets.actions;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;


public abstract class AbstractNewWizardAction extends Action
{

    @Override
    public void run()
    {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

        try
        {
            INewWizard wizard = createWizard();
            wizard.init( PlatformUI.getWorkbench(), getSelection() );
            WizardDialog dialog = new WizardDialog( shell, wizard );
            int res = dialog.open();
            notifyResult( res == Window.OK );
        }
        catch ( CoreException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    protected abstract INewWizard createWizard() throws CoreException;


    private IStructuredSelection getSelection()
    {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if ( window != null )
        {
            ISelection selection = window.getSelectionService().getSelection();
            if ( selection instanceof IStructuredSelection )
            {
                return ( IStructuredSelection ) selection;
            }
        }
        return StructuredSelection.EMPTY;
    }
}