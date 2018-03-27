package kdu_jni;

public class Kdu_sample_allocator {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_sample_allocator(long ptr) {
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
  public Kdu_sample_allocator() {
    this(Native_create());
  }
  public native boolean Configure(Kdu_membroker _membroker, int _frag_bits) throws KduException;
  public native Kdu_membroker Get_membroker() throws KduException;
  public native void Restart() throws KduException;
  public native void Release() throws KduException;
  public native void Finalize(Kdu_codestream _codestream) throws KduException;
}
