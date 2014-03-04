package org.apache.karaf.tooling.semantic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;

public class MojoContext {

	public final Logger logger;
	public final MavenProject project;
	public final Set<String> scopeIncluded;
	public final Set<String> scopeExcluded;
	public final RepositorySystem system;
	public final RepositorySystemSession session;
	public final List<RemoteRepository> projectRepos;
	public final Map<String, String> resolverSettings;
	public final Set<String> packagingIncluded;
	public final Set<String> typeIncluded;
	public final boolean includeTransitive;

	public MojoContext( //
			final Logger logger, //
			final MavenProject project, //
			final Set<String> scopeIncluded, //
			final Set<String> scopeExcluded, //
			final RepositorySystem system, //
			final RepositorySystemSession session, //
			final List<RemoteRepository> projectRepos, //
			final Map<String, String> resolverSettings, //
			final Set<String> packagingIncluded, //
			final Set<String> typeIncluded, //
			final boolean includeTransitive //
	) {
		this.logger = logger;
		this.project = project;
		this.scopeIncluded = scopeIncluded;
		this.scopeExcluded = scopeExcluded;
		this.system = system;
		this.session = session;
		this.projectRepos = projectRepos;
		this.resolverSettings = resolverSettings;
		this.packagingIncluded = packagingIncluded;
		this.typeIncluded=typeIncluded;
		this.includeTransitive = includeTransitive;
	}

}
