package kdu_jni;

public class Kdu_compressed_source_buffered extends Kdu_compressed_source implements AutoCloseable {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_compressed_source_buffered(long ptr) {
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
  public Kdu_compressed_source_buffered() {
    this(Native_create());
  }
}
