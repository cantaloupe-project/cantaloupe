/**
 * <p>Contains the {@link org.libjpegturbo.turbojpeg libjpeg-turbo JNI binding}
 * from the {@literal java} subdirectory in the libjpeg-turbo 2.0.2 source
 * distribution.</p>
 *
 * <p>Note that libjpeg-turbo must be compiled with Java support, which it
 * isn't by default.</p>
 *
 * <h2>Building libjpeg-turbo with Java support</h2>
 *
 * <h3>Linux/macOS</h3>
 *
 * {@code
 * $ cd libjpeg-turbo-2.0.x
 * $ export JAVAFLAGS="-source 1.8 -target 1.8"
 * $ export JAVA_HOME=`/usr/libexec/java_home -v 1.8` # or whatever
 * $ make clean
 * $ cmake -G"Unix Makefiles" -DWITH_JAVA=1
 * $ make
 * $ sudo make install
 * }
 *
 * <h3>Windows</h3>
 *
 * <p>Have fun!</p>
 */
package org.libjpegturbo.turbojpeg;