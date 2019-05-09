# Change Log

## 5.0

### Endpoints

* The `X-IIIF-ID` reverse proxy header is no longer supported. `X-Forwarded-ID`
  should be used instead.
* The `endpoint.iiif.content_disposition` configuration key has been removed.
  The `?response-content-disposition` query argument should be used instead.

### Sources

* Sources support inconsistencies between filename/identifier extensions and
  byte signatures.

### Processors

* The metadata-handling system has been redesigned:
    * Source image metadata is cached in derivative caches.
    * XMP metadata can be copied or edited using a delegate method.
    * The `processor.metadata.respect_orientation` configuration key has been
      removed and EXIF Orientation values are always respected.
* KakaduNativeProcessor, KakaduDemoProcessor, OpenJpegProcessor, and
  TurboJpegProcessor respect the EXIF `Orientation` tag.
* KakaduNativeProcessor, KakaduDemoProcessor, and OpenJpegProcessor support
  IPTC and XMP metadata.
* TurboJpegProcessor supports EXIF, IPTC, and XMP metadata.
* FfmpegProcessor, KakaduNativeProcessor, OpenJpegProcessor, and
  PdfBoxProcessor use libjpeg-turbo to write JPEGs if it is available.
* ImageMagickProcessor, GraphicsMagickProcessor, and KakaduDemoProcessor have
  been removed.

### Caches

* Added DynamoDBCache.

### Other

* Java 11 is required.
* JRuby has been updated to version 9.2, which supports Ruby 2.5.

## 4.1.2

* Fixed incorrect `@id` values in information responses when a query argument
  is present in the URI.
* Fixed a potential IIOException when reading certain JPEG images with
  JaiProcessor.
* Updated the Jetty dependency to address the following security vulnerability:
  CVE-2019-10247.
* Updated the PDFBox dependency to address the following security
  vulnerability: CVE-2019-0228.

## 4.1.1

* Fixed `NoSuchMethodError`s from various processors when running in Java 8.
* Fixed failure to locate the delegate script when `delegate_script.pathname`
  is set to a relative path outside of the current working directory.
* Fixed a regression in the handling of the `X-Forwarded-Path` header.
* Fixed breakage in manual processor selection from the Control Panel.
* Fixed an error from Java2dProcessor when downscaling TIFF source images to a
  very small size.
* Fixed improper codestream access in KakaduNativeProcessor that could cause
  cause degraded output for certain images.
* Improved resilience when reading certain oddly-encoded GIFs in
  Java2dProcessor and JaiProcessor.

## 4.1

### Configuration

* Configuration values can be obtained from the environment. See the user
  manual for more information.

### Endpoints

* The `redirect()` and `authorized?()` delegate methods have been merged into a
  new `authorize()` method, which enables a superset of their functionality,
  including challenge responses and redirects to "virtual" quality-limited
  versions. Authorization can also be set up to align with the IIIF
  Authentication API 1.0's "all or nothing access" or "tiered access" schemes.
* The `endpoint.public.auth.*` keys that controlled global HTTP Basic
  protection have also been replaced by the `authorize()` delegate method.
* The maximum allowed scale can set in the configuration.
* Running tasks are displayed in the Control Panel.
* Added HTTP API endpoints for status and health checks.
* In the IIIF Image API 2.x endpoint, the `Access-Control-Allow-Origin`
  response header is always included in the information response regardless of
  whether an `Origin` header was sent in the request, in order to align more
  closely with the IIIF Image API.
* Improved handling and documentation of the `X-Forwarded-Port` header.
* The `/iiif` URI path no longer redirects to a specific Image API version, and
  now returns HTTP 404.
* Bug fixes related to cropping-by-percent of EXIF-rotated images.
* During image requests, `authorize()` is called earlier in the setup process,
  for better efficiency.
* Removed the Restlet dependency.

### Sources

* HttpSource, S3Source, and AzureStorageSource are able to request chunks of
  resources on demand, which can improve performance when reading images in
  selectively-readable encodings like JPEG2000 and multi-resolution TIFF.
* HttpSource sends initial `HEAD` requests instead of ranged `GET` requests.
* The hash returned from the `httpsource_resource_info()` delegate method may
  include custom request headers.
* HttpSource's `HttpSource.trust_invalid_certs` configuration key has been
  renamed `HttpSource.allow_insecure`. When enabled, insecure cipher suites are
  accepted as well as insecure certificates.
