# 🍈 Cantaloupe

*[IIIF 2.0](http://iiif.io) image server in Java*

# Features

* Simple
* Easy to get working
* Pluggable resolvers for filesystem and HTTP sources
* Pluggable image processors

## What It Doesn't Do

* Cache anything. It may or may not do this some day. In the meantime, a
  caching reverse proxy would be an option.
* Write log files. Log messages go to standard output.
* Run inside an application server. ([Rationale]
  (http://johannesbrodwall.com/2010/03/08/why-and-how-to-use-jetty-in-mission-critical-production/))

# Requirements

* JRE 6+
* ImageMagick with PDF and JPEG2000 delegates (see the ImageMagickProcessor
  section below for more information)

# Configuration

Create a file called `cantaloupe.properties` anywhere on disk and paste into
it the following contents, editing it as necessary for your site:

    # The HTTP port to bind to.
    http.port = 8182

    # To help in debugging
    print_stack_trace_on_error_page = true

    # The image processor to use. The only available value is
    # `ImageMagickProcessor`.
    # Note that the `convert` and `identify` binaries must be in the PATH.
    processor = ImageMagickProcessor
    
    # The path resolver that translates the identifier in the URL to a path.
    # Available values are `FilesystemResolver` and `HttpResolver`.
    resolver = FilesystemResolver
    
    # The server-side path that will be prefixed to the identifier in the request
    # URL.
    FilesystemResolver.path_prefix = /home/myself/images
    
    # The server-side path or extension. that will be suffixed to the identifier
    # in the request URL.
    FilesystemResolver.path_suffix =
    
    # The URL that will be prefixed to the identifier in the request URL.
    HttpResolver.url_prefix = http://localhost/~alexd/
    
    # The path, extension, query string, etc. that will be suffixed to the
    # identifier in the request URL.
    HttpResolver.url_suffix =

# Running

Run it like:

`$ java -jar Cantaloupe-x.x.x.jar -Dcantaloupe.config=/path/to/cantaloupe.properties`

It is now available at: `http://localhost:{http.port}/iiif/`

# Processors

Cantaloupe can use different image processors. Currently, it includes only the
ImageMagickProcessor.

## ImageMagickProcessor

ImageMagickProcessor uses [im4java](http://im4java.sourceforge.net), which
forks out to the ImageMagick `convert` and `identify` shell commands. These
must be in the PATH.

ImageMagick produces high quality output and supports all of the IIIF
transforms and all IIIF output formats except WebP.

ImageMagick is not known for being particularly fast or efficient. Performance
degrades and memory usage increases as image size increases. Large amounts of
RAM and fast storage help.

# Resolvers

## FilesystemResolver

FilesystemResolver maps an identifier from an IIIF URL to a filesystem path
for retrieving images from a local filesystem.

### Example With Prefix Only

Given the following configuration options:

* `FilesystemResolver.path_prefix = /data/images`
* `FilesystemResolver.path_suffix = `

And the following URL:

* http://example.org/iiif/image.jpg/full/full/0/default.jpg

FilesystemResolver will look for an image located at
/data/images/image.jpg.

### Example With Prefix and Suffix

Given the following configuration options:

* `FilesystemResolver.path_prefix = /data/images`
* `FilesystemResolver.path_suffix = /image.jp2`

And the following URL:

* http://example.org/iiif/some-uuid/full/full/0/default.jpg

FilesystemResolver will look for an image located at
/data/images/some-uuid/image.jp2.

## HttpResolver

HttpResolver maps an identifier from an IIIF URL to some other URL, for
retrieving images from a web server.

### Example With Prefix Only

Given the following configuration options:

* `HttpResolver.url_prefix = http://localhost/images/`
* `HttpResolver.url_suffix = `

And the following URL:

* http://example.org/iiif/image.jpg/full/full/0/default.jpg

HttpResolver will look for an image located at
http://localhost/images/image.jpg.

### Example With Prefix and Suffix

Given the following configuration options:

* `HttpResolver.url_prefix = http://localhost/images/`
* `HttpResolver.url_suffix = /image.jp2`

And the following URL:

* http://example.org/iiif/some-uuid/full/full/0/default.jpg

HttpResolver will look for an image located at
http://localhost/images/some-uuid/image.jp2.

# Feedback

Ideas, suggestions, requests, and so on are welcome; please [contact the
author](mailto:alexd@illinois.edu).

# Custom Development

## Adding Custom Resolvers

Resolvers are easy to write. All you have to do is implement the one method
in the `edu.illinois.library.cantaloupe.resolver.Resolver` interface.

To use your custom resolver, set `resolver` in your properties file to its
class name.

Feel free to add new configuration keys to the properties file. They should
be in the form of `NameOfMyResolverClass.whatever`. They can then be accessed
via `edu.illinois.library.cantaloupe.Application.getConfiguration()`.

See one of the existing resolvers for examples.

## Adding Custom Image Processors

TODO: write this

## Contributing Code

1. Fork it (https://github.com/medusa-project/cantaloupe/fork)
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create a new Pull Request

# License

Cantaloupe is open-source software distributed under the University of
Illinois/NCSA Open Source License; see the file LICENSE for terms.
