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

package org.cauldron.sigil.ui.editors.project;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.cauldron.sigil.ui.util.ModelLabelProvider;
import org.cauldron.sigil.ui.util.SingletonSelection;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

public class ResourceSelectDialog extends Dialog {

	private AtomicInteger keyCheck = new AtomicInteger();
	private ScheduledExecutorService background = Executors.newSingleThreadScheduledExecutor();
	
	private class UpdateViewerRunnable implements Runnable {
		private int check;
		public UpdateViewerRunnable(int check) {
			this.check = check;
		}

		public void run() {
			if ( check == keyCheck.get() ) {
				try {
					viewer.refresh();
				}
				catch (SWTException e) {
					// discard
				}
			}
		}
	}

	private static final ISelection EMPTY_SELECTION = new ISelection() {
		public boolean isEmpty() {
			return true;
		}
	};
	
	private Job job;
	
	private boolean isCombo;
	private String title;
	private String selectionText;

	private StructuredViewer viewer;
	private Combo resourceNameCombo;
	private Table resourceNameTable;
	private Text errorMessageText;
	private String errorMessage;
	
	private ViewerFilter filter;
	private Object[] selected;
	
	private Object scope;	
	private IContentProvider content;
	private ILabelProvider labelProvider;

	public ResourceSelectDialog(Shell parentShell, IContentProvider content, ViewerFilter filter, Object scope, String title, String selectionText, boolean isCombo) {
		super(parentShell);
		this.title = title;
		this.selectionText = selectionText;
		this.content = content;
		this.filter = filter;
		this.scope = scope;
		this.isCombo = isCombo;
	}
	
	public void setJob(Job job) {
		this.job = job;
	}
	
	public void refreshResources() {
		try {
			getShell().getDisplay().asyncExec( new Runnable() {
				public void run() {
					try {
						viewer.refresh();
					}
					catch (SWTException e) {
						// attempt to exec after dialog closed - discard
					}
				}
			});
		}
		catch (NullPointerException e) {
			// attempt to exec after dialog closed - discard
		}
		catch (SWTException e) {
			// attempt to exec after dialog closed - discard
		}
	}

