package org.jboss.ide.eclipse.classpath.poc.popup.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.server.core.IRuntime;
import org.jboss.ide.eclipse.classpath.poc.popup.actions.AccessRestrictionUtil.GavModel;
import org.jboss.jdf.modules.jar.Gav;
import org.jboss.jdf.modules.jar.Jar;

public class AccessRestrictionRunner {
	public static final String ACCESSRULES= "accessrules"; //$NON-NLS-1$
	private IJavaProject jproject;
	
	private ArrayList<IClasspathEntry> entriesToModify;
	private ArrayList<IClasspathEntry> unchangedEntries;
	private IClasspathEntry[] updatedRawEntries;
	private HashMap<IPath, ArrayList<IClasspathEntry>> containerEntriesToModify;
	private HashMap<IPath, ArrayList<IClasspathEntry>> unchangedContainerEntries;
	
	private IRuntime runtime;
	public AccessRestrictionRunner( IJavaProject jp, IRuntime runtime) {
		this.jproject = jp;
		this.runtime = runtime;
		entriesToModify = new ArrayList<IClasspathEntry>();
		unchangedEntries = new ArrayList<IClasspathEntry>();
		containerEntriesToModify = new HashMap<IPath, ArrayList<IClasspathEntry>>();
		unchangedContainerEntries = new HashMap<IPath, ArrayList<IClasspathEntry>>();
	}
	
	public HashMap<IPath, ArrayList<IClasspathEntry>> getContainerEntriesToModify() {
		return containerEntriesToModify;
	}
	public HashMap<IPath, ArrayList<IClasspathEntry>> getUnchangedContainerEntries() {
		return unchangedContainerEntries;
	}
	public ArrayList<IClasspathEntry> getEntriesToModify() {
		return entriesToModify;
	}
	public ArrayList<IClasspathEntry> getUnchangedEntries() {
		return unchangedEntries;
	}

	public void begin(IProgressMonitor monitor) {
		try {
			IRuntime chosenRuntime = runtime;
			if( chosenRuntime == null )
				return;
			GavModel gavModel = new AccessRestrictionUtil().createGavModel(chosenRuntime);
			
			IClasspathEntry[] raw = jproject.getRawClasspath();
			IClasspathEntry[] updated = new IClasspathEntry[raw.length];
			for( int i = 0; i < raw.length; i++ ) {
				updated[i] = getUpdatedEntry(raw[i], gavModel, jproject);
				if( updated[i] != raw[i] )
					entriesToModify.add(updated[i]);
				else if( unchangedContainerEntries.get(raw[i].getPath()) == null) {
					// avoid duplicates, where the container itself is not changed, 
					// and then there's a list of other unchanged jars
					unchangedEntries.add(raw[i]);
				}
			}
			updatedRawEntries = updated;
		} catch(JavaModelException jme) {
			jme.printStackTrace();
		}
	}
	
	public void finish(IProgressMonitor monitor) {
		// finally set the official Classpath for the project to the new updated ones
		try {
			jproject.setRawClasspath(updatedRawEntries, new NullProgressMonitor());
		} catch(JavaModelException jme) {
			jme.printStackTrace();
		}
		
		Set<IPath> containerPaths = containerEntriesToModify.keySet();
		for(Iterator<IPath> i = containerPaths.iterator(); i.hasNext(); ) {
			IPath containerPath = i.next();
			ArrayList<IClasspathEntry> entries = containerEntriesToModify.get(containerPath);
			for( Iterator<IClasspathEntry> j = entries.iterator(); j.hasNext(); ) {
				try {
					ClasspathUtil.updateContainerClasspath(jproject, containerPath, j.next(), new String[]{ACCESSRULES}, new NullProgressMonitor());
				} catch(CoreException ce) {
					ce.printStackTrace();
				}
			}
		}
	}
	
	private IClasspathEntry getUpdatedEntry(IClasspathEntry entry, GavModel gavModel, IJavaProject jproject) {
		if(entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
			IPath path = entry.getPath();
			IAccessRule[] rules = findAccessRuleForFile(path, gavModel);
			if( rules != null && rules.length > 0 ) {
				// Create a new cpe with the access rules
				IClasspathEntry up = JavaCore.newLibraryEntry(entry.getPath(), 
						entry.getSourceAttachmentPath(), entry.getSourceAttachmentRootPath(),
						rules, entry.getExtraAttributes(), entry.isExported());
				return up;
			} else {
				return entry;
			}
		} else if( entry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
			// TODO figure out what to do here
			return entry;
		} else if( entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
			traverseContainerEntries(entry, gavModel);
			return entry;
		} else {
			return entry;
		}
	}
	
	private void traverseContainerEntries(IClasspathEntry raw, GavModel gavModel) {
		IPath containerPath = raw.getPath();
		try {
			IClasspathContainer container= JavaCore.getClasspathContainer(containerPath, jproject);
			if (container != null) {
				IClasspathEntry[] children = container.getClasspathEntries();
				for( int i = 0; i < children.length; i++ ) {
					IClasspathEntry updatedChild = getUpdatedEntry(children[i], gavModel, jproject);
					if( updatedChild != children[i]) {
						addEntryToContainerMap(updatedChild, containerPath, containerEntriesToModify);
					} else {
						addEntryToContainerMap(children[i], containerPath, unchangedContainerEntries);
					}
				}
			}
		} catch(JavaModelException jme) {
			// ignore
		}
	}
	
	private void addEntryToContainerMap(IClasspathEntry entry,IPath containerPath,  HashMap<IPath, ArrayList<IClasspathEntry>> map) {
		ArrayList<IClasspathEntry> entriesForContainer = map.get(containerPath);
		if( entriesForContainer == null ) {
			entriesForContainer = new ArrayList<IClasspathEntry>();
			map.put(containerPath, entriesForContainer);
		}
		entriesForContainer.add(entry);
	}
	
	public IAccessRule[] findAccessRuleForFile(IPath path, GavModel gavModel) {
		System.out.println(path);
		if( path.toOSString().contains("jboss-annotations-api")) {
			System.out.println("BREAK");
		}
		Jar j = new Jar(path.removeLastSegments(1).toFile(), path.toFile());
		if( !path.toFile().exists())
			return null;
		try {
			Gav gav = j.getGav();
			if( gav != null ) {
				IAccessRule[] rule = gavModel.getAccessRules(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), true);
				return rule;
			}
		} catch(IOException ioe) {
			
		}
		return null;
	}
}
