# About This Folder

Inside each platform-specific folder is a `lib` folder containing the Kakadu
shared library needed by `KakaduNativeProcessor`. See the user manual ->
Processors section -> KakaduNativeProcessor section for setup steps.

# License

All of the software in this directory tree is distributed under a Kakadu Evaluation License which is intended for an individual or organisation wishing to evaluate the Software with a view of procuring a longer term License Type. Please see the
[Kakadu License Terms](https://kakadusoftware.com/wp-content/uploads/3451-3100-0598_1_Kakadu-Software-Licence-Terms-and-Conditions-FINAL-1-Sept-2021.pdf)
for the terms of use which are outlined below. 

## Evaluation License
### 3.1 Applicability

(a) The Evaluation License is the new name for the following previously named licenses:

(i) “SDK Evaluation License”; and

(ii) “HT Evaluation License” (previously named “HTJ2K Evaluation License”).

(b) The Evaluation License is intended for an individual or organisation wishing to evaluate
the Software with a view of procuring a longer term License Type.

### 3.2 Grant of License

(a) Kakadu Company grants you during the License Term a non-exclusive, non-
transferable, worldwide and revocable license to install and use the Software and to
use the Software to develop Applications, for evaluation purposes only, provided that:

(i) any use of the Software is confined to your internal use and non-commercial
purposes (which, without limitation, must not include the commercial exploitation
of the Software or any Applications);

(ii) any copying, altering, modifying, adapting, translating and creating of derivative
works of the Software must be limited to the purpose of creating Applications
only;

(iii) any Applications developed by you or otherwise enabled by your use of the
Software may not be used to conduct business or distributed to any third party.
Any such distribution may require the purchase of an alternative license as
separately agreed between the parties; and

(iv) you comply with any special conditions as specified in the Order Form.

(b) Unless the parties agree otherwise, you will not be entitled to any updates or upgrades
to the Software.

### 3.3 License Term
The License Term will commence on the Commencement Date, and the Evaluation License
will terminate on the earlier of:

(a) 6 months from the Commencement Date; or

(b) the date this Agreement is terminated under clause 12.

### 3.4 Consequences of termination or expiry
Unless you have entered into a longer term License Type during the License Term, upon
termination or expiry of the Agreement or the License Term, you must cease all use of the Software and Applications for any purpose, destroy any copies and components of the Software, Applications and derivative works of the Software and provide written certification to Kakadu Company that you have complied with your obligations under this clause 3.4.

### 3.5 Fees
Unless otherwise specified in the Order Form, the License Fee is the single payment as specified in the Order Form, due on or before the Commencement Date.

# Build Info

## Linux

The Linux binaries were compiled on CentOS 7 x86 64-bit with gcc 4.8.5.

```
export JAVA_HOME=/usr/lib/jvm/java
cd coresys/make
make -f Makefile-Linux-x86-64-gcc
cd ../../managed/make
make -f Makefile-Linux-x86-64-gcc
cd ../../lib/Linux-x86-64-gcc

# Builds `libkdu_vNXX.so` & `libkdu_jni.so`
# Java class files are in `../../../java/kdu_jni`
```

## macOS

The macOS binaries were compiled on macOS 10.13.4 with xcodebuild and
clang 902.0.39.1, for target `x86_64-apple-darwin17.5.0`.

```
cd managed
xcodebuild -project managed.xcodeproj -target kdu_jni -configuration Release clean
xcodebuild -project managed.xcodeproj -target kdu_jni -configuration Release

# Resulting binaries are in ../../bin
# Java class files are in ../../java/kdu_jni
```

(Future note: for Catalina, add `-UseModernBuildSystem=NO` to the `xcodebuild`
commands.)

## Windows

The Windows binaries were compiled on Windows 7 SP1 64-bit with Visual
Studio Community 2015.

### Build Steps

1. Install the JDK
2. Install Visual Studio with the Microsoft Foundation Classes for C++
   component
3. Build `coresys`
    1. Open `coresys\coresys_2015`
    2. Retarget solution to the 8.1 platform version
    3. Build with Release configuration & x64 platform
4. Build `kdu_jni`
    1. Open `managed\kdu_managed_2015`
    2. Add the JDK headers to the include path
        1. Right-click on the `kdu_jni` solution
        2. Go to Properties -> VC++ Directories -> Include Directories
        3. Add `jdk-x.x.x\include` and `jdk-x.x.x\include\win32` paths to JDK
           headers
    3. Retarget solution to the 8.1 platform version
    4. Build with Release configuration & x64 platform

The resulting files are in `..\..\bin_x64`:
  * `kdu_v80R.dll`
  * `kdu_a80R.dll`
  * `kdu_jni.dll`
