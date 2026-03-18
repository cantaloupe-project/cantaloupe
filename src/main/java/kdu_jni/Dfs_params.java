package kdu_jni;

public class Dfs_params extends Kdu_params implements AutoCloseable {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Dfs_params(long ptr) {
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
  public Dfs_params() {
    this(Native_create());
  }
}
