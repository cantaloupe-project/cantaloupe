----------------------------------------
ABOUT THIS FOLDER
----------------------------------------

This folder contains native binaries needed by KakaduDemoProcessor and KakaduNativeProcessor.

Inside each platform-specific folder are `bin` and `lib` folders. The `bin`
folder contains `kdu_expand`, one of the Kakadu demo tools needed by
KakaduDemoProcessor. The `lib` folder contains the Kakadu shared library
needed by both `kdu_expand` and `KakaduNativeProcessor`.

See the Cantaloupe user manual -> Processors section -> KakaduDemoProcessor &
KakaduNativeProcessor sections for setup steps.

----------------------------------------
LICENSE
----------------------------------------

All of the software in this directory tree is distributed under a Kakadu Public
Service License, which means that it may only be used for non-commercial
purposes that are solely for the public good. See the following document for
detailed terms of use:
http://kakadusoftware.com/wp-content/uploads/2014/06/Kakadu-Licence-Terms-Feb-2018.pdf

----------------------------------------
HOW THE BINARIES WERE BUILT
----------------------------------------

* The Linux binaries were compiled on CentOS 7 x86 64-bit with gcc 4.8.5.
* The macOS binaries were compiled on macOS 10.13.4 with xcodebuild and
  clang 902.0.39.1, for target x86_64-apple-darwin17.5.0.
* The Windows binaries were compiled on Windows 7 SP1 64-bit with Visual
  Studio 2017.