* S3Source uses a Minio client instead of the AWS Java SDK client, to work
  around a possible thread-starvation bug in the AWS Java SDK when S3Source
  and S3Cache are in use at the same time.

### Processors

* Added configurable processor selection strategies, one of which uses the
  existing `processor.*` configuration keys, and the other of which uses an
  automatic strategy that attempts to choose a "best" processor on a per-
  request basis.
* Added TurboJpegProcessor.
* Added the `repeat` position for image overlays.
* Efficiency improvements in KakaduNativeProcessor.
* KakaduNativeProcessor supports `sizeByDistortedWidthHeight`.
* Java2dProcessor and PdfBoxProcessor can preserve XMP metadata across most
  source and output formats.
* KakaduNativeProcessor respects the `processor.metadata.preserve`
  configuration key for XMP metadata.
* Worked around a bug in the GraphicsMagick `gm` command that causes occasional
  "broken pipe" errors from GraphicsMagickProcessor when reading images from a
  FileSource.
* KakaduDemoProcessor has been deprecated, as it has been made more-or-less
  redundant by KakaduNativeProcessor.
* Updated the PDFBox dependency.
* Removed normalization.

### Caches

* S3Cache uses last-accessed rather than last-modified times.
* Changed the S3Cache and AzureStorageCache key naming schemes to improve
  organization and avoid problems with edge cases.

### Other

* The change log has moved from the website to this file.
* Java 11 is supported.

## 4.0.3

* Fixed a scaling-related regression in PdfBoxProcessor.
* Streams consumed from S3Source are drained fully before closure, which makes
  its connection pool more efficient and fixes a warning-level error message
  from its internal HTTP client.
* Fixed inability to use `ScriptLookupStrategy` with AzureStorageSource.
  (Thanks to @marc-sensenich)
* Fixed a potential math error when cropping certain images with an embedded
  EXIF Orientation tag.
* Improved exception handling in S3Source and S3Cache.
* Fixed inability of S3Cache and AzureStorageCache to recognize a
  `cache.server.derivative.ttl_seconds` key value greater than 2^31.
* Worked around an issue in OpenJPEG that could cause OpenJpegProcessor to fail
  when reading certain images from the source cache.
* Corrected the `gray` quality to `grey` in the IIIF Image API 1.x endpoint.
* Updated the Tika dependency to address the following security vulnerability:
  CVE-2018-17197.
* Updated the Jackson dependency to address several security vulnerabilities.
* Updated the PDFBox dependency.

## 4.0.2

* The exploded application is more reliably cleaned up at shutdown when running
  in standalone mode.
* KakaduNativeProcessor supports JPEG2000-compatible JPX source images.
* Fixed an error from KakaduDemoProcessor in Windows when using a system locale
  that uses characters other than periods as decimal separators.
* Fixed incorrect redaction positioning when scaling.
* Fixed an error from processors that use a Java 2D processing pipeline when
  attempting to scale a source image that is natively less than three pixels in
  either dimension, or has been cropped to that size.
* Fixed an empty response when image overlays are enabled but the overlay
  pathname or URL is invalid. Instead, images are served without the overlay,
  and warnings logged.
* Updated Jetty to version 9.4.12, which fixes a potential connection leak in
  long-running servers when using HttpSource. (Thanks to @giancarlobi)
* Miscellaneous other dependency updates.

## 4.0.1

* Fixed a regression introduced in 3.4.3 that could cause URIs to appear with
  the wrong scheme in information responses and `Link` headers. (Thanks to
  @mmatela and @jweisman)
* Fixed a bug that caused the minimum tile size in IIIF Image API 2.x
  information responses to be 512 regardless of the setting of
  `endpoint.iiif.min_tile_size`.
* Fixed a connection leak in HttpSource.
* Improved the resiliency of the internal JPEG2000 parser used by
  KakaduDemoProcessor and OpenJpegProcessor.
* Fixed KakaduNativeProcessor's handling of `sizeByDistortedWidthHeight`
  requests.
* Removed an error message that would appear in the log when the delegate
  script file was not found but the delegate script was disabled.
* The `X-Forwarded-Host` reverse proxy header may contain a port number, which
  will be used if `X-Forwarded-Port` is not supplied.

## 4.0

