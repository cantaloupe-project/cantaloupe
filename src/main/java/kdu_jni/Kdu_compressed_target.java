package kdu_jni;

public class Kdu_compressed_target {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_compressed_target(long ptr) {
    _native_ptr = ptr;
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  public native boolean Close() throws KduException;
  public native int Get_capabilities() throws KduException;
  public native Kdu_membroker Get_membroker() throws KduException;
  public native void Start_mainheader() throws KduException;
  public native void End_mainheader() throws KduException;
  public native void Start_tileheader(int _tnum, int _num_tiles) throws KduException;
  public native void End_tileheader(int _tnum) throws KduException;
  public native void Start_precinct(long _unique_id) throws KduException;
  public native void End_precinct(long _unique_id, int _num_packets, long[] _packet_lengths) throws KduException;
  public native boolean Start_rewrite(long _backtrack) throws KduException;
  public native boolean End_rewrite() throws KduException;
  public native boolean Write(byte[] _buf, int _num_bytes) throws KduException;
  public native void Set_target_size(long _num_bytes) throws KduException;
}
