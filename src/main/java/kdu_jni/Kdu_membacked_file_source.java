package kdu_jni;

public class Kdu_membacked_file_source extends Kdu_membacked_compressed_source {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_membacked_file_source(long ptr) {
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
  public Kdu_membacked_file_source() {
    this(Native_create());
  }
  public native boolean Open(String _fname, int _min_mode, int _quantum_bytes, double _max_bytes_per_second) throws KduException;
  public boolean Open(String _fname, int _min_mode) throws KduException
  {
    return Open(_fname,_min_mode,(int) 16384,(double) 0.0);
  }
  public boolean Open(String _fname, int _min_mode, int _quantum_bytes) throws KduException
  {
    return Open(_fname,_min_mode,_quantum_bytes,(double) 0.0);
  }
}
