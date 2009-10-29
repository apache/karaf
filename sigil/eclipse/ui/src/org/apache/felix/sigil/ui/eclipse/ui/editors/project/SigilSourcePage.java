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


import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDE;


public class SigilSourcePage extends TextEditor implements IFormPage
{
    private final String id;
    private int index;
    private SigilProjectEditorPart editor;
    private boolean active;
    private Control control;


    public SigilSourcePage( String id )
    {
        this.id = id;
    }


    @Override
    public void createPartControl( Composite parent )
    {
        super.createPartControl( parent );
        Control[] children = parent.getChildren();
        control = children[children.length - 1];
        getSourceViewer().addTextListener( new ITextListener()
        {
            public void textChanged( TextEvent event )
            {
                if ( editor != null )
                {
                    editor.refreshAllPages();
                }
            }
        } );
        //PlatformUI.getWorkbench().getHelpSystem().setHelp(fControl, IHelpContextIds.MANIFEST_SOURCE_PAGE);
    }


    public void initialize( FormEditor editor )
    {
        this.editor = ( SigilProjectEditorPart ) editor;
    }


    public FormEditor getEditor()
    {
        return editor;
    }


    public String getId()
    {
        return id;
    }


    public int getIndex()
    {
        return index;
    }


    public void setIndex( int index )
    {
        this.index = index;
    }


    public boolean isActive()
    {
        return active;
    }


    public void setActive( boolean active )
    {
        this.active = active;
    }


    public Control getPartControl()
    {
        return control;
    }


    public boolean selectReveal( Object object )
    {
        if ( object instanceof IMarker )
        {
            IDE.gotoMarker( this, ( IMarker ) object );
            return true;
        }
        return false;
    }


    // static impls
    public boolean isEditor()
    {
        return true;
    }


    public boolean canLeaveThePage()
    {
        return true;
    }


    public IManagedForm getManagedForm()
    {
        // this is not a form
        return null;
    }
}
