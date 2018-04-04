package kdu_jni;

public class Jp2_target extends Jp2_output_box {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Jp2_target(long ptr) {
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
  public Jp2_target() {
    this(Native_create());
  }
  public native void Open(Jp2_family_tgt _tgt, Kdu_membroker _membroker) throws KduException;
  public void Open(Jp2_family_tgt _tgt) throws KduException
  {
    Kdu_membroker membroker = null;
    Open(_tgt,membroker);
  }
  public native void Open(Jp2_family_tgt _tgt, long _box_type, boolean _rubber_length) throws KduException;
  public void Open(Jp2_family_tgt _tgt, long _box_type) throws KduException
  {
    Open(_tgt,_box_type,(boolean) false);
  }
  public native void Open(Jp2_output_box _super_box, long _box_type, boolean _rubber_length) throws KduException;
  public void Open(Jp2_output_box _super_box, long _box_type) throws KduException
  {
    Open(_super_box,_box_type,(boolean) false);
  }
  public native boolean Close_box() throws KduException;
  public native void Write_header() throws KduException;
  public native void Open_codestream(boolean _rubber_length) throws KduException;
  public void Open_codestream() throws KduException
  {
    Open_codestream((boolean) true);
  }
  public native Jp2_dimensions Access_dimensions() throws KduException;
  public native Jp2_colour Access_colour() throws KduException;
  public native Jp2_palette Access_palette() throws KduException;
  public native Jp2_channels Access_channels() throws KduException;
  public native Jp2_resolution Access_resolution() throws KduException;
}
