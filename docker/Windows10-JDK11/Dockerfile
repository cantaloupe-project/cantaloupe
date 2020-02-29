FROM mcr.microsoft.com/windows/servercore:ltsc2019

ENV chocolateyUseWindowsCompression false

# Install the Chocolatey package manager, which makes it easier to install
# dependencies.
RUN powershell -Command \
    iex ((new-object net.webclient).DownloadString('https://chocolatey.org/install.ps1')); \
    choco feature disable --name showDownloadProgress

# Install OpenJDK
#RUN powershell -Command \
#    New-Item -ItemType Directory -Force -Path c:\tmp; \
#    (new-object System.Net.WebClient).Downloadfile('https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_windows-x64_bin.zip', 'c:\tmp\openjdk.zip'); \
#    New-Item -ItemType Directory -Force -Path c:\Java; \
#    Expand-Archive c:\tmp\openjdk.zip -DestinationPath c:\Java -Force; \
#    Remove-Item c:\tmp\openjdk.zip -Force

# Install various dependencies
# N.B.: OpenJDK is installed instead of Oracle JDK because Oracle tends to move
# older releases which breaks Chocolatey's URLs.
# TODO: openjpeg & redis
RUN choco install -y ojdkbuild11 maven ffmpeg

# Install TurboJpegProcessor dependencies TODO: do this
#RUN mkdir -p /opt/libjpeg-turbo/lib
#COPY docker/Windows10-JDK11/image_files/libjpeg-turbo/lib64 c:\windows\system32

# Install KakaduNativeProcessor dependencies
COPY dist/deps/Windows-x86-64/lib/* c:/Windows/System32/

RUN powershell New-Item -ItemType Directory -Force -Path c:\cantaloupe
WORKDIR c:/cantaloupe

# Install application dependencies
COPY pom.xml pom.xml
RUN mvn dependency:resolve

# Copy the code
COPY docker/Windows10-JDK11/image_files/test.properties test.properties
COPY src src
