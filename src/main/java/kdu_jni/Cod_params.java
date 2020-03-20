package kdu_jni;

public class Cod_params extends Kdu_params {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Cod_params(long ptr) {
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
  public Cod_params() {
    this(Native_create());
  }
  public native boolean Is_valid_decomp_terminator(int _val) throws KduException;
  public native int Transpose_decomp(int _val) throws KduException;
  public native int Expand_decomp_bands(int _decomp_val, short[] _band_descriptors) throws KduException;
  public native void Get_max_decomp_levels(int _decomp_val, int[] _max_horizontal_levels, int[] _max_vertical_levels) throws KduException;
}