### Endpoints

* A new `X-Forwarded-ID` reverse proxy header replaces `X-IIIF-ID`, which has
  been deprecated, and will be removed in a future version.
* Error response representations are in `text/plain` format in the absence of a
  client preference.
* When `endpoint.iiif.content_disposition` is set to `inline`, the
  `Content-Disposition` header value includes a filename-safe version of the
  image identifier.

### Sources

* Resolvers have been renamed sources to better reflect their core function.
* AmazonS3Resolver has been renamed S3Source, and it now supports non-AWS
  endpoints.
* S3Source supports a configurable path prefix and suffix when using
  `BasicLookupStrategy`.
* AzureStorageSource supports shared access signature (SAS) URIs. (Thanks to
  @ddisciascio)
* Improved the efficiency of source cache content access when
  `cache.server.resolve_first = false`.
* All sources fall back to checking the magic bytes in image data if they
  cannot infer a format any other way.

### Processors

* KakaduProcessor has been renamed KakaduDemoProcessor to reflect the fact that
  it interfaces with the Kakadu demo tool, `kdu_expand`, and not the native
  library.
* Added KakaduNativeProcessor, as well as compiled binaries for Linux, macOS,
  and Windows, which are free to use with it for non-commercial purposes.
* Added `DownloadStrategy` as another optional retrieval strategy for
  stream-based processors, and as an optional fallback strategy. This enables
  all processors to work with all sources with no extra configuration.
* JPEG2000-compatible JPX images are readable by all processors that support
  JPEG2000.
* JaiProcessor has been deprecated. See the user manual for more information.
* KakaduDemoProcessor and OpenJpegProcessor work in Windows.
* KakaduDemoProcessor and OpenJpegProcessor read image metadata using custom
  code, which is more efficient than using the respective `kdu_jp2info` and
  `opj_dump` tools, and enables them to read the number of decomposition
  levels, which improves reliability when decoding images with less-common
  level counts.
* Image I/O plugins can be selected on a per-format basis.
* Improved the efficiency of the Java 2D resample filters.
* When using the Java 2D pipeline to downscale to less than three pixels on a
  side, an empty image with the correct dimensions is returned, rather than a
  downscaled 3×3 image, which is the smallest size that the Java 2D resample
  filters support.
* Java2dProcessor supports animated GIFs.
* Java2dProcessor supports CMYK & YCCK JPEGs.
* Worked around some behavior of the `opj_decompress` tool that could cause
  OpenJpegProcessor to break. (Thanks to @adam-vessey)
* Improved PdfBoxProcessor's validation of requests for multi-page PDFs whose
  pages have inconsistent dimensions.
* The `processor.limit_to_8_bits` configuration option is no longer available.
  All output is limited to 8 bits.
* Disabled support for GIF source images in JaiProcessor, as it was buggy and
  other processors can handle GIF better.

### Caching

* AmazonS3Cache has been renamed S3Cache, and it now supports non-AWS
  endpoints.
* The time-to-live of the source and derivative caches is independently
  configurable.
* The cache worker dumps the contents of HeapCache during its shifts, if both
  are enabled and if HeapCache persistence is also enabled.
* The cache worker's work interval specifies the amount of time between the end
  of one shift and the beginning of the next, rather between the start of one
  shift and the next.
* The deprecated `DELETE /cache/:identifier` HTTP API method has been removed.

### Delegate Script

* The delegate script has been redesigned as a class that is instantiated
  per-request. See the "Delegate Script" section of the user manual for
  detailed information.

### Other

* Java 10 (18.3) is supported.
* Updated JRuby to 9.1.17.0.

## 3.4.4

* Fixed a regression introduced in 3.4.3 that could cause URIs to appear with
  the wrong scheme in information responses and `Link` headers. (Thanks to
  @mmatela and @jweisman)
* The `X-Forwarded-Host` reverse proxy header may contain a port, which will be
  used if `X-Forwarded-Port` is not supplied.

## 3.4.3

* Updated the Jackson dependency to address the following security
  vulnerabilities: CVE-2018-7489, CVE-2018-5968, CVE-2017-15095.
* AmazonS3Resolver and AmazonS3Cache can obtain credentials from Amazon
  container environment variables or EC2 Instance Metadata Service.
* Arbitrary `X-Forwarded` headers are respected without `X-Forwarded-Host`
  needing to be set. (Thanks to @jweisman)
