package kdu_jni;

public class Kdu_servex extends Kdu_serve_target {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_servex(long ptr) {
    super(ptr);
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create();
  public Kdu_servex() {
    this(Native_create());
  }
  public native void Open(String _filename, int _phld_threshold, int _per_client_cache, long _cache_fp, boolean _cache_exists, long _sub_start, long _sub_lim) throws KduException;
  public native void Close() throws KduException;
}
