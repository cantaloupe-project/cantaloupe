package kdu_jni;

public class Jp2_locator {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jp2_locator(long ptr) {
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
  public Jp2_locator() {
    this(Native_create());
  }
  public native boolean Is_null() throws KduException;
  public native long Get_file_pos() throws KduException;
  public native void Set_file_pos(long _pos) throws KduException;
  public native long Get_databin_id() throws KduException;
  public native long Get_databin_pos() throws KduException;
}