* Fixed a potential resource leak involving stream-based resolvers.
* Fixed HttpResolver's handling of semicolon-delimited `Content-Type` header
  values.
* Fixed palette corruption when cropping source images with indexed color with
  Java2dProcessor.
* Fixed a `NullPointerException` when using the info cache without a derivative
  cache.
* Other minor fixes.

## 3.4.2

* Fixed a leak when using HttpResolver in a Servlet container that resulted in
  an inability to shut down properly. (Thanks to @punkfairy)
* Fixed aspect-fit scale-to-height not working properly in ImageMagickProcessor
  and GraphicsMagickProcessor.
* Fixed incorrect URIs in responses when using slash substitution or
  URI-encoded identifiers.
* Efficiency improvements in AmazonS3Resolver and AzureStorageResolver.
* `OutOfMemoryError`s during image responses are logged at error level rather
  than info level.
* Improved the reliability of Image I/O plugin selection.
* Updated the PDFBox dependency to version 2.0.8.

## 3.4.1

* Improved efficiency when handling a request for a bitonal or gray image when
  the source image is already bitonal or gray. (Thanks to @adam-vessey)
* Fixed upscaling by percent in KakaduProcessor and OpenJpegProcessor.
* Efficiency improvements in PdfBoxProcessor.
* Improved the reliability of source cache downloading and content acquisition.
* Miscellaneous reliability improvements in AmazonS3Resolver and AmazonS3Cache.
* Server misconfigurations cause HTTP 500 (Internal Server Error) responses
  rather than HTTP 400 (Bad Request).

## 3.4

### Endpoints

* HTTP/2 can be enabled in standalone mode. (H2 requires Java 9, but H2C works
  in Java 8.)
* The accept queue limit of the standalone HTTP & HTTPS servers is
  configurable.
* The minimum size in IIIF Image API 2.x `info.json` responses is configurable.
  (Thanks to @cbeer)
* The maximum size in IIIF Image API 2.x `info.json` responses is the full
  image size, rather than half size. (Thanks to @cbeer)
* The IIIF Image API 1.x endpoint returns a JPEG image, rather than GIF, when
  no client preference is expressed in the URI or `Accept` header.
* Certain derivative cache errors can be recovered from without causing an error
  response.
* The `sizes` key in Image API 2.x information responses respects the
  `max_pixels` configuration option.
* Image and information endpoints return `HTTP 501 Not Implemented`, rather
  than `500 Internal Server Error`, when the selected processor does not
  support the requested source format.
* Image requests may include a `?response-content-disposition` query argument
  to suggest a response `Content-Disposition` header value.
* Added several HTTP API methods for cache management.
* Changes to HTTP Basic authorization settings in all endpoints take effect
  without restarting.
* All endpoints support the HTTP `OPTIONS` method.
* Status items in the Control Panel update automatically via AJAX.
* The username used to access the Control Panel is configurable.

### Resolvers

* Miscellaneous efficiency improvements across all resolvers.
* The `HttpResolver::get_url()` delegate method may return a hash containing
  authentication credentials.
* Added an option to HttpResolver to trust all certificates.
* The request timeout of HttpResolver is configurable.
* AmazonS3Resolver and AzureStorageResolver attempt to infer a source format
  from the object/blob key if not able to do so from the identifier or from
  object/blob metadata.
* The maxmimum number of AWS connections used by AmazonS3Resolver is
  configurable.
* AmazonS3Resolver falls back to obtaining credentials from various other
  sources (see the user manual) when they are not set in the configuration
  file. (Thanks to @jweisman)
* Resolver delegates are passed an additional context parameter. (Thanks to
  @jonathangreen)

### Processors

* Added an option that enables Java2dProcessor, JaiProcessor, KakaduProcessor,
  and PdfBoxProcessor to preserve the sample depth of >8-bits-per-sample source
  images.
* GraphicsMagickProcessor and ImageMagickProcessor respect the `processor.dpi`
  configuration key.
* GraphicsMagickProcessor supports the GIF output format.
* ImageMagickProcessor supports image overlays.
* FfmpegProcessor supports FLV videos.
* The `processor.background_color` configuration key affects alpha blending as
  well as non-90°-multiple rotations. (Thanks to @Algae666)
