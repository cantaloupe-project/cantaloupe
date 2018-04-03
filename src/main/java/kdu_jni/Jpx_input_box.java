package kdu_jni;

public class Jpx_input_box extends Jp2_input_box {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Jpx_input_box(long ptr) {
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
  public Jpx_input_box() {
    this(Native_create());
  }
  public native boolean Exists() throws KduException;
  public native boolean Open_as(Jpx_fragment_list _frag_list, Jp2_data_references _data_refs, Jp2_family_src _ultimate_src, long _box_type) throws KduException;
}
