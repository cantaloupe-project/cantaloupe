package kdu_jni;

public class Kdu_compositor_buf {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_compositor_buf(long ptr) {
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
  public Kdu_compositor_buf() {
    this(Native_create());
  }
  public native void Init(long _buf, int _row_gap) throws KduException;
  public native void Init_float(long _float_buf, int _row_gap) throws KduException;
  public native boolean Is_read_access_allowed() throws KduException;
  public native boolean Set_read_accessibility(boolean _read_access_required) throws KduException;
  public native long Get_buf(int[] _row_gap, boolean _read_write) throws KduException;
  public native long Get_float_buf(int[] _row_gap, boolean _read_write) throws KduException;
  public native float Get_rendering_scale() throws KduException;
  public native Kdu_dims Get_rendering_region() throws KduException;
  public native Kdu_dims Get_rendering_composition_dims() throws KduException;
  public native boolean Get_rendering_status(int[] _num_rendered_rows, int[] _first_new_row, boolean _reset) throws KduException;
  public native int Get_rendered_rows() throws KduException;
  public native boolean Get_region(Kdu_dims _src_region, int[] _tgt_buf, int _tgt_offset, int _tgt_row_gap) throws KduException;
  public boolean Get_region(Kdu_dims _src_region, int[] _tgt_buf) throws KduException
  {
    return Get_region(_src_region,_tgt_buf,(int) 0,(int) 0);
  }
  public boolean Get_region(Kdu_dims _src_region, int[] _tgt_buf, int _tgt_offset) throws KduException
  {
    return Get_region(_src_region,_tgt_buf,_tgt_offset,(int) 0);
  }
  public native boolean Get_float_region(Kdu_dims _src_region, float[] _tgt_buf, int _tgt_offset, int _tgt_row_gap) throws KduException;
  public boolean Get_float_region(Kdu_dims _src_region, float[] _tgt_buf) throws KduException
  {
    return Get_float_region(_src_region,_tgt_buf,(int) 0,(int) 0);
  }
  public boolean Get_float_region(Kdu_dims _src_region, float[] _tgt_buf, int _tgt_offset) throws KduException
  {
    return Get_float_region(_src_region,_tgt_buf,_tgt_offset,(int) 0);
  }
  public native boolean Need_external_delete() throws KduException;
  public native void Set_geometry(float _scale, Kdu_dims _surface, Kdu_dims _composition_dims) throws KduException;
  public native void Set_invalid() throws KduException;
  public native void Renovate() throws KduException;
  public native void Reset_rendered_rows() throws KduException;
  public native void Note_new_rendered_rows(int _first_row, int _num_rows) throws KduException;
}
