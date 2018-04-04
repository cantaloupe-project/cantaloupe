package kdu_jni;

public class Mj2_video_target extends Kdu_compressed_video_target {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Mj2_video_target(long ptr) {
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
  public Mj2_video_target() {
    this(Native_create());
  }
  public native long Get_track_idx() throws KduException;
  public native boolean Set_compositing_order(short _layer_idx) throws KduException;
  public native boolean Set_graphics_mode(short _graphics_mode, short _op_red, short _op_green, short _op_blue) throws KduException;
  public boolean Set_graphics_mode(short _graphics_mode) throws KduException
  {
    return Set_graphics_mode(_graphics_mode,(short) 0,(short) 0,(short) 0);
  }
  public boolean Set_graphics_mode(short _graphics_mode, short _op_red) throws KduException
  {
    return Set_graphics_mode(_graphics_mode,_op_red,(short) 0,(short) 0);
  }
  public boolean Set_graphics_mode(short _graphics_mode, short _op_red, short _op_green) throws KduException
  {
    return Set_graphics_mode(_graphics_mode,_op_red,_op_green,(short) 0);
  }
  public native Jp2_colour Access_colour() throws KduException;
  public native Jp2_palette Access_palette() throws KduException;
  public native Jp2_channels Access_channels() throws KduException;
  public native Jp2_resolution Access_resolution() throws KduException;
  public native boolean Set_timescale(long _ticks_per_second) throws KduException;
  public native boolean Set_field_order(int _order) throws KduException;
  public native boolean Set_max_frames_per_chunk(long _max_frames) throws KduException;
  public native boolean Set_frame_period(long _num_ticks) throws KduException;
}
