package kdu_jni;

public class Kdu_window_prefs {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_window_prefs(long ptr) {
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
  public Kdu_window_prefs() {
    this(Native_create());
  }
  private static native long Native_create(Kdu_window_prefs _src);
  public Kdu_window_prefs(Kdu_window_prefs _src) {
    this(Native_create(_src));
  }
  public native void Init() throws KduException;
  public native void Copy_from(Kdu_window_prefs _src) throws KduException;
  public native int Update(Kdu_window_prefs _src) throws KduException;
  public native String Parse_prefs(String _string) throws KduException;
  public native boolean Set_pref(int _pref_flag, boolean _make_required) throws KduException;
  public boolean Set_pref(int _pref_flag) throws KduException
  {
    return Set_pref(_pref_flag,(boolean) false);
  }
  public native void Set_max_bandwidth(long _max_bw, boolean _make_required) throws KduException;
  public void Set_max_bandwidth(long _max_bw) throws KduException
  {
    Set_max_bandwidth(_max_bw,(boolean) false);
  }
  public native void Set_bandwidth_slice(long _bw_slice, boolean _make_required) throws KduException;
  public void Set_bandwidth_slice(long _bw_slice) throws KduException
  {
    Set_bandwidth_slice(_bw_slice,(boolean) false);
  }
  public native int Get_colour_description_priority(int _space, int _prec, byte _approx) throws KduException;
}
