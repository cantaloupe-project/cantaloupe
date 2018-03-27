package kdu_jni;

public class Kdu_sampled_range {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_sampled_range(long ptr) {
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
  public Kdu_sampled_range() {
    this(Native_create());
  }
  private static native long Native_create(Kdu_sampled_range _src);
  public Kdu_sampled_range(Kdu_sampled_range _src) {
    this(Native_create(_src));
  }
  private static native long Native_create(int _val);
  public Kdu_sampled_range(int _val) {
    this(Native_create(_val));
  }
  private static native long Native_create(int _from, int _to);
  public Kdu_sampled_range(int _from, int _to) {
    this(Native_create(_from, _to));
  }
  private static native long Native_create(int _from, int _to, int _step);
  public Kdu_sampled_range(int _from, int _to, int _step) {
    this(Native_create(_from, _to, _step));
  }
  public native boolean Is_empty() throws KduException;
  public native int Get_from() throws KduException;
  public native int Get_to() throws KduException;
  public native int Get_step() throws KduException;
  public native int Get_remapping_id(int _which) throws KduException;
  public native int Get_context_type() throws KduException;
  public native void Set_from(int _from) throws KduException;
  public native void Set_to(int _to) throws KduException;
  public native void Set_step(int _step) throws KduException;
  public native void Set_remapping_id(int _which, int _id_val) throws KduException;
  public native void Set_context_type(int _ctp) throws KduException;
}
