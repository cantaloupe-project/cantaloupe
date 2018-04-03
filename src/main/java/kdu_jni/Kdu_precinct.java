package kdu_jni;

public class Kdu_precinct {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_precinct(long ptr) {
    _native_ptr = ptr;
  }
  public Kdu_precinct() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native boolean Check_loaded() throws KduException;
  public native long Get_unique_id() throws KduException;
  public native boolean Get_valid_blocks(int _band_idx, Kdu_dims _indices) throws KduException;
  public native Kdu_block Open_block(int _band_idx, Kdu_coords _block_idx, Kdu_thread_env _env) throws KduException;
  public Kdu_block Open_block(int _band_idx, Kdu_coords _block_idx) throws KduException
  {
    Kdu_thread_env env = null;
    return Open_block(_band_idx,_block_idx,env);
  }
  public native void Close_block(Kdu_block _block, Kdu_thread_env _env) throws KduException;
  public void Close_block(Kdu_block _block) throws KduException
  {
    Kdu_thread_env env = null;
    Close_block(_block,env);
  }
  public native boolean Size_packets(int[] _cumulative_packets, int[] _cumulative_bytes, boolean[] _is_significant) throws KduException;
  public native boolean Get_packets(int _leading_skip_packets, int _leading_skip_bytes, int[] _cumulative_packets, int[] _cumulative_bytes, Kdu_output _out) throws KduException;
  public native void Restart() throws KduException;
  public native void Close(Kdu_thread_env _env) throws KduException;
  public void Close() throws KduException
  {
    Kdu_thread_env env = null;
    Close(env);
  }
}
