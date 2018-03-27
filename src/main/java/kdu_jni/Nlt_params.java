package kdu_jni;

public class Nlt_params extends Kdu_params {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Nlt_params(long ptr) {
    super(ptr);
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  public native void Make_gamma_params(float _p1, float _p2, float[] _gamma) throws KduException;
  public native void Apply_fwd_gamma(float[] _lut, int _num_entries, float[] _gamma) throws KduException;
  public native void Apply_rev_gamma(float[] _lut, int _num_entries, float[] _gamma) throws KduException;
}
