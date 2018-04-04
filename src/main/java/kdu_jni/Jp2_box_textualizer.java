package kdu_jni;

public class Jp2_box_textualizer {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jp2_box_textualizer(long ptr) {
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
  public Jp2_box_textualizer() {
    this(Native_create());
  }
  public native boolean Add_box_type(long _box_type, String _box_name) throws KduException;
  public native String Get_box_name(long _box_type) throws KduException;
  public native boolean Check_textualizer_function(long _box_type) throws KduException;
  public native boolean Textualize_box(Jp2_input_box _box, Kdu_message _tgt, boolean _xml_embedded, int _max_len) throws KduException;
}
