package kdu_jni;

public class Jp2_source extends Jp2_input_box {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Jp2_source(long ptr) {
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
  public Jp2_source() {
    this(Native_create());
  }
  public native int Read_header(boolean _return_if_incompatible) throws KduException;
  public int Read_header() throws KduException
  {
    return Read_header((boolean) false);
  }
  public native long Get_brand() throws KduException;
  public native long Get_header_bytes() throws KduException;
  public native Jp2_dimensions Access_dimensions() throws KduException;
  public native Jp2_palette Access_palette() throws KduException;
  public native Jp2_channels Access_channels() throws KduException;
  public native Jp2_colour Access_colour() throws KduException;
  public native Jp2_resolution Access_resolution() throws KduException;
}
