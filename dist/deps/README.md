# About this folder

This folder contains native binaries needed by KakaduDemoProcessor and
KakaduNativeProcessor.

Inside each platform-specific folder are `bin` and `lib` folders:

* The `bin` folder contains `kdu_expand`, one of the demo tools needed by
  KakaduDemoProcessor.
* The `lib` folder contains the Kakadu shared library needed by both
  `kdu_expand` and `KakaduNativeProcessor`.

See the user manual -> Processors section -> KakaduDemoProcessor &
KakaduNativeProcessor sections for setup steps.

# License

All of the software in this directory tree is distributed under a Kakadu Public
Service License, which means that it may only be used for non-commercial
purposes that are solely for the public good. See the following document for
detailed terms of use:
http://kakadusoftware.com/wp-content/uploads/2014/06/Kakadu-Licence-Terms-Feb-2018.pdf

# Build info

## Linux

The Linux binaries were compiled on CentOS 7 x86 64-bit with gcc 4.8.5.

```
cd coresys/make
make -f Makefile-Linux-x86-64-gcc
cd ../../managed/make
make -f Makefile-Linux-x86-64-gcc
cd ../../lib/Linux-x86-64-gcc

# Builds libkdu_vNXX.so & libkdu_jni.so
# Java class files are in ../../../java/kdu_jni
```

## macOS

The macOS binaries were compiled on macOS 10.13.4 with xcodebuild and
clang 902.0.39.1, for target x86_64-apple-darwin17.5.0.

```
cd managed
xcodebuild -project managed.xcodeproj -target kdu_jni -configuration Release clean
xcodebuild -project managed.xcodeproj -target kdu_jni -configuration Release

# Resulting binaries are in ../../bin
# Java class files are in ../../java/kdu_jni
```

## Windows

The Windows binaries were compiled on Windows 7 SP1 64-bit with Visual
Studio 2017.
