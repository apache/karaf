package org.apache.felix.sigil.eclipse.runtime.config;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class FormSelectionDialog extends Dialog
{
    private IFile formFile;
    
    public FormSelectionDialog(Shell parent)
    {
        super(parent);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);
        new Label(composite, SWT.NONE).setText("Select bundle form file from workspace:");
        Tree tree = new Tree(composite, SWT.SINGLE);

        TreeViewer viewer = new TreeViewer(tree);
        viewer.setContentProvider(new BaseWorkbenchContentProvider());
        viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
        viewer.addSelectionChangedListener(new ISelectionChangedListener()
        {        
            public void selectionChanged(SelectionChangedEvent evt)
            {
                if (evt.getSelection().isEmpty()) {
                    updateFile(null);
                }
                else {
                    StructuredSelection sel = (StructuredSelection) evt.getSelection();
                    IResource r = (IResource) sel.getFirstElement();
                    if ( r instanceof IFile ) {
                        IFile f = (IFile) r;
                        updateFile(f);
                    }
                    else {
                        updateFile(null);
                    }
                }
            }
        });
        viewer.setLabelProvider(new WorkbenchLabelProvider());
        
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        return composite;
    }
    
    public IFile getFormFile() {
        return formFile;
    }

    protected void updateFile(IFile file)
    {
        formFile = file;
        
        if ( file == null ) {
            getButton(Window.OK).setEnabled(false);            
        }
        else {
            getButton(Window.OK).setEnabled(true);            
        }
    }

}