	/*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (title != null) {
			shell.setText(title);
		}        
    }

	@Override
	public void create() {
		super.create();
		if ( getItemCount() == 0 ) {
			setErrorMessage( "No resources available" );
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
		else {
			ISelection selection = selected == null ? EMPTY_SELECTION : new SingletonSelection( selected );
			setSelected( new SelectionChangedEvent( viewer, selection ), true );
		}
		
		if ( job != null ) {
			job.schedule();
		}
	}

	@Override
	public boolean close() {
		if ( job != null ) {
			job.cancel();
		}
		background.shutdownNow();
		return super.close();
	}

	private int getItemCount() {
		if ( isCombo ) {
			return resourceNameCombo.getItemCount();
		}
		else {
			return resourceNameTable.getItemCount();
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite body = (Composite) super.createDialogArea(parent);
		
		GridLayout layout = (GridLayout) body.getLayout();
		layout.numColumns = 2;
		GridData data;
		
		labelProvider = new ModelLabelProvider();
		
		Label l = new Label( body, SWT.LEFT );
		l.setText( selectionText );
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		if ( !isCombo ) {
			data.horizontalSpan = 2;
		}
		l.setLayoutData( data );
	
		if ( isCombo ) {
			createCombo( body );
		}
		else {
			createTable( body );
		}
		
		viewer.addFilter( filter );
		viewer.setContentProvider(content);
		viewer.setLabelProvider( getLabelProvider() );
		viewer.setComparator( new ViewerComparator() );
		viewer.setInput( scope );
		
		viewer.addSelectionChangedListener( new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				setSelected(event, false);
			}			
		});
	    createCustom( body );
	    
		errorMessageText = new Text(body, SWT.READ_ONLY | SWT.WRAP);
		data = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 2;
	    errorMessageText.setLayoutData(data);
	    errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
	    setErrorMessage(errorMessage);
	    
		return body;
	}
	
    protected void createCustom(Composite body) {
	}

	private void createCombo(Composite body) {
		resourceNameCombo = new Combo( body, SWT.SINGLE | SWT.BORDER );
		GridData data = new GridData( GridData.HORIZONTAL_ALIGN_END);
		data.widthHint = 200;
		resourceNameCombo.setLayoutData( data );
		
		viewer = new ComboViewer(resourceNameCombo);
	}

	private void createTable(Composite body) {
		final Text txtFilter = new Text(body, SWT.BORDER);
		GridData data = new GridData( GridData.HORIZONTAL_ALIGN_END);
		data.horizontalSpan = 2;
		data.widthHint = 400;
		txtFilter.setLayoutData(data);
		
		resourceNameTable = new Table( body, SWT.MULTI | SWT.BORDER );
		data = new GridData( GridData.HORIZONTAL_ALIGN_END);
		data.widthHint = 400;
		data.heightHint = 400;
		resourceNameTable.setLayoutData( data );
		
		viewer = new TableViewer(resourceNameTable);

		txtFilter.addKeyListener( new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				switch ( e.keyCode ) {
				case SWT.ARROW_UP:
					scrollTable(-1);
					break;
				case SWT.ARROW_DOWN:
					scrollTable(+1);
					break;
				default:
					Runnable r = new UpdateViewerRunnable(keyCheck.incrementAndGet());
					background.schedule(r, 100, TimeUnit.MILLISECONDS);
					break;
				}
			}
		});
		
		ViewerFilter filter = new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				return getLabelProvider().getText(element).startsWith( txtFilter.getText() );
			}
		};
		
		viewer.addFilter(filter);
	}
	
	private void scrollTable(int delta) {
		int i = resourceNameTable.getSelectionIndex();
		
		if ( i == -1 ) {
			if ( delta < 0 ) { 
				i = resourceNameTable.getItemCount() - 1;
			}
			else {
				i = 0;
			}
		}
		else {
			i+=delta;
		}
		
		if ( i > -1 && i < resourceNameTable.getItemCount() ) {
			Item item = resourceNameTable.getItem(i);
			resourceNameTable.select(i);
			selected = new Object[] { item.getData() };
			ISelection selection = new SingletonSelection( selected );
			selectionChanged(new SelectionChangedEvent(viewer, selection));
			viewer.reveal(selected);
		}
	}

	private void setSelected(SelectionChangedEvent event, boolean reveal) {
		if ( event.getSelection().isEmpty() ) {
			selected = null;
			setErrorMessage( "No resource selected" );
		}
		else {
			selected = ((IStructuredSelection) event.getSelection()).toArray();
			setErrorMessage( null );
		}
		
		selectionChanged(event);
		
		if ( reveal && !event.getSelection().isEmpty() ) {
			if ( resourceNameCombo != null ) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				resourceNameCombo.select(resourceNameCombo.indexOf((String) sel.getFirstElement()));
			}
			else {
				viewer.setSelection(event.getSelection(), true);
			}
		}
	}

	protected ILabelProvider getLabelProvider() {
		return labelProvider;
	}

	public Object[] getSelected() {
    	return selected;
    }
	
	public void setSelected(Object[] selected) {
		this.selected = selected;
	}
	
	protected void selectionChanged(SelectionChangedEvent event) {
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		if (errorMessageText != null && !errorMessageText.isDisposed()) {
			errorMessageText.setText(errorMessage == null ? " \n " : errorMessage); 
			boolean hasError = errorMessage != null && (StringConverter.removeWhiteSpaces(errorMessage)).length() > 0;
			errorMessageText.setEnabled(hasError);
			errorMessageText.setVisible(hasError);
			errorMessageText.getParent().update();
			Control ok = getButton(IDialogConstants.OK_ID);
			if (ok != null) {
				ok.setEnabled(!hasError);
			}
		}
	}

}