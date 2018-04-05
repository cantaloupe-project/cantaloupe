----------------------------------------
ABOUT THIS FOLDER
----------------------------------------

This folder contains native libraries needed by KakaduNativeProcessor.
If you aren't using KakaduNativeProcessor, don't worry about any of this stuff.
Otherwise, see the Cantaloupe user manual -> Processors section ->
KakaduNativeProcessor section for an explanation of how to get things working.

Note that these native libraries were compiled using a Kakadu Public Service
License, which means that they may only be used with the application with
which they were bundled, and may only be used for non-commercial purposes.
See the following document for detailed terms:
http://kakadusoftware.com/wp-content/uploads/2014/06/Kakadu-Licence-Terms-Feb-2018.pdf

----------------------------------------
HOW THE LIBRARIES WERE BUILT
----------------------------------------

* The Linux libraries were compiled on CentOS 7 x86 64-bit with gcc 4.8.5.
* The macOS libraries were compiled on macOS 10.13.4 with xcodebuild and
  clang 902.0.39.1, for target x86_64-apple-darwin17.5.0.
* The Windows libraries were compiled on Windows 7 64-bit with Visual Studio
  2017.
