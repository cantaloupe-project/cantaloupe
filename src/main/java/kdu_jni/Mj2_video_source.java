package kdu_jni;

public class Mj2_video_source extends Kdu_compressed_video_source {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Mj2_video_source(long ptr) {
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
  public Mj2_video_source() {
    this(Native_create());
  }
  public native long Get_track_idx() throws KduException;
  public native short Get_compositing_order() throws KduException;
  public native short Get_graphics_mode(short[] _op_red, short[] _op_green, short[] _op_blue) throws KduException;
  public native short Get_graphics_mode() throws KduException;
  public native boolean Get_geometry(double[] _presentation_width, double[] _presentation_height, double[] _matrix, boolean _for_movie) throws KduException;
  public boolean Get_geometry(double[] _presentation_width, double[] _presentation_height, double[] _matrix) throws KduException
  {
    return Get_geometry(_presentation_width,_presentation_height,_matrix,(boolean) true);
  }
  public native boolean Get_cardinal_geometry(Kdu_dims _pre_dims, boolean[] _transpose, boolean[] _vflip, boolean[] _hflip, boolean _for_movie) throws KduException;
  public boolean Get_cardinal_geometry(Kdu_dims _pre_dims, boolean[] _transpose, boolean[] _vflip, boolean[] _hflip) throws KduException
  {
    return Get_cardinal_geometry(_pre_dims,_transpose,_vflip,_hflip,(boolean) true);
  }
  public native Jp2_dimensions Access_dimensions() throws KduException;
  public native Jp2_resolution Access_resolution() throws KduException;
  public native Jp2_palette Access_palette() throws KduException;
  public native Jp2_channels Access_channels() throws KduException;
  public native Jp2_colour Access_colour() throws KduException;
  public native int Get_stream_idx(int _field_idx) throws KduException;
  public native Jp2_input_box Access_image_box() throws KduException;
  public native boolean Can_open_stream(int _field_idx, boolean _need_main_header) throws KduException;
  public boolean Can_open_stream(int _field_idx) throws KduException
  {
    return Can_open_stream(_field_idx,(boolean) true);
  }
  public native long Get_frame_instant(int _frame_idx) throws KduException;
  public native long Get_frame_period(int _frame_idx) throws KduException;
}
