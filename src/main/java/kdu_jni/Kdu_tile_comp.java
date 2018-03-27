package kdu_jni;

public class Kdu_tile_comp {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_tile_comp(long ptr) {
    _native_ptr = ptr;
  }
  public Kdu_tile_comp() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native boolean Get_reversible() throws KduException;
  public native void Get_subsampling(Kdu_coords _factors) throws KduException;
  public native int Get_bit_depth(boolean _internal) throws KduException;
  public int Get_bit_depth() throws KduException
  {
    return Get_bit_depth((boolean) false);
  }
  public native boolean Get_signed() throws KduException;
  public native int Get_num_resolutions() throws KduException;
  public native Kdu_resolution Access_resolution(int _res_level) throws KduException;
  public native Kdu_resolution Access_resolution() throws KduException;
}
