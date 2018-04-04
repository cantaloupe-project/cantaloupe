package kdu_jni;

public class Kdu_compressed_source {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_compressed_source(long ptr) {
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
  public native int Read(byte[] _buf, int _num_bytes) throws KduException;
  public native boolean Seek(long _offset) throws KduException;
  public native long Get_pos() throws KduException;
  public native boolean Set_tileheader_scope(int _tnum, int _num_tiles) throws KduException;
  public native boolean Set_precinct_scope(long _unique_id) throws KduException;
}
