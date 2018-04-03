package kdu_jni;

public class Jpb_target extends Kdu_compressed_video_target {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Jpb_target(long ptr) {
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
  public Jpb_target() {
    this(Native_create());
  }
  public native boolean Exists() throws KduException;
  public native void Open(Jp2_family_tgt _tgt, int _timescale, int _frame_duration, int _field_order, byte _frame_space, long _max_bitrate, long _initial_timecode, int _timecode_flags) throws KduException;
  public native void Set_next_timecode(long _timecode) throws KduException;
  public native long Get_next_timecode() throws KduException;
  public native long Get_last_timecode() throws KduException;
  public native void Set_mastering_display_info(float _x0, float _y0, float _x1, float _y1, float _x2, float _y2, float _xw, float _yw, double _Lmin, double _Lmax) throws KduException;
}
