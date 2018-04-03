package kdu_jni;

public class Kdu_block_encoder {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_block_encoder(long ptr) {
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
  public Kdu_block_encoder() {
    this(Native_create());
  }
  public native void Encode(Kdu_block _block, boolean _reversible, double _msb_wmse, int _estimated_slope_threshold) throws KduException;
  public void Encode(Kdu_block _block) throws KduException
  {
    Encode(_block,(boolean) false,(double) 0.0F,(int) 0);
  }
  public void Encode(Kdu_block _block, boolean _reversible) throws KduException
  {
    Encode(_block,_reversible,(double) 0.0F,(int) 0);
  }
  public void Encode(Kdu_block _block, boolean _reversible, double _msb_wmse) throws KduException
  {
    Encode(_block,_reversible,_msb_wmse,(int) 0);
  }
  public native void Cellular_encode(Kdu_block _block, boolean _reversible, double _msb_wmse, float[] _cell_weights, int _first_cell_cols, int _first_cell_rows, int _estimated_slope_threshold) throws KduException;
}
