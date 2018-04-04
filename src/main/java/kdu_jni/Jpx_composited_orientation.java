package kdu_jni;

public class Jpx_composited_orientation {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jpx_composited_orientation(long ptr) {
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
  public Jpx_composited_orientation() {
    this(Native_create());
  }
  private static native long Native_create(boolean _transpose_first, boolean _flip_vert, boolean _flip_horz);
  public Jpx_composited_orientation(boolean _transpose_first, boolean _flip_vert, boolean _flip_horz) {
    this(Native_create(_transpose_first, _flip_vert, _flip_horz));
  }
  public native boolean Is_non_trivial() throws KduException;
  public native boolean Equals(Jpx_composited_orientation _rhs) throws KduException;
  public native void Init(boolean _transpose_first, boolean _flip_vert, boolean _flip_horz) throws KduException;
  public native void Init(int _rotation, boolean _flip) throws KduException;
  public native void Append(Jpx_composited_orientation _rhs) throws KduException;
}
