# 🍈 Cantaloupe

*High-performance dynamic image server in Java*

### The project website is located at:
**[https://medusa-project.github.io/cantaloupe/]
(https://medusa-project.github.io/cantaloupe/)**

This is a very-quick-start guide. The user manual at the website above is
the primary source of documentation.

## Requirements

The only hard requirement is Java 7 or newer--either the
[Oracle JRE](https://www.java.com/en/) or
[OpenJDK](http://openjdk.java.net/install/index.html) will work. Optional
components, such as particular processors, may have additional requirements.

## Configure

The distribution archive bundles a sample configuration file, named
`cantaloupe.properties.sample`. Make a copy and open it in an editor. Set
`FilesystemResolver.BasicLookupStrategy.path_prefix` to a folder path
containing some images, and leave all the other options alone.

## Run

From the command prompt (OS X/Linux):

    java -Dcantaloupe.config=/path/to/cantaloupe.properties -Xmx2g -jar Cantaloupe-x.x.x.war

From the command prompt (Windows):

    java -Dcantaloupe.config=C:\path\to\cantaloupe.properties -Xmx2g -jar Cantaloupe-x.x.x.war

Then, assuming you have an image named `image.tif`, try accessing:

* [http://localhost:8182/iiif/2/image.tif/info.json](http://localhost:8182/iiif/2/image.tif/info.json)
* [http://localhost:8182/iiif/2/image.tif/0,0,200,200/full/0/default.jpg](http://localhost:8182/iiif/2/image.tif/0,0,200,200/full/0/default.jpg)

## License

Cantaloupe is open-source software distributed under the University of
Illinois/NCSA Open Source License; see the file LICENSE for terms.
