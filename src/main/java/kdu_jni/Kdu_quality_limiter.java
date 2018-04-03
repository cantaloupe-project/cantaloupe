package kdu_jni;

public class Kdu_quality_limiter {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_quality_limiter(long ptr) {
    _native_ptr = ptr;
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create(float _weighted_rmse, boolean _preserve_if_reversible);
  public Kdu_quality_limiter(float _weighted_rmse, boolean _preserve_if_reversible) {
    this(Native_create(_weighted_rmse, _preserve_if_reversible));
  }
  private static long Native_create(float _weighted_rmse)
  {
    return Native_create(_weighted_rmse,(boolean) true);
  }
  public Kdu_quality_limiter(float _weighted_rmse) {
    this(Native_create(_weighted_rmse));
  }
  public native Kdu_quality_limiter Duplicate() throws KduException;
  public native void Set_display_resolution(float _hor_ppi, float _vert_ppi) throws KduException;
  public native void Set_comp_info(int _c, float _square_weight, int _type_flags) throws KduException;
  public native float Get_weighted_rmse() throws KduException;
  public native void Get_comp_info(int _c, float[] _square_weight, int[] _type_flags) throws KduException;
  public native float Get_square_visual_weight(int _orientation, int _rel_depth, Kdu_coords _component_subsampling, boolean _is_chroma, boolean _reversible) throws KduException;
}
