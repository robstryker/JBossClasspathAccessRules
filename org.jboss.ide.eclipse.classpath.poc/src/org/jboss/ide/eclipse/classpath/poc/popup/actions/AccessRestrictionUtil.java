package org.jboss.ide.eclipse.classpath.poc.popup.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.server.core.IRuntime;
import org.jboss.jdf.modules.BuildException;
import org.jboss.jdf.modules.ModulesInformationBuilder;
import org.jboss.jdf.modules.jar.Gav;
import org.jboss.jdf.modules.jar.Jar;
import org.jboss.jdf.modules.model.BaseModule;
import org.jboss.jdf.modules.model.Module;

public class AccessRestrictionUtil {
	public File[] getPublicModules(IRuntime runtime) {
		ArrayList<File> results = new ArrayList<File>();
		File f2 = runtime.getLocation().append("modules").toFile();
		try {
			List<BaseModule> list = ModulesInformationBuilder.getInstance(f2).build();
			for( int i = 0; i < list.size(); i++ ) {
				BaseModule o = list.get(i);
				if( o instanceof Module && !((Module)o).isPrivateModule()) {
					List<Jar> jars = ((Module)o).getResources();
					for( int j = 0; j < jars.size(); j++ ) {
						results.add(jars.get(j).getJarFile());
					}
				}
			}
		} catch(BuildException be) {
			be.printStackTrace();
		}
		return (File[]) results.toArray(new File[results.size()]);
	}
	
	public GavModel createGavModel(IRuntime runtime) {
		return new GavModel(runtime);
	}
	
	public static class GavModel {
		private IRuntime runtime;
		private HashMap<String, HashMap<String, HashMap<String, Module>>> model;
		public GavModel(IRuntime runtime) {
			this.runtime = runtime;
		}
		public void load(IProgressMonitor monitor) {
			model = new HashMap<String, HashMap<String, HashMap<String, Module>>>();
			File f2 = runtime.getLocation().append("modules").toFile();
			try {
				List<BaseModule> list = ModulesInformationBuilder.getInstance(f2).build();
				monitor.beginTask("Analyzing JBoss Runtime Modules", 200+(100*list.size()));
				monitor.setTaskName("Analyzing JBoss Runtime Modules");
				monitor.worked(200);
				for( int i = 0; i < list.size(); i++ ) {
					BaseModule o = list.get(i);
					String name = o.getName();
					if( o instanceof Module ) {
						List<Jar> jars = ((Module)o).getResources();
						for( int j = 0; j < jars.size(); j++ ) {
							Jar jar = jars.get(j);
							try {
								Gav gav = jar.getGav();
								if( gav == null )
									continue;
								HashMap<String, HashMap<String, Module>> artifactToVersions = model.get(gav.getGroupId());
								if( artifactToVersions == null ) {
									artifactToVersions = new HashMap<String, HashMap<String, Module>>();
									model.put(gav.getGroupId(), artifactToVersions);
								}
								HashMap<String, Module> versionToModule = artifactToVersions.get(gav.getArtifactId());
								if( versionToModule == null ) {
									versionToModule = new HashMap<String, Module>();
									artifactToVersions.put(gav.getArtifactId(), versionToModule);
								}
								versionToModule.put(gav.getVersion(), ((Module)o));
							} catch(IOException ioe) {
								// TODO log
							}
						}
					}
					monitor.worked(100);
				}
			} catch(BuildException be) {
				// TODO LOG
				be.printStackTrace();
			}
			monitor.done();
		}
		
		/**
		 * Given a group, artifact, and version, find the access rule 
		 * @param group
		 * @param artifact
		 * @param version
		 * @param fuzzyVersion allow non-precise versions to use the same access rule
		 * @return
		 */
		public IAccessRule[] getAccessRules(String group, String artifact, String version, boolean fuzzyVersion) {
			// Force load
			if( model == null )
				load(new NullProgressMonitor());
			
			HashMap<String, HashMap<String, Module>> artifactToVersion = model.get(group);
			if( artifactToVersion != null ) {
				HashMap<String, Module> versionToModule = artifactToVersion.get(artifact);
				if( versionToModule != null ) {
					Module m = versionToModule.get(version);
					if( m == null && fuzzyVersion) {
						ArrayList<String> s = new ArrayList<String>(versionToModule.keySet());
						if( s.size() > 0 ) {
							Collections.sort(s);
							m = versionToModule.get(s.get(s.size()-1));
						}
					}
					if( m != null ) {
						boolean privvy = m.isPrivateModule();
						if( privvy ) {
							IAccessRule r = JavaCore.newAccessRule(new Path("**"), IAccessRule.K_NON_ACCESSIBLE);
							return new IAccessRule[]{r};
						} else {
							IAccessRule r = JavaCore.newAccessRule(new Path("**"), IAccessRule.K_ACCESSIBLE);
							return new IAccessRule[]{r};
						}
					}
				}
			}
			return new IAccessRule[0];
		}
	}
}
