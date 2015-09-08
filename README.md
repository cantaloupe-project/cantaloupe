# 🍈 Cantaloupe

*Extensible [IIIF 2.0](http://iiif.io) image server in Java*

# Features

* Simple
* Easy to get working
* Pluggable resolvers for filesystem and HTTP sources
* Pluggable processors for different source image formats

## What It Doesn't Do

* Caching. This would be made redundant by a caching reverse proxy like
  Varnish, which can do a better job anyway.

# Requirements

The only hard requirement is JRE 7+. Additional requirements depend on your
choice of processor; see the Processors section below.

# Configuration

Create a file called `cantaloupe.properties` anywhere on disk and paste into
it the following contents, modifying as desired:

    # TCP port to bind the web server to.
    http.port = 8182

    # Helpful in development
    print_stack_trace_on_error_pages = true

    # The image processor to use for various source formats. Available values
    # are `ImageIoProcessor`, `GraphicsMagickProcessor`, and
    # `ImageMagickProcessor`.
    processor.jp2 = ImageMagickProcessor
    processor.jpg = ImageIoProcessor
    processor.tif = ImageMagickProcessor
    # Fall back to a general-purpose processor that supports just about
    # everything.
    processor.fallback = ImageMagickProcessor

    # Optional; overrides the PATH
    GraphicsMagickProcessor.path_to_binaries = /usr/local/bin

    # Optional; overrides the PATH
    ImageMagickProcessor.path_to_binaries = /usr/local/bin
    
    # The path resolver that translates the identifier in the URL to a path.
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

`$ java -jar Cantaloupe-x.x.x.jar -Dcantaloupe.config=/path/to/cantaloupe.properties`

It is now ready for use at: `http://localhost:{http.port}/iiif`

# Technical Overview

## Resolvers

Resolvers locate a source image based on the identifier in an IIIF URL. In
Java-speak, they take in an identifier and return either a File or an
InputStream from which the corresponding image can be read by a processor.

### FilesystemResolver

FilesystemResolver maps an identifier from an IIIF URL to a filesystem path,
for retrieving local images.

### HttpResolver

HttpResolver maps an identifier from an IIIF URL to some other URL, for
retrieving images from a web server.

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
libraries/delegates, etc, as well. In the case of ImageMagickProcessor, the
list is compiled from the output of the `identify -list format` command, which
tells it what to return in its `getSupportedSourceFormats()` and
`getAvailableOutputFormats(SourceFormat)` methods.

The list of supported source formats (source formats for which there are any
output formats) for each processor is displayed on the landing page, at
`/iiif`. The list of output formats supported *for a given source format* is
contained within the response to an information request. 

### ImageIoProcessor

ImageIoProcessor uses the Java ImageIO framework to load and operate on
BufferedImages.

ImageIO, as its name implies, is simply an I/O interface that does not care 
about image formats, and therefore the list of formats supported by this
processor varies depending on the codec jars available in the classpath. By
default, it is minimal -- typically something like JPEG, GIF, BMP, and PNG.

Dropping [this JAI ImageIO jar]
(http://maven.geotoolkit.org/javax/media/jai_imageio/1.1/) into the classpath
will enable some other formats, including TIFF and a dog-slow JPEG2000. Also
see the following links:

* [https://github.com/jai-imageio/jai-imageio-core]
  (https://github.com/jai-imageio/jai-imageio-core)
* [https://github.com/geosolutions-it/imageio-ext/]
  (https://github.com/geosolutions-it/imageio-ext/)
* [https://github.com/jai-imageio/jai-imageio-jpeg2000]
  (https://github.com/jai-imageio/jai-imageio-jpeg2000)

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
(http://www.imagemagick.org/) commands.

ImageMagick produces high-quality output and supports all of the IIIF
transforms and all IIIF output formats (assuming the WebP, JPEG2000, and
PDF delegates are installed). It also supports a wide array of source formats.

ImageMagick is not known for being particularly fast or efficient. Large
amounts of RAM and fast storage help.

## Request/Response Sequence of Events

### Image Request

1. Restlet framework receives request and hands it off to ImageResource
2. Extract parameters from the URL
3. Obtain a resolver from the ResolverFactory, which consults the
   `resolver` key in the config file
4. Using the resolver, obtain a File or InputStream from which the source image
   can be read
5. Obtain a processor appropriate for the source format
6. Processor processes the image based on the URL parameters
7. Processor writes the processed image to the response OutputStream

### Information Request

1. Restlet framework receives request and hands it off to InformationResource
2. Extract identifier from the URL
3. Obtain a resolver from the ResolverFactory, which consults the
   `resolver` key in the config file
4. Using the resolver, obtain a File or InputStream from which the source image
   can be read
5. Obtain a processor appropriate for the source format
6. Get an ImageInfo object from the processor
7. Serialize it to JSON and return it in the response

# Notes on Source Formats

(Feel free to contact the author with your own notes.)

## JPEG2000

GraphicsMagick can read/write JPEG2000 files using JasPer, and ImageMagick
using OpenJPEG. Both of these are very slow.

Another JPEG2000 implementation exists in the Java Advanced Imaging (JAI)
library, although it isn't much of an improvement over the above. See the
ImageIoProcessor section for a link to a jar.

Work is in progress on a KakaduProcessor.

# Custom Development

## Adding Custom Resolvers

Resolvers are easy to write: all you have to do is write a class that
implements the
`e.i.l.cantaloupe.resolver.Resolver` interface. Then, to use it,
set `resolver` in your properties file to its name.

Feel free to add new configuration keys to the properties file. They should
be in the form of `NameOfMyResolver.whatever`. They can then be accessed
via `e.i.l.cantaloupe.Application.getConfiguration()`.

See one of the existing resolvers for examples.

## Adding Custom Image Processors

Processors implement the `e.i.l.cantaloupe.processor.Processor`
interface. See the interface documentation for details and the existing
implementations for examples.

## Contributing Code

1. Fork it (https://github.com/medusa-project/cantaloupe/fork)
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create a new Pull Request

# Feedback

Ideas, suggestions, feature requests, bug reports, and so on are welcome;
please [contact the author](mailto:alexd@illinois.edu) or, better yet, create
an issue.

# License

Cantaloupe is open-source software distributed under the University of
Illinois/NCSA Open Source License; see the file LICENSE for terms.
