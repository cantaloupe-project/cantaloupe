[![Build Status](https://travis-ci.org/medusa-project/cantaloupe.svg?branch=develop)](https://travis-ci.org/medusa-project/cantaloupe)

# 🍈 Cantaloupe

*High-performance dynamic image server in Java*

# Users

**[Get started with a release build.](https://medusa-project.github.io/cantaloupe/get-started.html)**

# Developers

## Build & run

### Command line

* `mvn exec:java -Dcantaloupe.config=...` will build the project and run in
  standalone mode, using the embedded Servlet container listening on the
  port(s) specified in `cantaloupe.properties`.
* `mvn clean package -DskipTests` will build a release WAR in the `target`
  folder, which can be run like any other.

### IDE

There are a few ways to do this, in accordance with the options above. The
simplest is probably to:

1. Add a new run configuration using the "Java Application" template or its
   equivalent.
2. Set the main class to `edu.illinois.library.cantaloupe.StandaloneEntry` and
   add the `-Dcantaloupe.config=...` VM option.
3. You might have to download
   [servlet-api-3.1.jar](http://central.maven.org/maven2/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar)
   and add it to your classpath: `--class-path=/path/to/servlet-api-3.1.jar`

   Or, in Java 9, your module path: `--module-path=/path/to/containing/dir`

## Test

For testing, copy `test.properties.sample` to `test.properties` and fill in
whatever info you have.

The tests are structured into four profiles:

### 1. Tests with no dependencies

`mvn clean test -Pnodeps` will run only the tests that have no dependencies
on third-party services and do not require any exteral tools or libraries.

### 2. Tests with free dependencies

`mvn clean test -Pfreedeps` will run all of the above tests, plus any that
depend on open-source tools or libraries. These are the tests run in
continuous integration. The following dependencies are required:

* FFmpeg (for FfmpegProcessorTest)
* GraphicsMagick (for GraphicsMagickProcessorTest)
* ImageMagick (for ImageMagickProcessorTest)
* OpenJPEG (for OpenJpegProcessorTest)
* Redis (for RedisCacheTest)

### 3. All tests

`mvn clean test` will run all tests, including the ones above. The following
dependencies are required in addition to the ones above:

* Kakadu (for KakaduProcessorTest)
* A Microsoft Azure account

### 4. Performance tests

Performance tests use [JMH](http://openjdk.java.net/projects/code-tools/jmh/).
Run them with `mvn clean test -Pbenchmark`.

## Build the website

The website is built using [Jekyll](http://jekyllrb.com/). With that installed,
run `jekyll serve` from the `website` directory. Then, open
[http://localhost:4000/cantaloupe/](http://localhost:4000/cantaloupe/) in a
web browser.

An effort is made to keep the documentation in sync with development on the
same branch. The "Upgrading" and "Changes" sections of the website are
usually current.

## Contribute

Contributions are welcome. The suggested process for contributing code changes
is:

1. Submit a "heads-up" issue in the tracker, ideally before beginning any
   work.
2. [Create a fork.](https://github.com/medusa-project/cantaloupe/fork)
3. Create a feature branch, starting from either `release/x.x` or `develop`
   (see the "Versioning" section.)
4. Make your changes.
5. Commit your changes (`git commit -am 'Add some feature'`).
6. Push the branch (`git push origin feature/my-new-feature`).
7. Create a pull request.

## Other Notes

### Configuration keys

Different application versions may require different configuration file keys.
It's good practice to use a dedicated configuration file for each version.
Keys are documented on the website through the latest release. If you are
using a newer build, like a snapshot from the `develop` branch, newer keys will
be documented in
[website/upgrade.html](https://github.com/medusa-project/cantaloupe/blob/develop/website/upgrade.html).

### Versioning

Cantaloupe roughly uses semantic versioning. Major releases (n) involve major
rearchitecting that breaks backwards compatibility in a significant way. Minor
releases (n.n) either do not break compatibility, or only in a minor way.
Patch releases (n.n.n) are for bugfixes only.

### Branching

Cantaloupe uses the
[Gitflow](https://www.atlassian.com/git/tutorials/comparing-workflows#gitflow-workflow)
branching model. `develop` is the main branch that tracks the current state of
the next non-bugfix release. Significant features branch off of that into
feature branches (`feature/feature-name`), from which they can be integrated
into a particular release.

When a non-bugfix version is ready for release in `develop`, it branches
off into a new `release/n.n` branch, where its version is set. Finally, that
is merged into `master`, where the release is tagged and the release
distribution is created.

Bugfixes that would increment a minor version of a release are applied to the
release branch for that release, and merged back into `develop`.

## Releasing

1. Finalize the code to be released, addressing any relevant milestone issues,
   TODOs, etc.
2. Run the Maven Verifier plugin (`mvn verify -DskipTests=true`)
3. Run an OWASP dependency check (`mvn org.owasp:dependency-check-maven:check`)
4. Run Findbugs (`mvn clean compile findbugs:findbugs findbugs:gui`)
5. Ensure that the tests are current, comprehensive, and passing
6. Finalize the documentation, including the website, user manual, and change
   log
7. Merge into `release/vX.X`
8. Update the version in `pom.xml` and commit this change
9. Merge into `master`
10. Create the release `.zip` archive with `mvn clean package`
11. Verify that the `.zip` archive is as expected
12. Push the code: `git push origin master; git push origin release/x.x`
13. Wait for CI tests to pass
14. Tag the release: `git tag -a v{version} -m 'Tag v{version}'`
15. git push --tags
16. Add the `.zip` archive and change log info to the release tag on GitHub
17. Deploy the updated website using `build/deploy_website.rb`
18. Append `-SNAPSHOT` to the version in `pom.xml` and commit this change
19. Close the release's GitHub milestone

## License

Cantaloupe is open-source software distributed under the University of
Illinois/NCSA Open Source License; see the file LICENSE for terms.