* ImageMagickProcessor's support for ImageMagick versions prior to 7 is
  deprecated.
* OpenJpegProcessor's support for OpenJPEG versions prior to 2.2.0 is
  deprecated.
* Improved the predictability of Image I/O plugin selection.
* Initialization warnings (related to missing dependencies, for example) are
  displayed in the Control Panel.

### Caching

* Image info is written to caches asynchronously to enable faster responses.
* Added an optional in-memory image info cache.
* Added RedisCache.
* Added HeapCache.
* AmazonS3Cache uploads images to S3 asynchronously, enabling faster responses.
* The maxmimum number of AWS connections used by AmazonS3Cache is configurable.
* AmazonS3Cache falls back to obtaining credentials from various other sources
  (see the user manual) when they are not set in the config file. (Thanks to
  @jweisman)
* On macOS, FilesystemCache uses file last-modified times rather than
  last-accessed times as the latter are not reliable.

### Other

* Configuration files support inheritance via an `extends` key.
* The temporary directory is configurable.
* Added an optional error log.
* A logger is accessible from the delegate script.

## 3.3.5

* Fixed FilesystemCache failing to purge invalid source images. (Thanks to
  @jhosteny)
* Fixed the `cache.server.resolve_first` configuration key always being
  effectively `true` in the IIIF Image API 2.x endpoint.
* Fixed AmazonS3Cache sometimes purging fewer objects than it should.
* Fixed failure to auto-purge incompletely-written images from derivative
  caches other than FilesystemCache in response to a VM error (such as
  `OutOfMemoryError`).
* HttpResolver makes fewer HTTP requests.

## 3.3.4

* Fixed an `AbstractMethodError` in HTML views, caused by a regression in
  3.3.3.
* Fixed the `X-IIIF-ID` header not being respected when redirecting from
  `/:identifier` to `/:identifier/info.json` in both IIIF Image API endpoint
  versions. (Thanks to @jonathangreen)

## 3.3.3

* Added Java 9 compatibility.
* Upgraded dependencies to fix the following vulnerabilities: CVE-2015-6420.
  (Thanks to @kinow)
* Fixed incorrect computation of post-rotation bounding boxes, manifesting as
  bogus values passed to the `resulting_size` delegate method arguments.
  (Thanks to @SaschaAdler)
* Fixed a warning from FilesystemCache when multiple processes tried to create
  the same directory at the same time. (Thanks to @cbeer)
* Fixed potentially incorrect tile sizes in information responses when using
  OpenJpegProcessor.
* Fixed a bug in FfmpegProcessor that caused video info parsing to fail for
  some videos.
* Added `Origin` to all `Vary` response headers, which fixes a potential
  CORS-related caching issue.
* Fixed an issue where certain Image I/O plugins could fail to register when
  multiple applications that use Image I/O are running in the same container.
  (Thanks to @mmatela)
* Fixed a resource leak when using image overlays.
* Improvements in the testing and build process. (Thanks to @kinow)

## 3.3.2

* Fixed an inefficiency that caused image metadata to be read multiple times in
  a single image response.
* Fixed endpoint authentication being enabled for certain non-endpoint
  resources.
* Impoved compatibility between OpenJpegProcessor and OpenJPEG when using
  OpenJPEG version 2.2.0 or later. (Thanks to @RussellMcOrmond)
* Fixed inability to stream source images when using AmazonS3Resolver.
* Fixed a potential HTTP 500 error when multiple processes try to write the
  same image metadata concurrently using FilesystemCache.
* Corrected the documentation for `processor.upscale_filter` and
  `processor.downscale_filter`.
* GraphicsMagickProcessor and ImageMagickProcessor support a `?page` query
  argument with PDFs.

## 3.3.1

* Fixed caches failing to take URL query arguments into account.
* Fixed a potentially incorrect `Cache-Control` header being sent in HTTP ≥ 400
  responses.
* Fixed parsing of comma-separated `X-Forwarded-*` header values.
* Improved the reliability of the value of the `client_ip` argument to the
  `authorized?()` delegate method when behind a reverse proxy.
* Fixed a connection leak in AmazonS3Resolver.
* Fixed potential malformed concurrent image responses when using a processor
  other than ImageMagickProcessor or GraphicsMagickProcessor.
* Fixed a potential NullPointerException (HTTP 500) involving concurrent
  requests when using the delegate script.
