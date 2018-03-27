package kdu_jni;

public class Kdu_coords {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_coords(long ptr) {
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
  public Kdu_coords() {
    this(Native_create());
  }
  private static native long Native_create(int _x, int _y);
  public Kdu_coords(int _x, int _y) {
    this(Native_create(_x, _y));
  }
  private static native long Native_create(Kdu_nc_coords _src);
  public Kdu_coords(Kdu_nc_coords _src) {
    this(Native_create(_src));
  }
  public native void From_double(double _x_fval, double _y_fval) throws KduException;
  public native void Assign(Kdu_coords _src) throws KduException;
  public native void Assign(Kdu_nc_coords _src) throws KduException;
  public native int Get_x() throws KduException;
  public native int Get_y() throws KduException;
  public native void Set_x(int _x) throws KduException;
  public native void Set_y(int _y) throws KduException;
  public native void Transpose() throws KduException;
  public native Kdu_coords Plus(Kdu_coords _rhs) throws KduException;
  public native Kdu_coords Minus(Kdu_coords _rhs) throws KduException;
  public native Kdu_coords Add(Kdu_coords _rhs) throws KduException;
  public native Kdu_coords Subtract(Kdu_coords _rhs) throws KduException;
  public native boolean Equals(Kdu_coords _rhs) throws KduException;
  public native void From_apparent(boolean _transp, boolean _vflip, boolean _hflip) throws KduException;
  public native void To_apparent(boolean _transp, boolean _vflip, boolean _hflip) throws KduException;
}
