package kdu_jni;

public class Jpx_layer_target {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jpx_layer_target(long ptr) {
    _native_ptr = ptr;
  }
  public Jpx_layer_target() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native int Get_layer_id() throws KduException;
  public native Jp2_channels Access_channels() throws KduException;
  public native Jp2_resolution Access_resolution() throws KduException;
  public native Jp2_colour Add_colour(int _prec, byte _approx) throws KduException;
  public Jp2_colour Add_colour() throws KduException
  {
    return Add_colour((int) 0,(byte) 0);
  }
  public Jp2_colour Add_colour(int _prec) throws KduException
  {
    return Add_colour(_prec,(byte) 0);
  }
  public native Jp2_colour Access_colour(int _which) throws KduException;
  public native void Set_codestream_registration(int _codestream_id, Kdu_coords _alignment, Kdu_coords _sampling, Kdu_coords _denominator) throws KduException;
  public native void Copy_attributes(Jpx_layer_source _src) throws KduException;
}