* Fixed processors and caches not respecting the value of
  `processor.normalize`.
* Prevented the configuration file and/or delegate script from being reloaded
  twice on some platforms.
* Fixed PDF source format support in GraphicsMagickProcessor and
  ImageMagickProcessor.
* Upgraded JRuby to 9.1.10.0.

## 3.3

### Endpoints

* Auto-computed tile sizes of source images with an odd number of pixels in a
  dimension in information responses are rounded up, to prevent clients from
  requesting extremely narrow edge tiles.
* Operations that would have no effect in their context—for example, a crop to
  0,0/300×200, followed by a scale-to-fit to 300 pixels wide—are skipped.
* Improved request validation.
* Added a RESTful API endpoint to retrieve and update the configuration.

### Processors

* Most processor-specific configuration settings now apply across all
  processors, making them integrate better into the caching architecture.
* Added an auto-normalize option to all image processors.
* Added an option to all processors to output progressive JPEGs.
* Added experimental support for DICOM images to GraphicsMagickProcessor,
  ImageMagickProcessor, Java2dProcessor, and JaiProcessor. (This is a very
  complex format and not all variants are working.)
* FfmpegProcessor uses a Java 2D processing pipeline, giving it more
  capabilities.
* Java2dProcessor and JaiProcessor support 16-bit LZW- and ZIP-compressed
  TIFFs.
* GraphicsMagickProcessor and ImageMagickProcessor support selectable resample
  filters.
* GraphicsMagickProcessor and ImageMagickProcessor respect the EXIF Orientation
  tag.
* ImageMagickProcessor supports ImageMagick 7.
* PdfBoxProcessor supports JBIG2-encoded embedded images.
* PdfBoxProcessor returns HTTP 400 when given an illegal page number.
* FfmpegProcessor returns HTTP 400 when given an illegal time.

### Caching

* The `-Dcantaloupe.cache.purge` VM option accepts an identifier argument to
  purge all content related to an individual identifier.
* Added a RESTful API endpoint to purge individual images from the cache.
* A `cache=false` URL query option can be used in all endpoints to bypass the
  derivative cache and disable the `Cache-Control` header per-request.
* FilesystemCache supports identifiers longer than the filesystem's filename
  length limit.

### Overlays

* Watermarks have been renamed overlays.
* Added textual overlays.
* Overlay images can be located on the filesystem or an HTTP(S) server.
* Overlay images are cached in memory for better efficiency.

### Delegate Script

* Added an optional delegate method invocation cache.
* Added the ability to redirect to another URL via the `authorized?()` delegate
  method.
* The delegate script is reloaded on change rather than per-request.

## 3.2.4

* Fixed all processors except JaiProcessor treating scale-to-dimensions
  requests (`w,h`) as scale-to-fit (`!w,h`).
* Fixed a bug affecting upscaled crops when using pyramidal images with
  Java2dProcessor or JaiProcessor.
* Worked around a bug in JAI involving downscaling of compressed TIFF tiles,
  affecting JaiProcessor.

## 3.2.3

* Fixed inability to upscale in JaiProcessor, omitted in error from 3.2.2.
* Fixed incorrect scale size computation using flooring instead of rounding.
* Fixed a bug in FilesystemCache that could cause an HTTP 500 error when
  accessing the same uncached `info.json` resource concurrently.
* FilesystemCache cleans up zero-byte files created in certain situations,
  typically involving failure in the underlying storage.
* Fixed a bug that could cause incompletely-written cached images (with any
  cache) to not get cleaned up properly.

## 3.2.2

* Fixed inability to upscale by pixels.
* Fixed an error in response to requests for images less than 3 pixels on a
  side when sharpening is enabled, in all processors using the Java 2D
  pipeline.
* Fixed an error in JaiProcessor when attempting to scale an image less than 3
  pixels on the smallest side.
* Fixed a bug in FilesystemCache that caused zero-byte files to be written and
  an HTTP 500 response when handling concurrent identical requests.
* Error responses include a more appropriate `Cache-Control` header rather than
  the one set in the configuration.
* Trailing slashes in `X-Forwarded-Path` header values are trimmed off,
  preventing double-slashes from appearing in generated URLs.

## 3.2.1

* Fixed a bug that caused the resolver selected by the `get_resolver()`
  delegate method to not be initielized properly.
