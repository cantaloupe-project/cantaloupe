package kdu_jni;

public class Kdu_rtp_stream_fetch_state {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_rtp_stream_fetch_state(long ptr) {
    _native_ptr = ptr;
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  public native void Init(long _target_ssid, boolean _target_ssid_valid, int _format) throws KduException;
  public native void Configure_codestream_handling(int _initial_discard_count, int _initial_intercept_count, boolean _strip_intercepted_headers, double _looping_gap) throws KduException;
}
