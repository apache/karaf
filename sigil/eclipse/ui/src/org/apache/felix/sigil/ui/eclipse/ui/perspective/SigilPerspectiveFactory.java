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

package org.apache.felix.sigil.ui.eclipse.ui.perspective;


import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.progress.IProgressConstants;


public class SigilPerspectiveFactory implements IPerspectiveFactory
{

    private static final String ID_PROJECT_EXPLORER = "org.eclipse.ui.navigator.ProjectExplorer"; //$NON-NLS-1$
    private static final String ID_SEARCH_VIEW = "org.eclipse.search.ui.views.SearchView"; //$NON-NLS-1$
    private static final String ID_CONSOLE_VIEW = "org.eclipse.ui.console.ConsoleView"; //$NON-NLS-1$

    public void createInitialLayout( IPageLayout layout )
    {
        /*
         * Use ProjectExplorer vs PackageExplorer due to a bug with Drag and Drop on Mac OS X which affects PackageExplorer
         * but not ProjectExplorer. https://bugs.eclipse.org/bugs/show_bug.cgi?id=243529
         */
        String editorArea = layout.getEditorArea();

        IFolderLayout folder = layout.createFolder( "left", IPageLayout.LEFT, ( float ) 0.25, editorArea ); //$NON-NLS-1$
        folder.addView( ID_PROJECT_EXPLORER );
        folder.addView( JavaUI.ID_TYPE_HIERARCHY );
        folder.addPlaceholder( IPageLayout.ID_RES_NAV );
        folder.addPlaceholder( SigilUI.ID_REPOSITORY_VIEW );
        folder.addView( IPageLayout.ID_OUTLINE );

        IFolderLayout outputfolder = layout.createFolder( "bottom", IPageLayout.BOTTOM, ( float ) 0.75, editorArea ); //$NON-NLS-1$
        outputfolder.addView( IPageLayout.ID_PROBLEM_VIEW );
        outputfolder.addView( JavaUI.ID_JAVADOC_VIEW );
        outputfolder.addView( JavaUI.ID_SOURCE_VIEW );
        outputfolder.addPlaceholder( ID_SEARCH_VIEW );
        outputfolder.addPlaceholder( ID_CONSOLE_VIEW );
        outputfolder.addPlaceholder( IPageLayout.ID_BOOKMARKS );
        outputfolder.addPlaceholder( IProgressConstants.PROGRESS_VIEW_ID );
        outputfolder.addPlaceholder( SigilUI.ID_DEPENDENCY_VIEW );

        layout.addActionSet( IDebugUIConstants.LAUNCH_ACTION_SET );
        layout.addActionSet( JavaUI.ID_ACTION_SET );
        layout.addActionSet( JavaUI.ID_ELEMENT_CREATION_ACTION_SET );
        layout.addActionSet( IPageLayout.ID_NAVIGATE_ACTION_SET );

        // views - sigil
        layout.addShowViewShortcut( SigilUI.ID_REPOSITORY_VIEW );
        layout.addShowViewShortcut( SigilUI.ID_DEPENDENCY_VIEW );

        // views - java
        layout.addShowViewShortcut( JavaUI.ID_PACKAGES );
        layout.addShowViewShortcut( JavaUI.ID_TYPE_HIERARCHY );
        layout.addShowViewShortcut( JavaUI.ID_SOURCE_VIEW );
        layout.addShowViewShortcut( JavaUI.ID_JAVADOC_VIEW );

        // views - search
        layout.addShowViewShortcut( ID_SEARCH_VIEW );

        // views - debugging
        layout.addShowViewShortcut( ID_CONSOLE_VIEW );

        // views - standard workbench
        layout.addShowViewShortcut( IPageLayout.ID_OUTLINE );
        layout.addShowViewShortcut( IPageLayout.ID_PROBLEM_VIEW );
        layout.addShowViewShortcut( IPageLayout.ID_RES_NAV );
        layout.addShowViewShortcut( IPageLayout.ID_TASK_LIST );
        layout.addShowViewShortcut( IProgressConstants.PROGRESS_VIEW_ID );
        layout.addShowViewShortcut( ID_PROJECT_EXPLORER );

        // new actions - Java project creation wizard
        layout.addNewWizardShortcut( "org.eclipse.jdt.ui.wizards.NewPackageCreationWizard" ); //$NON-NLS-1$
        layout.addNewWizardShortcut( "org.eclipse.jdt.ui.wizards.NewClassCreationWizard" ); //$NON-NLS-1$
        layout.addNewWizardShortcut( "org.eclipse.jdt.ui.wizards.NewInterfaceCreationWizard" ); //$NON-NLS-1$
        layout.addNewWizardShortcut( "org.eclipse.jdt.ui.wizards.NewEnumCreationWizard" ); //$NON-NLS-1$
        layout.addNewWizardShortcut( "org.eclipse.jdt.ui.wizards.NewAnnotationCreationWizard" ); //$NON-NLS-1$
        layout.addNewWizardShortcut( "org.eclipse.jdt.ui.wizards.NewSourceFolderCreationWizard" ); //$NON-NLS-1$
        layout.addNewWizardShortcut( "org.eclipse.jdt.ui.wizards.NewSnippetFileCreationWizard" ); //$NON-NLS-1$
        layout.addNewWizardShortcut( "org.eclipse.jdt.ui.wizards.NewJavaWorkingSetWizard" ); //$NON-NLS-1$
        layout.addNewWizardShortcut( "org.eclipse.ui.wizards.new.folder" );//$NON-NLS-1$
        layout.addNewWizardShortcut( "org.eclipse.ui.wizards.new.file" );//$NON-NLS-1$
        layout.addNewWizardShortcut( "org.eclipse.ui.editors.wizards.UntitledTextFileWizard" );//$NON-NLS-1$

        layout.addNewWizardShortcut( "org.apache.felix.sigil.editors.newProjectWizard" );
    }

}
