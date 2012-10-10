package org.jboss.ide.eclipse.classpath.poc.popup.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.ide.eclipse.classpath.poc.dialogs.AccessRestrictionWizard;

public class AccessRestrictionHandler implements IHandler {
	public boolean isEnabled() {
		return true;
	}
	public boolean isHandled() {
		return true;
	}
	public void removeHandlerListener(IHandlerListener handlerListener) {
	}
	public void addHandlerListener(IHandlerListener handlerListener) {
	}
	public void dispose() {
	}
	
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("Executing");
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil
				.getCurrentSelectionChecked(event);
		final Object e = selection.getFirstElement();
		if( e instanceof IJavaProject ) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			AccessRestrictionWizard wiz = new AccessRestrictionWizard((IJavaProject)e);
			WizardDialog wd = new WizardDialog(shell, wiz);
			wd.open();
//			AccessRestrictionRunner runner = new AccessRestrictionRunner(
//					new RuntimeChooserDialog(shell), (IJavaProject)e);
//			runner.begin(new NullProgressMonitor());
//			
//			AccessRestrictionPreviewDialog d = new AccessRestrictionPreviewDialog(Display.getDefault().getActiveShell(), runner);
//			if( d.open() == Window.OK) {
//				runner.finish();
//			}
		}
		return null;
	}
	

}
