package kdu_jni;

public class Kdu_metareq {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_metareq(long ptr) {
    _native_ptr = ptr;
  }
  private native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  public native boolean Equals(Kdu_metareq _rhs) throws KduException;
  public native long Get_box_type() throws KduException;
  public native int Get_qualifier() throws KduException;
  public native boolean Get_priority() throws KduException;
  public native int Get_byte_limit() throws KduException;
  public native boolean Get_recurse() throws KduException;
  public native long Get_root_bin_id() throws KduException;
  public native int Get_max_depth() throws KduException;
  public native Kdu_metareq Get_next() throws KduException;
}
