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
* Lean and efficient with no app server or Servlet stack overhead
* Configurable caching
* Optional authentication

# Requirements

The only hard requirement is JRE 7+. Particular processors may have additional
requirements; see the Processors section below.

# Configuration

Create a file called `cantaloupe.properties` anywhere on disk and paste into
it the following contents, modifying as desired:

    # TCP port to bind the web server to.
    http.port = 8182
    
    # Uncomment to require HTTP Basic authentication.
    #http.auth.basic = true
    #http.auth.basic.username = myself
    #http.auth.basic.secret = password

    # Helpful in development
    print_stack_trace_on_error_pages = true

    # Image processors to use for various source formats. Available values are
    # `ImageIoProcessor`, `GraphicsMagickProcessor`, `ImageMagickProcessor`,
    # and `JaiProcessor`.
    # These extension-specific definitions are optional.
    #processor.jpg = ImageMagickProcessor
    #processor.tif = GraphicsMagickProcessor
    # Fall back to this processor for any formats not assigned above.
    processor.fallback = ImageIoProcessor

    # Optional; overrides the PATH
    #GraphicsMagickProcessor.path_to_binaries = /usr/local/bin

    # Optional; overrides the PATH
    #ImageMagickProcessor.path_to_binaries = /usr/local/bin
    
    # Optional; overrides the PATH
    KakaduProcessor.path_to_binaries = /usr/bin

    # JPEG output quality. Should be a number between 0-1 ending in "f"
    ImageIoProcessor.jpg.quality = 0.7f

    # The resolver that translates the identifier in the URL to an image source.

    # Available values are `FilesystemResolver` and `HttpResolver`.
    resolver = FilesystemResolver

    # Server-side path that will be prefixed to the identifier in the URL.
    FilesystemResolver.path_prefix = /home/myself/images
    # Server-side path or extension that will be suffixed to the identifier
    # in the URL.
    FilesystemResolver.path_suffix =
    # Normally, slashes in an identifier must be percent-encoded as "%2F". If
    # your proxy is incapable of passing these through un-decoded, you can
    # define an alternate character or character sequence to represent a path
    # separator. Supply the non-percent-encoded version here, and use the
    # percent-encoded version in IIIF URLs.
    #FilesystemResolver.path_separator =

    # URL that will be prefixed to the identifier in the request URL.
    HttpResolver.url_prefix = http://localhost/images/
    # Path, extension, query string, etc. that will be suffixed to the
    # identifier in the request URL.
    HttpResolver.url_suffix =
    # See FilesystemResolver.path_separator for an explanation of this.
    #HttpResolver.path_separator =
    # Used for HTTP Basic authentication.
    HttpResolver.username =
    HttpResolver.password =

    # Customize the response Cache-Control header. This header may be
    # overridden by proxies. Comment out to disable the Cache-Control header.
    cache.client.max_age = 2592000
    cache.client.shared_max_age =
    cache.client.public = true
    cache.client.private = false
    cache.client.no_cache = false
    cache.client.no_store = false
    cache.client.must_revalidate = false
    cache.client.proxy_revalidate = false
    cache.client.no_transform = true

    # The only available value is `FilesystemCache`. Comment out or set blank
    # to disable caching.
    #cache.server = FilesystemCache

    # If this directory does not exist, it will be created automatically.
    FilesystemCache.pathname = /var/cache/cantaloupe
    # Time before a cached image becomes stale and needs to be reloaded. Set to
    # blank or 0 for "forever"
    FilesystemCache.ttl_seconds = 2592000 # 1 month

(The above sample contains all available keys.)

# Running

From the command prompt:

`$ java -Dcantaloupe.config=/path/to/cantaloupe.properties -jar Cantaloupe-x.x.x.jar`

It is now ready for use at: `http://localhost:{http.port}/iiif`

To see information about an image, visit:
`http://localhost:{http.port}/iiif/{image filename}/info.json`

To see the image itself, try:
`http://localhost:{http.port}/iiif/{image filename}/full/full/0/default.jpg`

# Upgrading

Upgrading is usually just a matter of downloading a new version and running it.
Sometimes there are backwards-incompatible changes to the configuration file
structure, though, so check the change log (near the bottom) to see if there is
anything extra to do.

# Technical Overview

Cantaloupe features a configurable processing pipeline based on:

* Resolvers, that locate source images;
* Processors, that transform them according to the parameters in an image
  request; and
* Caches, that cache generated image tiles.

## Resolvers

Resolvers locate a source image based on the identifier in an IIIF URL. In
Java-speak, they take in an identifier and return an `ImageInputStream` object
from which the corresponding image can be read by a `Processor`.

### FilesystemResolver

