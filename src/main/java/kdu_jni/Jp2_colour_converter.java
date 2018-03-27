package kdu_jni;

public class Jp2_colour_converter {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jp2_colour_converter(long ptr) {
    _native_ptr = ptr;
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create();
  public Jp2_colour_converter() {
    this(Native_create());
  }
  public native void Clear() throws KduException;
  public native boolean Init(Jp2_colour _colour, boolean _use_wide_gamut, boolean _prefer_fast_approximations) throws KduException;
  public boolean Init(Jp2_colour _colour) throws KduException
  {
    return Init(_colour,(boolean) false,(boolean) false);
  }
  public boolean Init(Jp2_colour _colour, boolean _use_wide_gamut) throws KduException
  {
    return Init(_colour,_use_wide_gamut,(boolean) false);
  }
  public native boolean Exists() throws KduException;
  public native boolean Get_wide_gamut() throws KduException;
  public native boolean Is_approximate() throws KduException;
  public native boolean Is_non_trivial() throws KduException;
  public native boolean Get_channel_info(int _idx, float[] _square_weight, boolean[] _is_chroma) throws KduException;
  public native boolean Convert_lum(Kdu_line_buf _line, int _width) throws KduException;
  public boolean Convert_lum(Kdu_line_buf _line) throws KduException
  {
    return Convert_lum(_line,(int) -1);
  }
  public native boolean Convert_rgb(Kdu_line_buf _red, Kdu_line_buf _green, Kdu_line_buf _blue, int _width) throws KduException;
  public boolean Convert_rgb(Kdu_line_buf _red, Kdu_line_buf _green, Kdu_line_buf _blue) throws KduException
  {
    return Convert_rgb(_red,_green,_blue,(int) -1);
  }
  public native boolean Convert_rgb4(Kdu_line_buf _red, Kdu_line_buf _green, Kdu_line_buf _blue, Kdu_line_buf _extra, int _width) throws KduException;
  public boolean Convert_rgb4(Kdu_line_buf _red, Kdu_line_buf _green, Kdu_line_buf _blue, Kdu_line_buf _extra) throws KduException
  {
    return Convert_rgb4(_red,_green,_blue,_extra,(int) -1);
  }
}
