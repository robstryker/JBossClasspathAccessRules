package org.jboss.ide.eclipse.classpath.poc.dialogs;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.wst.server.core.IRuntime;

public class AccessRestrictionWizard extends Wizard {
	private IJavaProject jproject;
	private ChooseRuntimePage firstPage;
	private AccessRestrictionPreviewPage secondPage;
	public AccessRestrictionWizard(IJavaProject jproject) {
		this.jproject = jproject;
		setNeedsProgressMonitor(true);
	}
    public void addPages() {
    	firstPage = new ChooseRuntimePage(jproject); 
    	secondPage = new AccessRestrictionPreviewPage(jproject);
    	addPage(firstPage);
    	addPage(secondPage);
		IWizardContainer c = getContainer();
		if( c instanceof IPageChangeProvider) {
			((IPageChangeProvider)c).addPageChangedListener(secondPage);
		}

    }
    public IRuntime getSelectedRuntime() {
    	return firstPage.getSelectedRuntime();
    }
	public boolean performFinish() {
		try {
		getContainer().run(true, true, new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				secondPage.finishPage(monitor);
			}
		});
		} catch(Exception e ) {}
		return true;
	}

}
