package kdu_jni;

public class Jp2_dref_permissions {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jp2_dref_permissions(long ptr) {
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
  public Jp2_dref_permissions() {
    this(Native_create());
  }
  public native void Reset() throws KduException;
  public native void Init(String _container_fname, String _access_permissions) throws KduException;
  public native boolean Can_access(String _file_url) throws KduException;
}
