package kdu_jni;

public class Jp2_colour {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jp2_colour(long ptr) {
    _native_ptr = ptr;
  }
  public Jp2_colour() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native void Copy(Jp2_colour _src) throws KduException;
  public native void Init(int _space) throws KduException;
  public native void Init(int _space, int _Lrange, int _Loff, int _Lbits, int _Arange, int _Aoff, int _Abits, int _Brange, int _Boff, int _Bbits, long _illuminant, int _temperature) throws KduException;
  public void Init(int _space, int _Lrange, int _Loff, int _Lbits, int _Arange, int _Aoff, int _Abits, int _Brange, int _Boff, int _Bbits, long _illuminant) throws KduException
  {
    Init(_space,_Lrange,_Loff,_Lbits,_Arange,_Aoff,_Abits,_Brange,_Boff,_Bbits,_illuminant,(int) 5000);
  }
  public native void Init(byte[] _icc_profile) throws KduException;
  public native void Init(byte[] _uuid, int _data_bytes, byte[] _data) throws KduException;
  public native void Init(double _gamma, double _beta, int _num_points) throws KduException;
  public void Init(double _gamma) throws KduException
  {
    Init(_gamma,(double) 0.0F,(int) 100);
  }
  public void Init(double _gamma, double _beta) throws KduException
  {
    Init(_gamma,_beta,(int) 100);
  }
  public native void Init(double[] _xy_red, double[] _xy_green, double[] _xy_blue, double _gamma, double _beta, int _num_points, boolean _reference_is_D50) throws KduException;
  public void Init(double[] _xy_red, double[] _xy_green, double[] _xy_blue, double _gamma) throws KduException
  {
    Init(_xy_red,_xy_green,_xy_blue,_gamma,(double) 0.0,(int) 100,(boolean) false);
  }
  public void Init(double[] _xy_red, double[] _xy_green, double[] _xy_blue, double _gamma, double _beta) throws KduException
  {
    Init(_xy_red,_xy_green,_xy_blue,_gamma,_beta,(int) 100,(boolean) false);
  }
  public void Init(double[] _xy_red, double[] _xy_green, double[] _xy_blue, double _gamma, double _beta, int _num_points) throws KduException
  {
    Init(_xy_red,_xy_green,_xy_blue,_gamma,_beta,_num_points,(boolean) false);
  }
  public native boolean Is_jp2_compatible() throws KduException;
  public native boolean Is_jph_compatible() throws KduException;
  public native int Get_num_colours() throws KduException;
  public native int Get_space() throws KduException;
  public native boolean Is_opponent_space() throws KduException;
  public native float Get_natural_unsigned_zero_point(int _channel_idx) throws KduException;
  public native int Get_precedence() throws KduException;
  public native byte Get_approximation_level() throws KduException;
  public native long Get_icc_profile(int[] _num_bytes) throws KduException;
  public long Get_icc_profile() throws KduException
  {
    return Get_icc_profile(null);
  }
  public native int Get_icc_profile(byte[] _buffer, int _buf_len) throws KduException;
  public native boolean Get_lab_params(int[] _Lrange, int[] _Loff, int[] _Lbits, int[] _Arange, int[] _Aoff, int[] _Abits, int[] _Brange, int[] _Boff, int[] _Bbits, long[] _illuminant, int[] _temperature) throws KduException;
  public native boolean Get_jab_params(int[] _Lrange, int[] _Loff, int[] _Lbits, int[] _Arange, int[] _Aoff, int[] _Abits, int[] _Brange, int[] _Boff, int[] _Bbits) throws KduException;
  public native boolean Check_cie_default() throws KduException;
  public native boolean Get_vendor_uuid(byte[] _uuid) throws KduException;
  public native long Get_vendor_data(int[] _num_bytes) throws KduException;
  public long Get_vendor_data() throws KduException
  {
    return Get_vendor_data(null);
  }
}
