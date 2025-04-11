package kdu_jni;

public class Kdu_mjc_stream_fetch_state {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_mjc_stream_fetch_state(long ptr) {
    _native_ptr = ptr;
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  public native void Init(long _fixed_length, double _frame_interval, boolean _is_ycc) throws KduException;
  public native int Read_bytes(byte[] _buf, int _num) throws KduException;
}
