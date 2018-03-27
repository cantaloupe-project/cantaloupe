package kdu_jni;

public class Kdu_thread_env extends Kdu_thread_entity {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_thread_env(long ptr) {
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
  public Kdu_thread_env() {
    this(Native_create());
  }
  public native Kdu_block Get_block() throws KduException;
  public native boolean Cs_terminate(Kdu_codestream _codestream, int[] _exc_code) throws KduException;
  public boolean Cs_terminate(Kdu_codestream _codestream) throws KduException
  {
    return Cs_terminate(_codestream,null);
  }
}
