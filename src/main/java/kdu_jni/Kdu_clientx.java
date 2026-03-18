package kdu_jni;

public class Kdu_clientx extends Kdu_client_translator implements AutoCloseable {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_clientx(long ptr) {
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
  public Kdu_clientx() {
    this(Native_create());
  }
}
