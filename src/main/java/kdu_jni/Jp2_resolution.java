package kdu_jni;

public class Jp2_resolution {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jp2_resolution(long ptr) {
    _native_ptr = ptr;
  }
  public Jp2_resolution() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native void Copy(Jp2_resolution _src) throws KduException;
  public native boolean Init(float _aspect_ratio) throws KduException;
  public native boolean Set_different_capture_aspect_ratio(float _aspect_ratio) throws KduException;
  public native boolean Set_resolution(float _resolution, boolean _for_display) throws KduException;
  public boolean Set_resolution(float _resolution) throws KduException
  {
    return Set_resolution(_resolution,(boolean) true);
  }
  public native float Get_aspect_ratio(boolean _for_display) throws KduException;
  public float Get_aspect_ratio() throws KduException
  {
    return Get_aspect_ratio((boolean) true);
  }
  public native float Get_resolution(boolean _for_display) throws KduException;
  public float Get_resolution() throws KduException
  {
    return Get_resolution((boolean) true);
  }
}
