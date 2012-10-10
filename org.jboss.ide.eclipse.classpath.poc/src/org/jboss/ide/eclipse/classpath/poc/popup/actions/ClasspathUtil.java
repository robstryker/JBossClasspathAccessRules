package org.jboss.ide.eclipse.classpath.poc.popup.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.jboss.ide.eclipse.classpath.poc.Activator;

public class ClasspathUtil {
	public static void updateContainerClasspath(IJavaProject jproject, IPath containerPath, IClasspathEntry newEntry, String[] changedAttributes, IProgressMonitor monitor) throws CoreException {
		IClasspathContainer container= JavaCore.getClasspathContainer(containerPath, jproject);
		if (container == null) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, "Container " + containerPath + " cannot be resolved", null));  //$NON-NLS-1$//$NON-NLS-2$
		}
		IClasspathEntry[] entries= container.getClasspathEntries();
		IClasspathEntry[] newEntries= new IClasspathEntry[entries.length];
		for (int i= 0; i < entries.length; i++) {
			IClasspathEntry curr= entries[i];
			if (curr.getEntryKind() == newEntry.getEntryKind() && curr.getPath().equals(newEntry.getPath())) {
				newEntries[i]= getUpdatedEntry(curr, newEntry, changedAttributes, jproject);
			} else {
				newEntries[i]= curr;
			}
		}
		requestContainerUpdate(jproject, container, newEntries);
		monitor.worked(1);
	}
	
	private static IClasspathEntry getUpdatedEntry(IClasspathEntry currEntry, IClasspathEntry updatedEntry, String[] updatedAttributes, IJavaProject jproject) {
		if (updatedAttributes == null) {
			return updatedEntry; // used updated entry 'as is'
		}
		CPListElement currElem= CPListElement.createFromExisting(currEntry, jproject);
		CPListElement newElem= CPListElement.createFromExisting(updatedEntry, jproject);
		for (int i= 0; i < updatedAttributes.length; i++) {
			String attrib= updatedAttributes[i];
			currElem.setAttribute(attrib, newElem.getAttribute(attrib));
		}
		return currElem.getClasspathEntry();
	}
	
	public static void requestContainerUpdate(IJavaProject jproject, IClasspathContainer container, IClasspathEntry[] newEntries) throws CoreException {
		IPath containerPath= container.getPath();
		IClasspathContainer updatedContainer= new UpdatedClasspathContainer(container, newEntries);
		ClasspathContainerInitializer initializer= JavaCore.getClasspathContainerInitializer(containerPath.segment(0));
		if (initializer != null) {
			initializer.requestClasspathContainerUpdate(containerPath, jproject, updatedContainer);
		}
	}

	private static class UpdatedClasspathContainer implements IClasspathContainer {

		private IClasspathEntry[] fNewEntries;
		private IClasspathContainer fOriginal;

		public UpdatedClasspathContainer(IClasspathContainer original, IClasspathEntry[] newEntries) {
			fNewEntries= newEntries;
			fOriginal= original;
		}

		public IClasspathEntry[] getClasspathEntries() {
			return fNewEntries;
		}

		public String getDescription() {
			return fOriginal.getDescription();
		}

		public int getKind() {
			return fOriginal.getKind();
		}

		public IPath getPath() {
			return fOriginal.getPath();
		}
	}

}
