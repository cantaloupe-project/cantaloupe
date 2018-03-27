package kdu_jni;

public class Kdu_cache_file_info {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_cache_file_info(long ptr) {
    _native_ptr = ptr;
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create();
  public Kdu_cache_file_info() {
    this(Native_create());
  }
  public native boolean Exists() throws KduException;
  public native void Reset() throws KduException;
  public native String Get_cache_identifier() throws KduException;
  public native String Get_host_name() throws KduException;
  public native String Get_target_name() throws KduException;
  public native int Get_header_bytes() throws KduException;
  public native int Get_preamble_bytes() throws KduException;
}
