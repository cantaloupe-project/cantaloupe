package kdu_jni;

public class Jpb_source extends Kdu_compressed_video_source {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Jpb_source(long ptr) {
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
  public Jpb_source() {
    this(Native_create());
  }
  public native boolean Exists() throws KduException;
  public native int Open(Jp2_family_src _src, boolean _return_if_incompatible) throws KduException;
  public int Open(Jp2_family_src _src) throws KduException
  {
    return Open(_src,(boolean) false);
  }
  public native Jp2_family_src Get_ultimate_src() throws KduException;
  public native byte Get_frame_space() throws KduException;
  public native boolean Get_mastering_display_info(float[] _x0, float[] _y0, float[] _x1, float[] _y1, float[] _x2, float[] _y2, float[] _xw, float[] _yw, double[] _Lmin, double[] _Lmax) throws KduException;
  public native long Get_frame_timecode() throws KduException;
}
