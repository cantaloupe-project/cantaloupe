package kdu_jni;

public class Jp2_family_tgt {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jp2_family_tgt(long ptr) {
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
  public Jp2_family_tgt() {
    this(Native_create());
  }
  public native boolean Exists() throws KduException;
  public native void Open(String _fname, Kdu_membroker _membroker) throws KduException;
  public void Open(String _fname) throws KduException
  {
    Kdu_membroker membroker = null;
    Open(_fname,membroker);
  }
  public native void Open(Kdu_compressed_target _indirect, Kdu_membroker _membroker) throws KduException;
  public void Open(Kdu_compressed_target _indirect) throws KduException
  {
    Kdu_membroker membroker = null;
    Open(_indirect,membroker);
  }
  public native void Open(long _simulated_start_pos, Kdu_membroker _membroker) throws KduException;
  public void Open(long _simulated_start_pos) throws KduException
  {
    Kdu_membroker membroker = null;
    Open(_simulated_start_pos,membroker);
  }
  public native long Get_bytes_written() throws KduException;
  public native void Close() throws KduException;
  public native Kdu_membroker Get_membroker() throws KduException;
}
