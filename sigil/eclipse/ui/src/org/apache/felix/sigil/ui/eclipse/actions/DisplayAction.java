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

package org.apache.felix.sigil.ui.eclipse.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;


public abstract class DisplayAction extends Action
{

    public DisplayAction()
    {
        super();
    }


    public DisplayAction( String text )
    {
        super( text );
    }


    public DisplayAction( String text, ImageDescriptor image )
    {
        super( text, image );
    }


    public DisplayAction( String text, int style )
    {
        super( text, style );
    }


    protected Display findDisplay()
    {
        Display d = Display.getCurrent();

        if ( d == null )
        {
            d = Display.getDefault();
        }

        return d;
    }


    protected void runInUI( final Shell shell, final WorkspaceModifyOperation op )
    {
    }


    protected void info( final Shell shell, final String msg )
    {
        shell.getDisplay().asyncExec( new Runnable()
        {
            public void run()
            {
                MessageDialog.openInformation( shell, "Information", msg );
            }
        } );
    }

}