package kdu_jni;

public class Kdu_line_buf {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_line_buf(long ptr) {
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
  public Kdu_line_buf() {
    this(Native_create());
  }
  public native void Destroy() throws KduException;
  public native void Pre_create(Kdu_sample_allocator _allocator, int _width, boolean _absolute, boolean _use_shorts, int _extend_left, int _extend_right) throws KduException;
  public native void Create() throws KduException;
  public native int Check_status() throws KduException;
  public native void Set_exchangeable() throws KduException;
  public native boolean Exchange(Kdu_line_buf _src) throws KduException;
  public native long Get_buf() throws KduException;
  public native boolean Get_floats(float[] _buffer, int _first_idx, int _num_samples) throws KduException;
  public native boolean Set_floats(float[] _buffer, int _first_idx, int _num_samples) throws KduException;
  public native boolean Get_ints(int[] _buffer, int _first_idx, int _num_samples) throws KduException;
  public native boolean Set_ints(int[] _buffer, int _first_idx, int _num_samples) throws KduException;
  public native boolean Get_ints(short[] _buffer, int _first_idx, int _num_samples) throws KduException;
  public native boolean Set_ints(short[] _buffer, int _first_idx, int _num_samples) throws KduException;
  public native int Get_width() throws KduException;
  public native boolean Is_absolute() throws KduException;
}
