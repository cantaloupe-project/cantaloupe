package kdu_jni;

public class Kdu_compressed_source_nonnative extends Kdu_compressed_source {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_compressed_source_nonnative(long ptr) {
    super(ptr);
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create();
  private native void Native_init();
  public Kdu_compressed_source_nonnative() {
    this(Native_create());
    this.Native_init();
  }
  public int Get_capabilities() throws KduException
  {
    // Override in a derived class to respond to the callback
    return (int) 0;
  }
  public boolean Seek(long _offset) throws KduException
  {
    // Override in a derived class to respond to the callback
    return false;
  }
  public long Get_pos() throws KduException
  {
    // Override in a derived class to respond to the callback
    return (long) 0;
  }
  public boolean Set_tileheader_scope(int _tnum, int _num_tiles) throws KduException
  {
    // Override in a derived class to respond to the callback
    return false;
  }
  public boolean Set_precinct_scope(long _unique_id) throws KduException
  {
    // Override in a derived class to respond to the callback
    return false;
  }
  public int Post_read(int _num_bytes) throws KduException
  {
    // Override in a derived class to respond to the callback
    return (int) 0;
  }
  public native void Push_data(byte[] _data, int _first_byte_pos, int _num_bytes) throws KduException;
}
