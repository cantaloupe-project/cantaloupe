package kdu_jni;

public class Jp2_family_src {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jp2_family_src(long ptr) {
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
  public Jp2_family_src() {
    this(Native_create());
  }
  public native boolean Exists() throws KduException;
  public native void Open(String _fname, boolean _allow_seeks, Kdu_membroker _membroker) throws KduException;
  public void Open(String _fname) throws KduException
  {
    Kdu_membroker membroker = null;
    Open(_fname,(boolean) true,membroker);
  }
  public void Open(String _fname, boolean _allow_seeks) throws KduException
  {
    Kdu_membroker membroker = null;
    Open(_fname,_allow_seeks,membroker);
  }
  public native void Open(Kdu_compressed_source _indirect, Kdu_membroker _membroker) throws KduException;
  public void Open(Kdu_compressed_source _indirect) throws KduException
  {
    Kdu_membroker membroker = null;
    Open(_indirect,membroker);
  }
  public native void Open(Kdu_cache _cache, Kdu_membroker _membroker) throws KduException;
  public void Open(Kdu_cache _cache) throws KduException
  {
    Kdu_membroker membroker = null;
    Open(_cache,membroker);
  }
  public native void Close(boolean _partial) throws KduException;
  public void Close() throws KduException
  {
    Close((boolean) false);
  }
  public native Kdu_membroker Get_membroker() throws KduException;
  public native boolean Uses_cache() throws KduException;
  public native boolean Is_top_level_complete() throws KduException;
  public native boolean Is_codestream_main_header_complete(long _logical_codestream_idx) throws KduException;
  public native int Get_id() throws KduException;
  public native String Get_filename() throws KduException;
  public native void Acquire_lock() throws KduException;
  public native void Release_lock() throws KduException;
  public native void Synch_with_cache() throws KduException;
}
