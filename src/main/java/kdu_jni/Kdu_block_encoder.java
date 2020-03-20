package kdu_jni;

public class Kdu_block_encoder extends Kdu_block_encoder_base {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_block_encoder(long ptr) {
    super(ptr);
  }
  private native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create();
  public Kdu_block_encoder() {
    this(Native_create());
  }
  public native void Speedpack_config(Kdu_coords _nominal_block_size, int _K_max_prime) throws KduException;
  public native void Encode(Kdu_block _block, boolean _reversible, double _msb_wmse, int _min_slope_threshold, int _max_slope_threshold, boolean _use_existing_slopes) throws KduException;
  public void Encode(Kdu_block _block, boolean _reversible, double _msb_wmse, int _min_slope_threshold, int _max_slope_threshold) throws KduException
  {
    Encode(_block,_reversible,_msb_wmse,_min_slope_threshold,_max_slope_threshold,(boolean) false);
  }
  public native void Cellular_encode(Kdu_block _block, boolean _reversible, double _msb_wmse, float[] _cell_weights, int _first_cell_cols, int _first_cell_rows, int _min_slope_threshold, int _max_slope_threshold, boolean _use_existing_slopes) throws KduException;
  public void Cellular_encode(Kdu_block _block, boolean _reversible, double _msb_wmse, float[] _cell_weights, int _first_cell_cols, int _first_cell_rows, int _min_slope_threshold, int _max_slope_threshold) throws KduException
  {
    Cellular_encode(_block,_reversible,_msb_wmse,_cell_weights,_first_cell_cols,_first_cell_rows,_min_slope_threshold,_max_slope_threshold,(boolean) false);
  }
  public native boolean Encode16(Kdu_block _block, int[] _data, boolean _reversible, double _msb_wmse, int _min_slope_threshold, int _max_slope_threshold) throws KduException;
  public native boolean Encode32(Kdu_block _block, long[] _data, boolean _reversible, double _msb_wmse, int _min_slope_threshold, int _max_slope_threshold) throws KduException;
}
