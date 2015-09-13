# 🍈 Cantaloupe

*Extensible [IIIF Image API 2.0](http://iiif.io) image server in Java*

Home: [https://github.com/medusa-project/cantaloupe]
(https://github.com/medusa-project/cantaloupe)

# Features

* Simple
* Self-contained
* Easy to get working
* Pluggable resolvers for filesystem and HTTP sources
* Pluggable processors to support a wide variety of source image formats

## What It Doesn't Do

* Caching. The current thinking is that this would add complexity and be made
  redundant by a caching HTTP proxy, which can do a better job anyway.

# Requirements

The only hard requirement is JRE 7+. Additional requirements depend on the
processor(s) being used; see the Processors section below.

# Configuration

Create a file called `cantaloupe.properties` anywhere on disk and paste into
it the following contents, modifying as desired:

    # TCP port to bind the web server to.
    http.port = 8182

    # Helpful in development
    print_stack_trace_on_error_pages = true

    # Image processor to use for various source formats. Available values are
    # `ImageIoProcessor`, `GraphicsMagickProcessor`, and `ImageMagickProcessor`.
    # These definitions are optional.
    processor.jp2 = ImageMagickProcessor
    processor.jpg = ImageIoProcessor
    processor.tif = ImageMagickProcessor
    # For any formats not assigned above, fall back to a general-purpose
    # processor.
    processor.fallback = ImageMagickProcessor

    # Optional; overrides the PATH
    GraphicsMagickProcessor.path_to_binaries = /usr/local/bin

    # Optional; overrides the PATH
    ImageMagickProcessor.path_to_binaries = /usr/local/bin

    # JPEG output quality. Should be a number between 0-1 ending in "f"
    ImageIoProcessor.jpg.quality = 0.7f

    # The resolver that translates the identifier in the URL to an image source.
    # Available values are `FilesystemResolver` and `HttpResolver`.
    resolver = FilesystemResolver

    # The server-side path that will be prefixed to the identifier in the
    # request URL.
    FilesystemResolver.path_prefix = /home/myself/images
    # The server-side path or extension that will be suffixed to the identifier
    # in the request URL.
    FilesystemResolver.path_suffix =
    
    # The URL that will be prefixed to the identifier in the request URL.
    HttpResolver.url_prefix = http://localhost/images/
    # The path, extension, query string, etc. that will be suffixed to the
    # identifier in the request URL.
    HttpResolver.url_suffix =
    # Used for HTTP Basic authentication
    HttpResolver.username =
    HttpResolver.password =

(The above sample contains all available keys.)

# Running

From the command prompt, simply run:

`$ java -Dcantaloupe.config=/path/to/cantaloupe.properties -jar Cantaloupe-x.x.x.jar`

It is now ready for use at: `http://localhost:{http.port}/iiif`

# Technical Overview

Cantaloupe has a configurable processing pipeline based on resolvers, that
locate source images; and processors, that transform them according to the
parameters in an image request.

## Resolvers

Resolvers locate a source image based on the identifier in an IIIF URL. In
Java-speak, they take in an identifier and return either a `File` or an
`InputStream` object from which the corresponding image can be read by a
processor.

### FilesystemResolver

FilesystemResolver maps an identifier from an IIIF URL to a filesystem path,
for retrieving local images.

For files with extensions that are missing or unrecognized, this resolver will
check the "magic number" to determine type, which will add some overhead. It
is therefore slightly more efficient to serve files with extensions.

The `FilesystemResolver.path_prefix` and `path_suffix` configuration options
are optional and intended to make URLs shorter, more stable, and hide
potentially sensitive information. For example, with these options set as such:

FilesystemResolver.path_prefix = /usr/local/images/
FilesystemResolver.path_suffix =

An identifier of `image.jpg` in the IIIF URL will resolve to
`/usr/local/images/image.jpg`.

### HttpResolver

HttpResolver maps an identifier from an IIIF URL to some other URL, for
retrieving images from a web server.

It is preferable to use this resolver with source images with recognizable file
extensions. For images with an extension that is missing or unrecognizable, it
will issue an HTTP HEAD request to the server to check the Content-Type header.
If the type cannot be inferred from that, the image won't be processable.

The `HttpResolver.url_prefix` and `url_suffix` configuration options are
optional and intended to make URLs shorter, more stable, and hide potentially
sensitive information. For example, with these options set as such:

HttpResolver.url_prefix = http://example.org/images/
HttpResolver.url_suffix =

An identifier of `image.jpg` in the IIIF URL will resolve to
`http://example.org/images/image.jpg`.

## Processors

Cantaloupe can use different image processors, each of which can be assigned to
particular source formats via the config file (see the Configuration section
above). Currently, the available processors are:

* ImageIoProcessor
* GraphicsMagickProcessor
* ImageMagickProcessor

In terms of format support, a distinction is made between the concepts of
source formats and output formats, and furthermore, available output formats
may differ depending on the source format.

Supported source formats depend on the processor, and maybe installed
libraries/delegates, etc., as well. Lists of these are displayed on the
landing page, at `/iiif`. A list of output formats supported *for a given
source format* is contained within the response to an information request.

### ImageIoProcessor

ImageIoProcessor uses Java ImageIO framework to load and process images in a
native-Java way.

ImageIO, as its name implies, is simply an I/O interface that does not care 
about image formats, and therefore the list of formats supported by this
processor varies depending on the codec JARs available in the classpath.
(JAI-EXT)[https://github.com/geosolutions-it/jai-ext] by GeoSolutions is
bundled in to improve the versatility of this processor.

### GraphicsMagickProcessor

GraphicsMagickProcessor uses [im4java](http://im4java.sourceforge.net) to
fork out to the [GraphicsMagick](http://www.graphicsmagick.org) executable
(`gm`). As such, GraphicsMagick must be installed.

GraphicsMagick produces high-quality output and supports all of the IIIF
transforms and all IIIF output formats (assuming the necessary libraries are
installed; see [Supported Formats](http://www.graphicsmagick.org/formats.html)).

GraphicsMagickProcessor is a good fallback processor, as it supports a wide
range of source formats and is generally faster than ImageMagickProcessor.

*Note: due to a quirk in im4java, ImageMagick has to be installed for this
processor to work. (The `identify` command is used to get image dimensions.)
Eliminating this dependency is on the to-do list.*

### ImageMagickProcessor

ImageMagickProcessor, like GraphicsMagickProcessor, also uses
[im4java](http://im4java.sourceforge.net) to wrap [ImageMagick]
(http://www.imagemagick.org/) commands. As such, ImageMagick must be installed.

ImageMagick produces high-quality output and supports all of the IIIF
transforms and all IIIF output formats (assuming the WebP, JPEG2000, and
PDF delegates are installed). It also supports a wide array of source formats.

ImageMagick is not known for being particularly fast or efficient. Large
amounts of RAM and fast storage help.

# Notes on Source Formats

(Feel free to add in your own notes, and send a pull request.)

## JPEG2000

GraphicsMagick can read/write JPEG2000 using JasPer, and ImageMagick using
OpenJPEG. Both of these are unusably slow.

Cantaloupe includes the (JAI-EXT)[https://github.com/geosolutions-it/jai-ext]
library, which adds support for several codecs in addition to the few supported
natively by ImageIO -- JPEG2000 among them. Unfortunately, this implementation
is not much of an improvement over the above.

Apparently, platform-native adapters are available for the JPEG2000 codec that
improve its performance. These are quite old and are available only for Windows
and Linux, and maybe not current versions. (The author has not looked into it.)

There does appear to be an [ImageIO Kakadu adapter]
(https://github.com/geosolutions-it/imageio-ext/) available. Please report
back if you have any experience with it.

# Custom Development

## Adding Custom Resolvers

A custom resolver needs to implement the `e.i.l.cantaloupe.resolver.Resolver`
interface. Then, to use it, set `resolver` in your properties file to its name.

Feel free to add new configuration keys to the properties file. They should
be in the form of `NameOfMyResolver.whatever`. They can then be accessed
via `e.i.l.cantaloupe.Application.getConfiguration()`.

See one of the existing resolvers for examples.

## Adding Custom Image Processors

Custom processors can be added by implementing the
`e.i.l.cantaloupe.processor.Processor` interface. See the interface
documentation for details and the existing implementations for examples.

A better but more difficult option would be to add an ImageIO format plugin
instead of a custom processor, and then use ImageIoProcessor with it.

## Contributing Code

1. Fork it (https://github.com/medusa-project/cantaloupe/fork)
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create a new Pull Request

# Feedback

Ideas, suggestions, feature requests, bug reports, and other kinds of feedback
are welcome; please [contact the author](mailto:alexd@illinois.edu).

# License

Cantaloupe is open-source software distributed under the University of
Illinois/NCSA Open Source License; see the file LICENSE for terms.
