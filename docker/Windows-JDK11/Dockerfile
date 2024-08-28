FROM mcr.microsoft.com/windows/servercore:ltsc2022

ENV chocolateyUseWindowsCompression false

# Install the Chocolatey package manager, which makes it easier to install
# dependencies.
RUN powershell -Command \
  iex ((new-object net.webclient).DownloadString('https://chocolatey.org/install.ps1')); \
  choco feature disable --name showDownloadProgress

# Install various dependencies
# TODO: openjpeg
RUN choco install -y ojdkbuild11 maven ffmpeg

# Install TurboJpegProcessor dependencies TODO: libjpeg-turbo
#RUN mkdir -p /opt/libjpeg-turbo/lib
#COPY docker/image_files/libjpeg-turbo/lib64 c:\windows\system32

# Install KakaduNativeProcessor dependencies
COPY dist/deps/Windows-x86-64/lib/* c:/Windows/System32/

# Install application dependencies
COPY pom.xml pom.xml
RUN mvn dependency:resolve

# Copy the code
COPY docker/image_files/test.properties test.properties
COPY src src
