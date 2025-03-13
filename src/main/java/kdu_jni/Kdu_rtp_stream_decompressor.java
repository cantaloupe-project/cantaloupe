package kdu_jni;

public class Kdu_rtp_stream_decompressor extends Kdu_ovlp_stream_decompressor {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_rtp_stream_decompressor(long ptr) {
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
  public Kdu_rtp_stream_decompressor() {
    this(Native_create());
  }
  public native void Configure(Kdu_rtp_stream_fetch_state _common_fetch_state) throws KduException;
}
