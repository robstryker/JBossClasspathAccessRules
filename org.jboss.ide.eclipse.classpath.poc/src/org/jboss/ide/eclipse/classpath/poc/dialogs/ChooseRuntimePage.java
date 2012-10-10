package org.jboss.ide.eclipse.classpath.poc.dialogs;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.ui.internal.viewers.RuntimeTableLabelProvider;
import org.jboss.ide.eclipse.classpath.poc.popup.actions.ServerRuntimeUtil;

public class ChooseRuntimePage extends WizardPage {
	private TableViewer viewer;
	private IRuntime selected;
	private IJavaProject jproject;
	protected ChooseRuntimePage(IJavaProject jp) {
		super("Please select a runtime from which to generate access restrictions");
		this.jproject = jp;
	}

	public IRuntime getSelectedRuntime() {
		return selected;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(GridData.FILL_BOTH));
		main.setLayout(new FillLayout());
		createUI(main);
		setTitle("Please Choose a runtime");
		setMessage("Please choose from the list of runtimes below. From this runtime,\njars recognized as JBoss jars will be given access rules corresponding to their status in the given distribution.");
		getShell().setText("Choose a Runtime");
		setControl(main);
	}
	
	protected void createUI(Composite main) {
		final IRuntime[] rt = ServerRuntimeUtil.findAllPossibleRuntimes(jproject);

		viewer = new TableViewer(main, SWT.SINGLE | SWT.BORDER);
		viewer.setContentProvider(new ArrayContentProvider(){
			public Object[] getElements(Object inputElement) {
				return rt;
			}
		});
		viewer.setLabelProvider(new RuntimeTableLabelProvider());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				selected = (IRuntime) ((IStructuredSelection)viewer.getSelection()).getFirstElement();
			}
		});
		viewer.setInput(ResourcesPlugin.getWorkspace());
	}
	
}
