package org.jboss.ide.eclipse.classpath.poc.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.AccessRulesLabelProvider;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IRuntime;
import org.jboss.ide.eclipse.classpath.poc.popup.actions.AccessRestrictionRunner;

public class AccessRestrictionPreviewPage extends WizardPage implements IPageChangedListener{
	private AccessRestrictionRunner runner;
	private TreeViewer toChange, unchanged;
	private IRuntime selectedRuntime;
	private IJavaProject jp;
	public AccessRestrictionPreviewPage(IJavaProject jp) {
		super("");
		this.jp = jp;
	}
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(GridData.FILL_BOTH));
		main.setLayout(new FormLayout());
		createUI(main);
		setTitle("Verify the changes to the projects classpath below, or press cancel");
		setMessage("The entries on the left have been recognized as JBoss jars, and will be marked as either public or private.\nThe entries on the right are not recognized as JBoss classpath entries and will have no changes performed.");
		setControl(main);
		getShell().setText("Preview classpath changes");
	}
	
	protected void createUI(Composite main) {
		Label toChangeLabel = new Label(main, SWT.NONE);
		Label unchangedLabel = new Label(main,SWT.NONE);
		toChangeLabel.setText("Recognized Classpath Entries");
		unchangedLabel.setText("Unrecognized Classpath Entries");
		toChangeLabel.setLayoutData(createFormData(0,5,null,0,0,5,50,-5));
		unchangedLabel.setLayoutData(createFormData(0,5,null,0,50,5,100,-5));
		
		
		toChange = new TreeViewer(main);
		toChange.getTree().setLayoutData(createFormData(toChangeLabel,5,100,0,0,5,50,-5));
		unchanged = new TreeViewer(main);
		unchanged.getTree().setLayoutData(createFormData(toChangeLabel,5,100,0,50,5,100,-5));
		
		toChange.setLabelProvider(new PreviewLabelProvider());
		unchanged.setLabelProvider(new PreviewLabelProvider());
		
		toChange.setContentProvider(new PreviewContentProvider(new ContentFetcher(ContentFetcher.MATCHED)));
		unchanged.setContentProvider(new PreviewContentProvider(new ContentFetcher(ContentFetcher.UNMATCHED)));
		
		toChange.setInput(ResourcesPlugin.getWorkspace());
		unchanged.setInput(ResourcesPlugin.getWorkspace());
	}
	
	private class ContentFetcher {
		public static final boolean MATCHED = true;
		public static final boolean UNMATCHED = false;
		private boolean type;
		public ContentFetcher(boolean type) {
			this.type = type;
		}
		public HashMap<IPath, ArrayList<IClasspathEntry>> getContainerEntries() {
			if( runner != null ) {
				return type ? runner.getContainerEntriesToModify() : runner.getUnchangedContainerEntries();
			}
			return new HashMap<IPath, ArrayList<IClasspathEntry>>();
		}
		public ArrayList<IClasspathEntry> getNormalEntries() {
			if( runner != null ) {
				return type ? runner.getEntriesToModify() : runner.getUnchangedEntries();
			}
			return new ArrayList<IClasspathEntry>();
		}
	}
	
	public static class PreviewContentProvider implements ITreeContentProvider {
		private ContentFetcher fetcher;
		public PreviewContentProvider(ContentFetcher fetcher) {
			this.fetcher = fetcher;
		}
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		public Object[] getElements(Object inputElement) {
			ArrayList<Object> elements = new ArrayList<Object>();
			elements.addAll(fetcher.getNormalEntries());
			elements.addAll(fetcher.getContainerEntries().keySet());
			return (Object[]) elements.toArray(new Object[elements.size()]);
		}
		public Object[] getChildren(Object parentElement) {
			if( parentElement instanceof IPath ) {
				ArrayList<IClasspathEntry> ret = fetcher.getContainerEntries().get((IPath)parentElement);
				return (IClasspathEntry[]) ret.toArray(new IClasspathEntry[ret.size()]);
			} else if( parentElement instanceof IClasspathEntry) {
				return ((IClasspathEntry)parentElement).getAccessRules();
			}
			return null;
		}
		public Object getParent(Object element) {
			return null;
		}
		public boolean hasChildren(Object element) {
			Object[] children = getChildren(element);
			return children != null && children.length > 0;
		}
	}
	
	
	public static class PreviewLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			if( element instanceof IPath ) {
				// It's a container
				return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_LIBRARY);
			} else if( element instanceof IClasspathEntry ){
				IClasspathEntry e = (IClasspathEntry)element;
				switch (e.getEntryKind()) {
					case IClasspathEntry.CPE_SOURCE:
					case IClasspathEntry.CPE_PROJECT:
						return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJ_PROJECT);
					case IClasspathEntry.CPE_VARIABLE:
						return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASSPATH_VAR_ENTRY);
				}
				return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE);
			}
			if (element instanceof IAccessRule) {
				IAccessRule rule= (IAccessRule) element;
				return AccessRulesLabelProvider.getResolutionImage(rule.getKind());
			}
			return null;
		}
		public String getText(Object element) {
			if( element instanceof IPath) {
				return ((IPath)element).lastSegment();
			}
			if( element instanceof IClasspathEntry) {
				return ((IClasspathEntry)element).getPath().lastSegment();
			}
			return element == null ? "" : element.toString();//$NON-NLS-1$
		}
	}

	public static FormData createFormData(Object topStart, int topOffset, Object bottomStart, int bottomOffset, 
			Object leftStart, int leftOffset, Object rightStart, int rightOffset) {
		FormData data = new FormData();

		if( topStart != null ) {
			data.top = topStart instanceof Control ? new FormAttachment((Control)topStart, topOffset) : 
				new FormAttachment(((Integer)topStart).intValue(), topOffset);
		}

		if( bottomStart != null ) {
			data.bottom = bottomStart instanceof Control ? new FormAttachment((Control)bottomStart, bottomOffset) : 
				new FormAttachment(((Integer)bottomStart).intValue(), bottomOffset);
		}

		if( leftStart != null ) {
			data.left = leftStart instanceof Control ? new FormAttachment((Control)leftStart, leftOffset) : 
				new FormAttachment(((Integer)leftStart).intValue(), leftOffset);
		}

		if( rightStart != null ) {
			data.right = rightStart instanceof Control ? new FormAttachment((Control)rightStart, rightOffset) : 
				new FormAttachment(((Integer)rightStart).intValue(), rightOffset);
		}
		return data;
	}

	@Override
	public void pageChanged(final PageChangedEvent event) {
		Object o = event.getSelectedPage();
		if( o == this ) {
			new Job("Launch Something") {
				protected IStatus run(IProgressMonitor monitor) {
					runPageChangedInWizard(event);
					return Status.OK_STATUS;
				}
			}.schedule();
		}
	}
	
	private void runPageChangedInWizard(final PageChangedEvent event) {
		final IRunnableWithProgress r = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				AccessRestrictionWizard wiz = (AccessRestrictionWizard)getWizard();
				if( !wiz.getSelectedRuntime().equals(selectedRuntime)) {
					selectedRuntime = wiz.getSelectedRuntime();
					runner = new AccessRestrictionRunner(jp, selectedRuntime);
					// Long running task
					runner.begin(new NullProgressMonitor());
				}
				Display.getDefault().asyncExec(new Runnable() { public void run() { 
					refreshViewer();
				}});
			}
		};
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					getWizard().getContainer().run(false, false, r);
				} catch(InterruptedException ie) {
					ie.printStackTrace();
				} catch(InvocationTargetException ite) {
					ite.printStackTrace();
				}
			}
		});
	}
	private void refreshViewer() {
		toChange.refresh();
		unchanged.refresh();
	}
	public void finishPage(IProgressMonitor monitor) {
		runner.finish(monitor);
	}
}
