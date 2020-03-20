package kdu_jni;

public class Kdu_platform_file {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_platform_file(long ptr) {
    _native_ptr = ptr;
  }
  private native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create();
  public Kdu_platform_file() {
    this(Native_create());
  }
  public native boolean Exists() throws KduException;
  public native boolean Open(String _pathname, boolean _for_writing) throws KduException;
  public native boolean Append(String _pathname) throws KduException;
  public native boolean Map(Kdu_platform_file _file) throws KduException;
  public native boolean Map(long _file, boolean _for_writing) throws KduException;
  public native void Close() throws KduException;
  public native int Read(byte[] _buf, int _num_bytes) throws KduException;
  public native int Write(byte[] _buf, int _num_bytes) throws KduException;
}
