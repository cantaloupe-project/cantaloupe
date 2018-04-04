package kdu_jni;

public class Kdu_simple_video_target extends Kdu_compressed_video_target {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_simple_video_target(long ptr) {
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
  public Kdu_simple_video_target() {
    this(Native_create());
  }
  private static native long Native_create(String _fname, long _timescale, long _frame_period, long _flags, Kdu_membroker _broker);
  public Kdu_simple_video_target(String _fname, long _timescale, long _frame_period, long _flags, Kdu_membroker _broker) {
    this(Native_create(_fname, _timescale, _frame_period, _flags, _broker));
  }
  private static long Native_create(String _fname, long _timescale, long _frame_period, long _flags)
  {
    Kdu_membroker broker = null;
    return Native_create(_fname,_timescale,_frame_period,_flags,broker);
  }
  public Kdu_simple_video_target(String _fname, long _timescale, long _frame_period, long _flags) {
    this(Native_create(_fname, _timescale, _frame_period, _flags));
  }
  public native boolean Exists() throws KduException;
  public native void Open(String _fname, long _timescale, long _frame_period, long _flags, Kdu_membroker _broker) throws KduException;
  public void Open(String _fname, long _timescale, long _frame_period, long _flags) throws KduException
  {
    Kdu_membroker broker = null;
    Open(_fname,_timescale,_frame_period,_flags,broker);
  }
  public native void Set_fixed_length(long _fixed_length) throws KduException;
  public native boolean Attach_as_view(Kdu_simple_video_target _master_target) throws KduException;
}