FilesystemResolver maps an identifier from an IIIF URL to a filesystem path,
for retrieving images on a local or attached filesystem. This is theoretically
the fastest resolver, as it returns a `FileImageInputStream`, which supports
arbitrary seeking.

For images with extensions that are missing or unrecognized, this resolver will
check the "magic number" to determine type, which will add some overhead. It
is therefore slightly more efficient to serve images with extensions.

#### Prefix and Suffix

The `FilesystemResolver.path_prefix` and `path_suffix` configuration options
are optional and intended to make URLs shorter, more stable, and hide
potentially sensitive information. For example, with these options set as such:

    FilesystemResolver.path_prefix = /usr/local/images/
    FilesystemResolver.path_suffix =

An identifier of `image.jpg` in the URL will resolve to
`/usr/local/images/image.jpg`.

#### Path Separator

The IIIF Image API 2.0 specification mandates that identifiers in URLs be
percent-encoded, which changes slashes to `%2F`. Some reverse-proxy servers
automatically decode `%2F` into `/` before passing on the request. This will
cause HTTP 404 errors in Cantaloupe.

If you are unable to configure your reverse proxy to stop decoding slashes,
the `FilesystemResolver.path_separator` configuration option will enable you to
use an alternate string as a path separator in an identifier. (Be sure to
supply the non-percent-encoded string, and then percent-encode it in URLs if it
contains any URL-unsafe characters.)

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

An identifier of `image.jpg` in the URL will resolve to
`http://example.org/images/image.jpg`.

#### Path Separator

The IIIF Image API 2.0 specification mandates that identifiers in URLs be
percent-encoded, which changes slashes to `%2F`. Some proxy servers
automatically decode `%2F` into `/` before passing on the request. This will
cause HTTP 404 errors in Cantaloupe.

If you are unable to configure your proxy to stop decoding slashes, the
`HttpResolver.path_separator` configuration option will enable you to use an
alternate string as a path separator in an identifier. (Be sure to supply the
non-percent-encoded string, and then percent-encode it in URLs if it contains
any URL-unsafe characters.)

## Processors

Cantaloupe can use different image processors for different source formats.
Assignments are made in the config file. Currently, the available processors
are:

* ImageIoProcessor
* GraphicsMagickProcessor
* ImageMagickProcessor
* JaiProcessor (experimental)

In terms of format support, a distinction is made between the concepts of
source format and output format, and furthermore, available output formats may
differ depending on the source format.

Supported source formats depend on the processor, and maybe installed
libraries/delegates, etc., as well. Lists of these are displayed on the
landing page, at `/iiif`. A list of output formats supported *for a given
source format* is contained within the response to an information request
(`/iiif/{identifier}/info.json`).

### ImageIoProcessor

ImageIoProcessor uses the Java ImageIO framework to load and process images in
a native-Java way. This is a good processor to get started with as it has no
external dependencies and works out-of-the-jar.

ImageIO, as its name implies, is simply an I/O interface that does not care 
about image formats, and therefore the list of formats supported by this
processor varies depending on the codec JARs available in the classpath.

ImageIoProcessor buffers entire source images in RAM, and is therefore
memory-intensive. Large amounts of RAM and fast storage help.

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
quite usable. Large amounts of RAM and fast storage help. If you can find or
compile a "Q8" version of ImageMagick, its memory use will be halved.

### KakaduProcessor

KakaduProcessor uses the `kdu_expand` binary (part of the proprietary Kakadu
toolkit) to efficiently process JPEG2000 source images.

### JaiProcessor

