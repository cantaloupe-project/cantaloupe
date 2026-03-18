package kdu_jni;

public class Kdu_membacked_compressed_source extends Kdu_compressed_source implements AutoCloseable {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_membacked_compressed_source(long ptr) {
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

  public native double Get_timestamp() throws KduException;
  public native int Get_term_code() throws KduException;
  public native boolean Finished_loading() throws KduException;
}
