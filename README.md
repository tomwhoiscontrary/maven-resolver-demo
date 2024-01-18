This is a simple command-line tool for resolving Maven dependencies. Resolving consists of identifying the set of transitive dependencies, downloading the artifacts, and reporting the Maven coordinates and local file path.

This project is built with Gradle. To build it for command line use in place, do:

```
./gradlew installDist
```

You can then run the tool at:

```
build/install/maven-resolver-demo/bin/maven-resolver-demo
```

Alternatively, do:

```
./gradlew build
```

You can then find a tarball of the tool at:

```
build/distributions/maven-resolver-demo.tar
```

This tool tries to have a fairly helpful command-line interface; run it with a --help flag to get started. Here is an example with a couple of dependencies with overlapping transitive dependencies:

```
maven-resolver-demo org.junit.jupiter:junit-jupiter-api:5.6.2 org.junit.jupiter:junit-jupiter-engine:5.6.2
```

Output is in CSV.

This tool is based on Maven's own resolver <https://maven.apache.org/resolver/>. The documentation for this is horrible, but it was previously an Eclipse project, and they still have some quite good documentation for it <https://wiki.eclipse.org/Aether/Resolving_Dependencies>.

Maven manages its components using dependency injection, although this is a bit of a long story <https://maven.apache.org/maven-jsr330.html>. So, the most natural, and hopefully maintainable, way to use the Maven resolver is to set up a Maven-like dependency injection environment. This is done using Google Guice, and Maven's Sisu extension to it. This tool uses a more recent version of Guice than Maven, so it can run without warnings on Java 17. Mercifully, this is really easy to do.

A key part of this tool is the DemoResolverModule, which specifies the wireup of dependencies for the RepositorySystem. This is copied wholesale from the Maven resolver's examples <https://github.com/apache/maven-resolver/blob/maven-resolver-1.8.1/maven-resolver-demos/maven-resolver-demo-snippets/src/main/java/org/apache/maven/resolver/examples/guice/DemoResolverModule.java>, with the only change being to make it public.

There is a .tool-versions file to select the right Java version when using asdf. If you're not using asdf, you might need to set JAVA_HOME, but to be honest, Gradle will probably take care of this.
