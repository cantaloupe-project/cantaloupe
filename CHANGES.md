# Change Log

## 1.0-beta4

* Speed up TIFF decoding in Java2dProcessor.

## 1.0-beta3

* Added client-side caching.
* Added HTTP Basic authentication.
* Added KakaduProcessor.
* Added a `sizes` key to information responses.
* Renamed ImageIoProcessor to Java2dProcessor.
* Java2dProcessor supports pyramidal TIFF.
* Improved the thread-safety of FilesystemCache.
* Improved the security of FilesystemResolver.
* Source images are streamed directly rather than being re-encoded when the
  IIIF parameters request a full-size, unmodified image.
* Cantaloupe is now distributed as a .zip archive.

## 1.0-beta2

* Added optional server-side caching via FilesystemCache.
* Added configurable path separators in FilesystemResolver and HttpResolver.
* The application version is displayed on the landing page and in a startup log
  message.

## 1.0-beta1

* First version.
