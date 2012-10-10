package org.jboss.ide.eclipse.classpath.poc.popup.actions;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;

public class ServerRuntimeUtil {

	public static IRuntime[] findAllPossibleRuntimes(IJavaProject jp) {
		ArrayList<IRuntime> list = new ArrayList<IRuntime>();
		try {
			IFacetedProject facetedProject = ProjectFacetsManager.create(jp.getProject());
			if( facetedProject != null ) {
				// First get the targeted runtime
				org.eclipse.wst.common.project.facet.core.runtime.IRuntime rt = facetedProject.getPrimaryRuntime();
				if( rt != null ) {
					IRuntime wstRuntime = ServerCore.findRuntime(rt.getName());
					if( wstRuntime != null )
						list.add(wstRuntime);
				}
				
				// Next check all servers for this deployment
				IServer[] all = ServerCore.getServers();
				for( int i = 0; i < all.length; i++ ) {
					if( all[i].getRuntime() != null ) {
						if( projectModuleOnServer(all[i], jp.getProject())) {
							if( !list.contains(all[i].getRuntime()))
								list.add(all[i].getRuntime());
						}
					}
				}
			}
		} catch(CoreException ce) {
			
		}
		return (IRuntime[]) list.toArray(new IRuntime[list.size()]);
	}
	
	
	public static boolean projectModuleOnServer(IServer server, IProject needle) {
		IModule[] possible = ServerUtil.getModules(needle);
		for( int i = 0; i < possible.length; i++ ) {
			if( serverContainsModule(server, null, possible[i])) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean serverContainsModule(IServer server, IModule[] parent, IModule needle) {
		
		IModule[] children;
		if( parent != null )
			children = server.getChildModules(parent, null);
		else
			children = server.getModules();
		for( int i = 0; i < children.length; i++ ) {
			if( children[i].getId().equals(needle.getId())) {
				return true;
			}
			IModule[] newParentPaths = combine(parent, children[i]);
			if( serverContainsModule(server,newParentPaths, needle)) 
				return true;
		}
		return false;
	}
	
	public static IModule[] combine(IModule[] parent, IModule child) {
		if( parent == null )
			return new IModule[] { child };
		IModule[] ret = new IModule[parent.length+1];
		for( int i = 0; i < parent.length; i++ ) {
			ret[i] = parent[i];
		}
		ret[ret.length-1] = child;
		return ret;
	}
}
