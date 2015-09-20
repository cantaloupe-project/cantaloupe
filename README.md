# 🍈 Cantaloupe

*Extensible [IIIF Image API 2.0](http://iiif.io) image server in Java*

Home: [https://github.com/medusa-project/cantaloupe]
(https://github.com/medusa-project/cantaloupe)

# Features

* Simple
* Self-contained
* Easy to install and get working
* Pluggable resolvers for filesystem and HTTP sources
* Pluggable processors to support a wide variety of source image formats

## What It Doesn't Do

* Write log files. This may come in a future version. In the meantime,
  log messages go to standard output.

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

    # Image processors to use for various source formats. Available values are
    # `ImageIoProcessor`, `GraphicsMagickProcessor`, `ImageMagickProcessor`,
    # and `JaiProcessor`.
    # These extension-specific definitions are optional.
    processor.jpg = ImageIoProcessor
    processor.tif = GraphicsMagickProcessor
    # For any formats not assigned above, fall back to a general-purpose
    # processor. (This is NOT optional.)
    processor.fallback = ImageMagickProcessor

    # Optional; overrides the PATH
    #GraphicsMagickProcessor.path_to_binaries = /usr/local/bin

    # Optional; overrides the PATH
    #ImageMagickProcessor.path_to_binaries = /usr/local/bin

    # JPEG output quality. Should be a number between 0-1 ending in "f"
    ImageIoProcessor.jpg.quality = 0.7f

    # The resolver that translates the identifier in the URL to an image source.
    # Available values are `FilesystemResolver` and `HttpResolver`.
    resolver = FilesystemResolver

    # Server-side path that will be prefixed to the identifier in the request
    # URL.
    FilesystemResolver.path_prefix = /home/myself/images
    # Server-side path or extension that will be suffixed to the identifier
    # in the request URL.
    FilesystemResolver.path_suffix =
    # Normally, slashes in an identifier must be percent-encoded as "%2F". If
    # your web stack can't deal with this, you can define an alternate character
    # or character sequence to represent a path separator. Supply the non-
    # percent-encoded version here, and use the percent-encoded version in IIIF
    # request URLs.
    #FilesystemResolver.path_separator =

    # URL that will be prefixed to the identifier in the request URL.
    HttpResolver.url_prefix = http://localhost/images/
    # Path, extension, query string, etc. that will be suffixed to the
    # identifier in the request URL.
    HttpResolver.url_suffix =
    # Normally, slashes in an identifier must be percent-encoded as "%2F". If
    # your web stack can't deal with this, you can define an alternate character
    # or character sequence to represent a path separator. Supply the non-
    # percent-encoded version here, and use the percent-encoded version in IIIF
    # request URLs.
    #HttpResolver.path_separator =
    # Used for HTTP Basic authentication.
    HttpResolver.username =
    HttpResolver.password =
    
    cache.enabled = false
    FilesystemCache.pathname = /var/cache/cantaloupe

(The above sample contains all available keys.)

# Running

From the command prompt, simply run:

`$ java -Dcantaloupe.config=/path/to/cantaloupe.properties -jar Cantaloupe-x.x.x.jar`

It is now ready for use at: `http://localhost:{http.port}/iiif`

To see information about an image, visit:
`http://localhost:{http.port}/iiif/{image filename}/info.json`

To see the image itself, try:
`http://localhost:{http.port}/iiif/{image filename}/full/full/0/default.jpg`

# Technical Overview

Cantaloupe features a configurable processing pipeline based on resolvers, that
locate source images; and processors, that transform them according to the
parameters in an image request.

## Resolvers

Resolvers locate a source image based on the identifier in an IIIF URL. In
Java-speak, they take in an identifier and return an `ImageInputStream` object
from which the corresponding image can be read by a `Processor`.

### FilesystemResolver

FilesystemResolver maps an identifier from an IIIF URL to a filesystem path,
for retrieving images on a local or attached filesystem. This is theoretically
the fastest resolver, as it returns a `FileImageInputStream`, which supports
arbitrary seeking.

For files with extensions that are missing or unrecognized, this resolver will
check the "magic number" to determine type, which will add some overhead. It
is therefore slightly more efficient to serve files with extensions.

#### Prefix and Suffix

The `FilesystemResolver.path_prefix` and `path_suffix` configuration options
are optional and intended to make URLs shorter, more stable, and hide
potentially sensitive information. For example, with these options set as such:

    FilesystemResolver.path_prefix = /usr/local/images/
    FilesystemResolver.path_suffix =

An identifier of `image.jpg` in the IIIF URL will resolve to
`/usr/local/images/image.jpg`.

#### Path Separator

The IIIF Image API 2.0 spec mandates that identifiers in URLs be
percent-encoded, which changes slashes to `%2F`. Unfortunately, some web
servers have issues dealing with this. If this affects you, you can use the
`FilesystemResolver.path_separator` option to use an alternate string as a path
separator in a URL. (Be sure to supply the non-percent-encoded string, and then
percent-encode it in URLs if it contains any URL-unsafe characters.)

### HttpResolver

HttpResolver maps an identifier from an IIIF URL to an HTTP resource. Image
access via HTTP is likely to be slower than via a filesystem, though this
varies depending on the relative performance of both, as well as the particular
source image format and processor.

It is preferable to use this resolver with source images with recognizable file
extensions. For images with an extension that is missing or unrecognizable, it
will issue an HTTP HEAD request to the server to check the `Content-Type`
header. If the type cannot be inferred from that, an HTTP 415 response will be
returned.

#### Prefix and Suffix

The `HttpResolver.url_prefix` and `url_suffix` configuration options are
optional and intended to make URLs shorter, more stable, and hide potentially
sensitive information. For example, with these options set as such:

    HttpResolver.url_prefix = http://example.org/images/
    HttpResolver.url_suffix =

An identifier of `image.jpg` in the IIIF URL will resolve to
`http://example.org/images/image.jpg`.

#### Path Separator

The IIIF Image API 2.0 spec mandates that identifiers in URLs be
percent-encoded, which changes slashes to `%2F`. Unfortunately, some web
servers have issues dealing with this. If this affects you, you can use the
`HttpResolver.path_separator` option to use an alternate string as a path
separator in a URL. (Be sure to supply the non-percent-encoded string, and then
percent-encode it in URLs if it contains any URL-unsafe characters.)

## Processors

Cantaloupe can use different image processors for different source formats.
Assignments are made in the config file. (See the Configuration section above.)
Currently, the available processors are:

* ImageIoProcessor
* JaiProcessor
* GraphicsMagickProcessor
* ImageMagickProcessor

In terms of format support, a distinction is made between the concepts of
source format and output format, and furthermore, available output formats may
differ depending on the source format.

Supported source formats depend on the processor, and maybe installed
libraries/delegates, etc., as well. Lists of these are displayed on the
landing page, at `/iiif`. A list of output formats supported *for a given
source format* is contained within the response to an information request.

### ImageIoProcessor

ImageIoProcessor uses the Java ImageIO framework to load and process images in
a native-Java way.

ImageIO, as its name implies, is simply an I/O interface that does not care 
about image formats, and therefore the list of formats supported by this
processor varies depending on the codec JARs available in the classpath.
[ImageIO-Ext](https://github.com/geosolutions-it/imageio-ext/) by GeoSolutions
is bundled in to improve the format support.

ImageIoProcessor buffers entire source images in RAM, and is therefore
memory-intensive. Large amounts of RAM and fast storage help.

### JaiProcessor

Java Advanced Imaging (JAI) is a powerful low-level imaging framework
developed by Sun Microsystems beginning in the late nineties. JaiProcessor uses
an updated fork called [JAI-EXT](https://github.com/geosolutions-it/jai-ext).

JaiProcessor's main advantage (see below for disclaimer) is that it can exploit
JAI's internal tiling engine, which makes it capable of region-of-interest
decoding with some formats.

JaiProcessor can read and write the same formats as ImageIoProcessor.

*Note: JaiProcessor is very much experimental at this time. Stand by with
CTRL+C if you don't want to melt your CPU.*

### GraphicsMagickProcessor

GraphicsMagickProcessor uses [im4java](http://im4java.sourceforge.net) to
fork out to the [GraphicsMagick](http://www.graphicsmagick.org) executable
(`gm`). As such, GraphicsMagick must be installed.

GraphicsMagick produces high-quality output and supports all of the IIIF
transforms and all IIIF output formats (assuming the necessary libraries are
installed; see [Supported Formats](http://www.graphicsmagick.org/formats.html)).

GraphicsMagickProcessor is a good fallback processor, as it supports a wide
range of source formats and is generally faster than ImageMagickProcessor.

GraphicsMagickProcessor buffers entire source images in RAM, and is therefore
memory-intensive. Large amounts of RAM and fast storage help.

*Note: due to a quirk in im4java, ImageMagick has to be installed for this
processor to work. (The `identify` command is used to get image dimensions.)
Eliminating this dependency is on the to-do list.*

### ImageMagickProcessor

ImageMagickProcessor, like GraphicsMagickProcessor, also uses
[im4java](http://im4java.sourceforge.net) to wrap [ImageMagick]
(http://www.imagemagick.org/) commands. As such, ImageMagick must be installed.

ImageMagick produces high-quality output and supports all of the IIIF
transforms and all IIIF output formats, assuming the necessary delegates are
installed. It also supports a wide array of source formats.

ImageMagick is not known for being particularly fast or efficient, but it is
generally usable. Large amounts of RAM and fast storage help. If you can find or
compile a "Q8" version of ImageMagick, its memory use will be halved.

### Ideas for Future Processors

* KakaduProcessor (using `kdu_expand`)
* [VipsProcessor](http://www.vips.ecs.soton.ac.uk/index.php?title=VIPS) using
  JNI
* [GdalProcessor](http://gdal.org) (see also
  [ImageIO-Ext](https://github.com/geosolutions-it/imageio-ext))
* [CommonsImagingProcessor](https://commons.apache.org/proper/commons-imaging/)

# Notes on Source Formats

(Feel free to add in your own notes, and send a pull request. Controlled
benchmarks are also welcome.)

## BMP

Probably no one is going to care about this source format. Too bad, because it
is blazing fast with ImageIoProcessor. Now you can serve your Microsoft Paint
drawings faster than ever before.

## JPEG2000

GraphicsMagick can read/write JPEG2000 using JasPer, and ImageMagick using
OpenJPEG. Both of these are extremely slow.

Cantaloupe bundles the [ImageIO-Ext]
(https://github.com/geosolutions-it/imageio-ext) library, which adds support
for several codecs in addition to the ones bundled with the JRE -- JPEG2000
among them. Bundled along with that is imageio-ext-kakadu, which ought to be
able to interface with Kakadu via the `kdu_jni.dll` (Windows) or `kdu_jni.so`
(Linux) library. The author lacks access to this library and is unable to test
against it, so this feature probably doesn't work in Cantaloupe yet.

Years ago, Sun published platform-native JAI JPEG2000 accelerator JARs for
Windows, Linux, and Solaris, which improved performance from dreadful to merely
poor. It is unknown what has happened to these amidst the flotsam of the old
Sun Java web pages on the Oracle website, but in any case, they probably
wouldn't help enough for this application.

## TIFF

GraphicsMagickProcessor and ImageMagickProcessor can both handle TIFF if the
necessary delegate or plugin is installed. (See the landing page, at `/iiif`.)

ImageIoProcessor can read and write TIFF thanks to the bundled [ImageIO-Ext]
(https://github.com/geosolutions-it/imageio-ext) library. ZIP-compressed TIFFs
are not supported.

JaiProcessor is able to load TIFF images in tiles, which makes it pretty fast
with this format.

# Custom Development

If adding your own resolver or processor, feel free to add new configuration
keys to the properties file. They should be in the form of
`NameOfMyClass.whatever`. They can then be accessed via
`e.i.l.cantaloupe.Application.getConfiguration()`.

## Adding Custom Resolvers

A custom resolver needs to implement the `e.i.l.cantaloupe.resolver.Resolver`
interface. Then, to use it, set `resolver` in your properties file to its name.

See one of the existing resolvers for examples.

## Adding Custom Image Processors

Custom processors can be added by implementing the
`e.i.l.cantaloupe.processor.Processor` interface. See the interface
documentation for details and the existing implementations for examples.

Another option would be to add an ImageIO format plugin instead of a custom
processor, and then use ImageIoProcessor or JaiProcessor with it.

## Contributing Code

1. Fork it (https://github.com/medusa-project/cantaloupe/fork)
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create a new Pull Request

# Feedback

Ideas, suggestions, feature requests, bug reports, and other kinds of feedback
are welcome; please [contact the author](mailto:alexd@illinois.edu), or submit
an issue or pull request.

# License

Cantaloupe is open-source software distributed under the University of
Illinois/NCSA Open Source License; see the file LICENSE for terms.