* Fixed a path-handling bug affecting ImageMagickProcessor and
  GraphicsMagickProcessor primarily in Windows.
* Fixed incorrect image sizes when using FfmpegProcessor with percentage scales
  above 100%.
* Improved support for 16-bit source images in Java2dProcessor, JaiProcessor,
  GraphicsMagickProcessor, and ImageMagickProcessor.

## 3.2

* Java 8 is now required.
* Significant efficiency improvements in the Java 2D processing pipeline
  affecting Java2dProcessor, JaiProcessor, and KakaduProcessor.
* All processors that use the Java 2D pipeline (Java2dProcessor,
  KakaduProcessor, OpenJpegProcessor, PdfBoxProcessor) support selectable
  upscaling and downscaling filters.
* Improved the scale quality of JaiProcessor.
* All processors offer configurable sharpening levels.
* Applicable processors respect source ICC color profiles.
* Added the option to copy EXIF, IPTC IIM, XMP, and/or native TIFF and PNG
  metadata from source images to derivative images of the same format, for
  some processors.
* Java2dProcessor and JaiProcessor respect the value of an embedded EXIF
  Orientation tag.
* Increased the default Java2dProcessor/JaiProcessor JPEG quality to 0.8 (80).
* Added support for a `X-IIIF-ID` request header, in order to return correct
  image identifiers in information responses whose requests have passed through
  a reverse proxy using a different identifier.
* Removed the JAI post-processor option for KakaduProcessor and
  OpenJpegProcessor.
* Zero-byte files are deleted when purging or cleaning FilesystemCache.
* KakaduProcessor and OpenJpegProcessor return a less inscrutable error
  message when they are unable to read a source file.
* HTTP 415 is returned instead of HTTP 400 when an output format is requested
  that is valid but unsupported by the current processor.
* Colorized the console application log.
* Console application log messages with a severity of `WARN` and above are
  routed to standard error.
* Upgraded a number of libraries.

## 3.1.2

* Fixed a resource leak that would cause landing pages, error pages, and the
  Control Panel to become inaccessible after a long time running.
* Fixed a bug where the watermark wouldn't be added if redactions were enabled
  and set to use `DelegateScriptStrategy` but the delegate script was disabled.
* Fixed tile sizes in the IIIF Image API 2.x information response when using
  KakaduProcessor.
* HTTP 400 is returned in response to a request for an image that would be
  zero pixels on a side, even when the zero dimension is not explicit in the
  request. (For example, a size of `4,` for an image that is at least five
  times wider than it is tall.) (Previously, it was only returned when zero
  pixels was explicitly requested, and the image returned for implicit zero
  sizes was incorrect.)

## 3.1.1

* Added the `http.host` and `https.host` standalone mode configuration options,
  defaulting to listen on `0.0.0.0` (all interfaces).

## 3.1

* Added support for version 2.1 of the IIIF Image API.
    * Added support for the `square` region parameter to all processors.
    * Added the `max` size parameter.
    * Removed the information request `Link` header for the JSON-LD context.
    * Added the `sizeByConfinedWh` and `sizeByDistortedWh` feature names,
      supported by all processors.
    * Added the `maxArea` property to the profile description, based on the
      value of the `max_pixels` configuration key.
* Replaced the `get_iiif2_service` delegate method with
  `extra_iiif2_information_response_keys`.

## 3.0.1

* Fixed a broken build in standalone mode.

## 3.0

* Added a Control Panel for web-based configuration changes.
* Changed the distribution archive to a custom WAR file that can be run
  standalone (as before) or in a Servlet container.
* Added AmazonS3Cache.
* Added AzureStorageCache.
* Added redaction.
* Added the ability to restrict available sizes to those specified in the
  `info.json` response in the IIIF Image API 2.0 endpoint.
* Added an optional source image cache, which can improve the performance of
  StreamProcessors, as well as enable FileProcessors to work with
  StreamResolvers.
* Moved JdbcResolver delegate methods to the delegate script.
* Moved resolver-related delegate methods to submodules to improve
  organization.
* Changes to the logging configuration are applied immediately without
  restarting.
* Requests for more than `max_pixels` receive an HTTP 403 response rather than
  HTTP 500.
* Upgraded the Amazon S3 client to version 1.10.56.
* Upgraded the Restlet framework to version 2.3.7.
