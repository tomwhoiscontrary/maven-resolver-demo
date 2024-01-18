package demo.resolver;

import com.google.inject.Guice;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.resolver.examples.guice.DemoResolverModule;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import picocli.CommandLine;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class Main {

    @CommandLine.Command(name = "maven-resolver-demo",
                         mixinStandardHelpOptions = true,
                         description = "Resolves Maven dependencies",
                         usageHelpAutoWidth = true)
    static class Options implements Runnable {
        @CommandLine.Option(names = {"-r", "--remote-repo"},
                            description = "the URI of the remote repository, from where dependencies will be fetched (defaults to ${DEFAULT-VALUE})",
                            defaultValue = "${MAVEN_REMOTE_REPO:-https://repo1.maven.org/maven2/}")
        URI remoteRepo;

        @CommandLine.Option(names = {"-l", "--local-repo"},
                            description = "the path of the local repository, where dependencies will be cached (defaults to ${DEFAULT-VALUE})",
                            defaultValue = "${MAVEN_LOCAL_REPO:-${sys:user.home}/.m2/repository}")
        Path localRepo;

        @CommandLine.Option(names = {"-s", "--scope"},
                            description = "the scope for which to evaluate transitive dependencies (defaults to ${DEFAULT-VALUE})",
                            defaultValue = "compile")
        String scope;

        @CommandLine.Parameters(description = "the coordinates of the dependencies to resolve", arity = "1..*")
        List<String> dependencies = Collections.emptyList();

        boolean run = false;

        @Override
        public void run() {
            run = true;
        }
    }

    public static void main(String[] args) throws DependencyResolutionException {
        Options options = parse(args);

        main(options.remoteRepo,
             options.localRepo,
             options.scope,
             options.dependencies);
    }

    private static Options parse(String[] args) {
        Options options = new Options();
        CommandLine commandLine = new CommandLine(options);

        int status = commandLine.execute(args);
        if (!options.run) System.exit(status);

        return options;
    }

    private static void main(URI remoteRepo,
                             Path localRepo,
                             String scope,
                             List<String> dependencies) throws DependencyResolutionException {
        RepositorySystem system = Guice.createInjector(new DemoResolverModule()).getInstance(RepositorySystem.class);

        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepository = new LocalRepository(localRepo.toFile());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));

        List<Artifact> artifacts = dependencies.stream()
                                               .map(coords -> (Artifact) new DefaultArtifact(coords))
                                               .toList();

        RemoteRepository remoteRepository = new RemoteRepository.Builder("repo", "default", remoteRepo.toString()).build();

        DependencyRequest request = new DependencyRequest(new CollectRequest((Dependency) null,
                                                                             artifacts.stream()
                                                                                      .map(artifact -> new Dependency(artifact, scope))
                                                                                      .toList(),
                                                                             List.of(remoteRepository)),
                                                          (node, parents) -> true);

        DependencyResult result = system.resolveDependencies(session, request);

        for (ArtifactResult artifactResult : result.getArtifactResults()) {
            Artifact artifact = artifactResult.getArtifact();
            boolean direct = containsMatching(artifacts, artifact);
            String coordinate = artifact.toString();
            File path = artifact.getFile();
            System.out.println((direct ? "direct" : "transitive") + "," + coordinate + "," + path);
        }
    }

    private static boolean containsMatching(List<Artifact> artifacts, Artifact artifact) {
        return artifacts.stream().anyMatch(a -> matches(a, artifact));
    }

    private static boolean matches(Artifact a, Artifact b) {
        // do not check version, because that may have been changed by the resolution process
        return a.getGroupId().equals(b.getGroupId())
               && a.getArtifactId().equals(b.getArtifactId())
               && a.getClassifier().equals(b.getClassifier());
    }

}
