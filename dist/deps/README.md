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

Github action using `ubuntu-latest`

```
- name: Set up Java
  uses: actions/setup-java@v3
  with:
    java-version: '17'
    distribution: 'temurin'  

- name: Make coresys
  run: |
    cd coresys/make
    make -f Makefile-Linux-x86-64-gcc

- name: Make managed
  run: | 
    cd managed/make
    make -f Makefile-Linux-x86-64-gcc

# Builds `libkdu_vNXX.so` & `libkdu_jni.so`
# Java class files are in `../../../java/kdu_jni`
```

## Linux Jammy

Github action using `ubuntu-22.04`

```
- name: Set up Java
  uses: actions/setup-java@v3
  with:
    java-version: '17'
    distribution: 'temurin'  

- name: Make coresys
  run: |
    cd coresys/make
    make -f Makefile-Linux-x86-64-gcc

- name: Make managed
  run: | 
    cd managed/make
    make -f Makefile-Linux-x86-64-gcc
```

## macOS

The macOS binaries were compiled using Github actions on `macos-latest`

```
- name: Patch jni_builder.cpp to use standard jni.h
run: |
    sed -i.bak 's|# include <JavaVM/jni\.h>|# include <jni.h>|' apps/kdu_hyperdoc/jni_builder.cpp
    rm apps/kdu_hyperdoc/jni_builder.cpp.bak  
    grep jni.h apps/kdu_hyperdoc/jni_builder.cpp

- name: Build kdu_hyperdoc tool
run: |
    # This needs to be run first so the files are available for the last step
    xcodebuild -project managed/managed.xcodeproj -target kdu_hyperdoc -configuration Release \
    MACOSX_DEPLOYMENT_TARGET=12.0

- name: Run kdu_hyperdoc to generate sources (errors NOT swallowed)
working-directory: documentation
run: |
    set -e  # fail the step on any error, unlike the original script's `|| echo warning`

    BUILD_DIR=/Users/runner/work/kakadu/bin
    TARGET_NAME=kdu_hyperdoc

    # Ensure the java output directory exists -- this path resolves to
    # a sibling of the repo checkout, which likely doesn't exist yet on a
    # fresh CI runner (unlike a full local SDK extraction).
    mkdir -p ../../java/kdu_jni

    "${BUILD_DIR}/${TARGET_NAME}" -o html_pages -s hyperdoc.src \
    -java ../../java/kdu_jni ../managed/kdu_jni ../managed/kdu_aux ../managed/all_includes

- name: Verify generated files exist
run: |
    test -f managed/kdu_jni/kdu_jni.h
    test -f managed/kdu_jni/kdu_jni.cpp
    test -f managed/kdu_aux/kdu_aux.cpp
    echo "All generated files present."

- name: Build kdu_jni
run: |
    set -o pipefail
    xcodebuild -project managed/managed.xcodeproj -target kdu_jni -configuration Release \
    MACOSX_DEPLOYMENT_TARGET=12.0 \
    HEADER_SEARCH_PATHS='$(inherited) '"$JAVA_HOME"'/include '"$JAVA_HOME"'/include/darwin' \
    2>&1 | tee build.log
    grep -B 2 -A 6 "error:" build.log || true   
```

(Future note: for Catalina, add `-UseModernBuildSystem=NO` to the `xcodebuild`
commands.)

## Windows

Using Windows Github action `windows-latest`

```
   # Setup Java
- name: Set up Java
uses: actions/setup-java@v3
with:
    java-version: '17'
    distribution: 'temurin'

- name: Add msbuild to PATH
uses: microsoft/setup-msbuild@v2      

- name: see install versions
run: Get-ChildItem "C:\Program Files (x86)\Windows Kits\10\Include"  

- name: Make coresys
run: |
    msbuild coresys/coresys_2022.sln /p:Configuration=Release /property:Platform=x64

- name: Make managed
run: |
    msbuild managed/kdu_managed_2022.sln /p:Configuration=Release /property:Platform=x64  
```