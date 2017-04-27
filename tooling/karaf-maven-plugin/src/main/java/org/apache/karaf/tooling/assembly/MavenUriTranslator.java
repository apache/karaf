package org.apache.karaf.tooling.assembly;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles translation of Maven URI to local file URI.
 */
class MavenUriTranslator {

    Map<String, String> getTranslatedUris(final MavenProject mavenProject, final Map<String, String> translatedUrls) {
        final List<Artifact> artifacts = getProjectAndAttachedArtifacts(mavenProject);
        final Stream<Artifact> artifactsWithLocalFiles = getArtifactsWithLocalFiles(artifacts);
        final Map<String, String> urls = artifactsWithLocalFiles.collect(collectAsMvnUriToLocalFileMap());
        urls.putAll(translatedUrls);
        return urls;
    }

    private Collector<Artifact, ?, Map<String, String>> collectAsMvnUriToLocalFileMap() {
        return Collectors.toMap(this::artifactToMvnUri, this::artifactFileUriAsString);
    }

    private Stream<Artifact> getArtifactsWithLocalFiles(final List<Artifact> artifacts) {
        return artifacts.stream()
                        .filter(artifact -> artifact.getFile() != null)
                        .filter(artifact -> artifact.getFile()
                                                    .exists());
    }

    private List<Artifact> getProjectAndAttachedArtifacts(final MavenProject mavenProject) {
        return Stream.concat(mavenProject.getAttachedArtifacts()
                                         .stream(), Stream.of(mavenProject.getArtifact()))
                     .collect(Collectors.toList());
    }

    String artifactToMvnUri(final Artifact artifact) {
        final String classifier = Optional.ofNullable(artifact.getClassifier())
                                          .filter(c -> !"".matches(c))
                                          .map(c -> "/" + c)
                                          .orElse("");
        String extension = artifact.getArtifactHandler()
                                   .getExtension();
        if ("bundle".equals(extension)) {
            extension = "jar";
        }
        final String type = "/" + extension;
        String suffix = "";
        if (!classifier.isEmpty() || !"/jar".equals(type)) {
            suffix = type + classifier;
        }
        return String.format("mvn:%s/%s/%s%s", artifact.getGroupId(), artifact.getArtifactId(),
                             artifact.getBaseVersion(), suffix
                            );
    }

    private String artifactFileUriAsString(final Artifact artifact) {
        return artifact.getFile()
                       .toURI()
                       .toString();
    }

}
