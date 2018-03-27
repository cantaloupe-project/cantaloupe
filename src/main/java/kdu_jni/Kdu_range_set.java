package kdu_jni;

public class Kdu_range_set {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_range_set(long ptr) {
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
  public Kdu_range_set() {
    this(Native_create());
  }
  private static native long Native_create(Kdu_range_set _src);
  public Kdu_range_set(Kdu_range_set _src) {
    this(Native_create(_src));
  }
  public native void Copy_from(Kdu_range_set _src) throws KduException;
  public native boolean Is_empty() throws KduException;
  public native boolean Contains(Kdu_range_set _rhs, boolean _empty_set_defaults_to_zero) throws KduException;
  public boolean Contains(Kdu_range_set _rhs) throws KduException
  {
    return Contains(_rhs,(boolean) false);
  }
  public native boolean Equals(Kdu_range_set _rhs, boolean _empty_set_defaults_to_zero) throws KduException;
  public boolean Equals(Kdu_range_set _rhs) throws KduException
  {
    return Equals(_rhs,(boolean) false);
  }
  public native void Init() throws KduException;
  public native void Add(Kdu_sampled_range _range, boolean _allow_merging) throws KduException;
  public void Add(Kdu_sampled_range _range) throws KduException
  {
    Add(_range,(boolean) true);
  }
  public native void Add(int _val) throws KduException;
  public native void Add(int _from, int _to) throws KduException;
  public native int Get_num_ranges() throws KduException;
  public native Kdu_sampled_range Get_range(int _n) throws KduException;
  public native Kdu_sampled_range Access_range(int _n) throws KduException;
  public native boolean Test(int _index) throws KduException;
  public native int Expand(int[] _buf, int _accept_min, int _accept_max) throws KduException;
}
