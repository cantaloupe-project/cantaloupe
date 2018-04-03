package kdu_jni;

public class Jpx_fragment_list {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jpx_fragment_list(long ptr) {
    _native_ptr = ptr;
  }
  public Jpx_fragment_list() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native boolean Read_only() throws KduException;
  public native boolean Add_fragment(int _url_idx, long _offset, long _length) throws KduException;
  public native long Get_total_length() throws KduException;
  public native int Get_num_fragments() throws KduException;
  public native boolean Get_fragment(int _frag_idx, int[] _url_idx, long[] _offset, long[] _length) throws KduException;
  public native int Locate_fragment(long _pos, long[] _bytes_into_fragment) throws KduException;
  public native boolean Any_local_fragments() throws KduException;
}
