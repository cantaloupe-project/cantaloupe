package kdu_jni;

public class Kdu_istream_ref {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_istream_ref(long ptr) {
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
  public Kdu_istream_ref() {
    this(Native_create());
  }
  public native boolean Is_null() throws KduException;
  public native boolean Exists() throws KduException;
}
