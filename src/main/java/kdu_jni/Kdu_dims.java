package kdu_jni;

public class Kdu_dims {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_dims(long ptr) {
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
  public Kdu_dims() {
    this(Native_create());
  }
  public native void Assign(Kdu_dims _src) throws KduException;
  public native void From_u32(long _x0, long _y0, long _width, long _height) throws KduException;
  public native void From_double(double _x0, double _y0, double _width, double _height) throws KduException;
  public native Kdu_coords Access_pos() throws KduException;
  public native Kdu_coords Access_size() throws KduException;
  public native long Area() throws KduException;
  public native int Area32() throws KduException;
  public native void Transpose() throws KduException;
  public native Kdu_dims Intersection(Kdu_dims _rhs) throws KduException;
  public native boolean Intersects(Kdu_dims _rhs) throws KduException;
  public native boolean Is_empty() throws KduException;
  public native boolean Equals(Kdu_dims _rhs) throws KduException;
  public native boolean Contains(Kdu_coords _rhs) throws KduException;
  public native boolean Contains(Kdu_dims _rhs) throws KduException;
  public native void Augment(Kdu_coords _p) throws KduException;
  public native void Augment(Kdu_dims _src) throws KduException;
  public native boolean Clip_point(Kdu_coords _pt) throws KduException;
  public native void From_apparent(boolean _transp, boolean _vflip, boolean _hflip) throws KduException;
  public native void To_apparent(boolean _transp, boolean _vflip, boolean _hflip) throws KduException;
}
