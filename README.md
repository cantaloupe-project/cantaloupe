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
* `mvn tomcat:run -Dcantaloupe.config=...` will build the project and run in
  Servlet mode, using a Tomcat container listening on port 8080.
* `mvn package` will build a release WAR in the `target` folder, which
  can be run like any other.

### IDE

There are a few ways to do this, in accordance with the options above. The
simplest is probably to:

1. Add a new run configuration using the "Java Application" template or its
   equivalent.
2. Set the main class to `edu.illinois.library.cantaloupe.StandaloneEntry` and
   add the `-Dcantaloupe.config=...` VM option.
3. You might have to download `servlet-api-3.1.jar` and add it to your
   classpath.

## Test

For testing, copy `test.properties.sample` to `test.properties` and fill in
whatever info you have.

The tests are structured into three profiles:

### Tests with no dependencies

`mvn clean test -Pnodeps` will run only the tests that have no dependencies
on third-party services and do not require any tools or libraries to be
installed.

### Tests with free dependencies

`mvn clean test -Pfreedeps` will run all of the above tests, plus any that
depend on open-source tools and libraries. These are the tests run in
continuous integration. The following dependencies are required:

* Arial font
* FFmpeg (FfmpegProcessorTest)
* GraphicsMagick (GraphicsMagickProcessorTest)
* ImageMagick (ImageMagickProcessorTest)
* OpenJPEG (OpenJpegProcessorTest)
* Redis (RedisCacheTest)

### All tests

`mvn clean test` will run all tests, including the ones above. The following
dependencies are required in addition to the ones above:

* An Amazon Web Services account
* An Azure Storage account
* Kakadu (KakaduProcessorTest)

## Build the website

The website is built using [Jekyll](http://jekyllrb.com/). With that installed,
run `jekyll serve` from the `website` directory. Then, open
[http://localhost:4000/cantaloupe/](http://localhost:4000/cantaloupe/) in a
web browser. Changes to the HTML files will be reloaded automatically.

An effort is made to keep the documentation in sync with main-branch
development. The "Upgrading" and "Changes" sections in the above website are
usually current.

## Contributing

Contributions are welcome. The suggested process for contributing code changes
is:

1. Submit a "heads-up" issue in the tracker, ideally before beginning any
   work.
2. [Create a fork.](https://github.com/medusa-project/cantaloupe/fork)
3. Create a feature branch (git checkout -b feature/my-new-feature).
4. Make your changes.
5. Commit your changes (`git commit -am 'Add some feature').
6. Push the branch (git push origin feature/my-new-feature).
7. Create a pull request.

## Other Notes

### Versioning

Cantaloupe roughly uses semantic versioning. Major releases (n) involve major
rearchitecting that breaks backwards compatibility in a significant way. Minor
releases (n.n) either do not break compatibility, or only in a minor way.
Patch releases (n.n.n) are for bugfixes only.

### Branching

Cantaloupe uses the
[Gitflow](https://www.atlassian.com/git/tutorials/comparing-workflows#gitflow-workflow)
branching model. `develop` is the main branch that tracks the current state of
the next non-bugfix release. Significant features branch off into
feature branches (`feature/xxxx`), from which they can be integrated into a
particular release.

When a major or minor version is ready for release in `develop`, it branches
off into a new `release/n.n` branch. There, its version is formally set and it
is then merged into `master`, where the release is tagged.

Bugfixes that would increment a minor version of a release are applied to the
release branch for that release, and merged back into develop.

### Configuration keys

Different application versions may require different configuration file keys.
If you are switching between versions, it's good practice to use a dedicated
configuration file for each version. Keys are documented on the website
through the latest release. If you are using a newer build, such as a
snapshot on the develop branch, newer keys will be documented in
`website/upgrade.html`.

## Releasing

(This section is for the release manager only.)

The release process consists of the following steps:

1. Run the Maven Verifier plugin (`mvn verify -DskipTests=true`)
2. Run an OWASP dependency check (`mvn org.owasp:dependency-check-maven:check`)
3. Finalize the code to be released, addressing any relevant milestone issues,
   TODOs, etc.
4. Ensure that the tests are current, comprehensive, and passing
5. Finalize the documentation, including the website, user manual, and change
   log
6. Merge into `release/vX.X`
7. Update the version in `pom.xml` and commit this change
8. Merge into `master`
9. Create the release `.zip` archive with `mvn clean package`
10. Verify that the `.zip` archive is as expected
11. Tag the release: `git tag -a v{version} -m 'Tag v{version}'`
12. Push the code: `git push origin master; git push origin release/x.x;
    git push --tags`
13. Add the `.zip` archive and change log info to the release tag on GitHub
14. Deploy the updated website using `build/deploy_website.rb`
15. Append `-SNAPSHOT` to the version in `pom.xml` and commit this change

## License

Cantaloupe is open-source software distributed under the University of
Illinois/NCSA Open Source License; see the file LICENSE for terms.