Java Advanced Imaging (JAI) is a powerful low-level imaging framework
developed by Sun Microsystems until around 2006. JaiProcessor uses an updated
fork called [JAI-EXT](https://github.com/geosolutions-it/jai-ext).

JaiProcessor's main theoretical advantage is the ability to exploit JAI's
internal tiling engine, which makes it capable of region-of-interest decoding
with some formats. Unfortunately, JAI is old and buggy and the defects offset
the advantages.

JaiProcessor can read and write the same formats as ImageIoProcessor.

*Note: JaiProcessor is very much experimental at this time. Stand by with
CTRL+C to prevent your CPU from melting.*

### Which Processor Should I Use?

1. Don't use JaiProcessor
2. For each of the source formats you would like to serve, it's a good idea to
experiment with different processors, comparing their performance, resource
usage, and output quality. Benchmark results, if you can contribute them, are
welcome.

## Caches

There is currently one cache, FilesystemCache, which caches generated images
into a filesystem directory. The location of this directory is configurable,
as is the "time-to-live" of the cached tiles. Expired images are replaced the
next time they are requested.

Cantaloupe does not cache entire information response representations -- only
image dimensions, which are the only expensive part to generate. This means it
is possible to change variables that might affect the contents of the
representation (like processors) without having to flush the cache.

### Flushing the Cache

Of course, `rm *` works, but there are other options too:

#### Expired Images Only

Start Cantaloupe with the `-Dcantaloupe.cache.flush_expired` option:

`$ java -Dcantaloupe.config=... -Dcantaloupe.cache.flush_expired -jar Cantaloupe-x.x.x.jar`

#### All Images

Start Cantaloupe with the `-Dcantaloupe.cache.flush` option:

`$ java -Dcantaloupe.config=... -Dcantaloupe.cache.flush -jar Cantaloupe-x.x.x.jar`

# Authentication

Cantaloupe supports HTTP Basic authentication via the `http.auth.basic*` keys
in the configuration file.

# Client-Side Caching

The HTTP/1.1 `Cache-Control` response header is configurable via the
`cache.client.*` keys in the configuration file.

# Notes on Source Formats

(Feel free to add in your own notes, and send a pull request. Controlled
benchmarks are also welcome.)

## BMP

Probably no one is going to care about this source format. Too bad, because it
is blazing fast with ImageIoProcessor. Now you can serve your Microsoft Paint
drawings faster than ever before.

## JPEG

It should be possible to use TurboJPEG (high-level API over [libjpeg-turbo]
(http://www.libjpeg-turbo.org)) with ImageIoProcessor to hardware-accelerate
JPEG coding. To do so, include the [TurboJPEG plugin JAR]
(https://github.com/geosolutions-it/imageio-ext/wiki/TurboJPEG-plugin) in your
classpath, and compile libjpeg-turbo with Java support (using the `--with-java`
configuration option). (The author has not tried this.)

## JPEG2000

GraphicsMagick can read/write JPEG2000 using JasPer, and ImageMagick using
OpenJPEG. Both of these are extremely slow.

KakaduProcessor is, by far, the fastest and most efficient processor for this
format.

Cantaloupe bundles the GeoTools [JP2K Plugin]
(http://docs.geotools.org/latest/userguide/library/coverage/jp2k.html), which
ought to be able to interface with Kakadu. The author hasn't tested this yet.

Years ago, Sun published [platform-native JAI JPEG2000 accelerator JARs]
(http://download.java.net/media/jai/builds/release/1_1_3/INSTALL.html) for
Windows, Linux, and Solaris, which improved performance from dreadful to merely
poor. It is unknown whether these are still available or whether they would
still work on modern platforms.

## TIFF

GraphicsMagickProcessor and ImageMagickProcessor can both handle TIFF if the
necessary delegate or plugin is installed. (See the landing page, at `/iiif`.)

TIFF is disabled in ImageIoProcessor because it is too buggy.

# Custom Development

## Custom Resolvers

A custom resolver needs to implement the `e.i.l.cantaloupe.resolver.Resolver`
interface. Then, to use it, set `resolver` in your properties file to its name.

See one of the existing resolvers for examples.

## Custom Image Processors

Custom processors can be added by implementing the
`e.i.l.cantaloupe.processor.Processor` interface. See the interface
documentation for details and the existing implementations for examples.

Another option would be to add an ImageIO format plugin instead of a custom
processor, and then use ImageIoProcessor or JaiProcessor with it.

## Custom Caches

Custom caches can be added by implementing the `e.i.l.cantaloupe.cache.Cache`
interface. See the interface documentation for details and the existing
implementations for examples.

## Custom Configuration

Feel free to add new configuration keys to the properties file. They should
be in the form of `NameOfMyClass.whatever`. They can then be accessed via
`e.i.l.cantaloupe.Application.getConfiguration()`.

## Contributing Code

1. Fork it (https://github.com/medusa-project/cantaloupe/fork)
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create a new Pull Request

# Change Log

## 1.0-beta3

* `cache` key in the configuration file renamed to `cache.server`.
* Added client-side caching. To enable, add the `cache.client.*` keys from the
  sample configuration above.
* Added HTTP Basic authentication. To enable, add the `http.auth.basic*` keys
  from the sample configuration above.
* Improved the thread-safety of FilesystemCache.

## 1.0-beta2

* Added optional server-side caching. To enable, add the `cache` and
  `FilesystemCache.*` keys from the sample configuration above.
* Added configurable path separators in FilesystemResolver and HttpResolver.
* The application version is displayed on the landing page and in a startup log
  message.

## 1.0-beta1

* First version.

# Feedback

Ideas, suggestions, feature requests, bug reports, and other kinds of feedback
are welcome; [contact the author](mailto:alexd@illinois.edu), or submit an
issue or pull request.

# License

Cantaloupe is open-source software distributed under the University of
Illinois/NCSA Open Source License; see the file LICENSE for terms.
