[![Build Status](https://travis-ci.org/cantaloupe-project/cantaloupe.svg?branch=develop)](https://travis-ci.org/cantaloupe-project/cantaloupe)

# 🍈 Cantaloupe

*High-performance dynamic image server in Java*

# Users

**[Get started with a release build.](https://github.com/cantaloupe-project/cantaloupe/releases)**

# Developers

## Build & run

### Command line

* `mvn exec:java -Dcantaloupe.config=...` will build the project and run in
  standalone mode, using the embedded Servlet container listening on the
  port(s) specified in `cantaloupe.properties`.
* `mvn clean package -DskipTests` will build a release WAR in the `target`
  folder, which can be run like any other.

### IDE

There are a few ways to do this. The simplest is probably:

1. Add a new run configuration using the "Java Application" template or its
   equivalent.
2. Set the main class to `edu.illinois.library.cantaloupe.StandaloneEntry` and
   add the `-Dcantaloupe.config=...` VM option.
3. Set the run configuration to include dependencies with `provided` scope.
   (IntelliJ has a checkbox for this.) Alternatively, download
   [servlet-api-3.1.jar](http://central.maven.org/maven2/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar)
   and add it to your classpath: `--class-path=/path/to/servlet-api-3.1.jar`

   Or, in Java 9+, your module path: `--module-path=/path/to/containing/dir`

## Test

### Testing the code

Copy `test.properties.sample` to `test.properties` and fill in whatever info
you have.

The code tests are structured into three Maven profiles:

#### 1. Tests with no dependencies

`mvn clean test -Pnodeps` will run only the tests that have no dependencies
on third-party services and do not require any external tools or libraries.

#### 2. Tests with free dependencies

`mvn clean test -Pfreedeps` will run all of the above tests, plus any that
depend on open-source tools or libraries. These are the tests run in
continuous integration. The following dependencies are required:

* DynamoDB or [dynamodb-local](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html)
  (for DynamoDBCacheTest)
* FFmpeg (for FfmpegProcessorTest)
* GraphicsMagick (for GraphicsMagickProcessorTest)
* ImageMagick (for ImageMagickProcessorTest)
* OpenJPEG (for OpenJpegProcessorTest)
* Redis (for RedisCacheTest)
* TurboJPEG with Java binding (for TurboJpegProcessorTest)

#### 3. All tests

`mvn clean test` will run all tests, including the ones above. The following
dependencies are required in addition to the ones above:

* Kakadu native library (for KakaduNativeProcessorTest) - see the
  KakaduNativeProcessor section of the user manual for information.
* A Microsoft Azure account

#### Docker

Because it can be a chore to install all of the dependencies needed to get all
of the tests in the `freedeps` profile passing, there is a Docker testing image
available that contains all needed dependencies. See the `/.travis.yml` file.

### Output testing

There is an [output tester tool](https://github.com/cantaloupe-project/output-tester)
that enables visual inspection of image output.

### Performance testing

Performance tests use [JMH](http://openjdk.java.net/projects/code-tools/jmh/).
Run them with `mvn clean test -Pbenchmark`.

## Contribute

Contributions are welcome. The suggested process for contributing code changes
is:

1. Submit a "heads-up" issue in the tracker, ideally before beginning any
   work.
2. [Create a fork.](https://github.com/cantaloupe-project/cantaloupe/fork)
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
Key changes are documented in [UPGRADING.md](https://github.com/cantaloupe-project/cantaloupe/blob/develop/UPGRADING.md).

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

When a major or minor version is ready for release in `develop`, it branches
off into a `release/n.n` branch, where its version is set. Finally, that is
merged into `master`, where the release is tagged and the release archive is
created.

Bugfixes that would increment a minor version of a release are applied to the
release branch for that release, and merged back into `develop`.

## Prerelease

1. Run the Maven Verifier plugin (`mvn verify -DskipTests=true`)
2. Run an OWASP dependency check (`mvn org.owasp:dependency-check-maven:check`)
3. Run Findbugs (`mvn clean compile findbugs:findbugs findbugs:gui`)
4. Run the tests:
   * JUnit
   * [Endpoint tests](https://github.com/cantaloupe-project/output-tester)
5. Finalize the code to be released, addressing any relevant milestone issues,
   TODOs, etc.
6. Finalize the documentation, including the website, user manual, and change
   log

## Releasing

1. Merge into `release/vX.X`
2. Update the version in `pom.xml` and commit this change
3. Merge into `master`
4. Push the code: `git push origin master; git push origin release/x.x`
5. Wait for CI tests to pass
6. Create the release `.zip` archive with `mvn clean package -DskipTests`
7. Verify that the `.zip` archive is as expected
8. Tag the release: `git tag -a v{version} -m 'Tag v{version}'`
9. git push --tags
10. Add the `.zip` archive and change log info to the release tag on GitHub
11. Deploy the updated website (usually not necessary for bugfix releases)
12. Close the release's GitHub milestone

## License

Cantaloupe is open-source software distributed under the University of
Illinois/NCSA Open Source License; see the file LICENSE.txt for terms.
