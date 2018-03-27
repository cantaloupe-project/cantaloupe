package kdu_jni;

public class Kdu_overlay_params {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_overlay_params(long ptr) {
    _native_ptr = ptr;
  }
  public native int Get_codestream_idx() throws KduException;
  public native int Get_compositing_layer_idx() throws KduException;
  public native int Get_max_painting_border() throws KduException;
  public native int Get_num_aux_params() throws KduException;
  public native long Get_aux_param(int _n) throws KduException;
  public native void Push_aux_params(long[] _aux_params, int _num_aux_params) throws KduException;
  public native void Restore_aux_params() throws KduException;
  public native void Configure_ring_points(int _stride, int _radius) throws KduException;
  public native long Get_ring_points(int _min_y, int _max_y, int[] _num_vals) throws KduException;
  public native Jpx_roi Map_jpx_regions(Jpx_roi _regions, int _num_regions, Kdu_coords _image_offset, Kdu_coords _subsampling, boolean _transpose, boolean _vflip, boolean _hflip, Kdu_coords _expansion_numerator, Kdu_coords _expansion_denominator, Kdu_coords _compositing_offset) throws KduException;
}
