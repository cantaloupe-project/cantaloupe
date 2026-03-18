package kdu_jni;

public class Atk_params extends Kdu_params implements AutoCloseable {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Atk_params(long ptr) {
    super(ptr);
  }
  public native void Native_destroy();
  @Override
  public void close() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }

  private static native long Native_create();
  public Atk_params() {
    this(Native_create());
  }
  public native boolean Is_predef_kernel_symmetric(int _kernel_idx) throws KduException;
}
