package kdu_jni;

public class Jp2_threadsafe_family_src extends Jp2_family_src implements AutoCloseable {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Jp2_threadsafe_family_src(long ptr) {
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
  @Override
  @SuppressWarnings("removal")
  protected void finalize() {
    close();
  }
  private static native long Native_create();
  public Jp2_threadsafe_family_src() {
    this(Native_create());
  }
}
