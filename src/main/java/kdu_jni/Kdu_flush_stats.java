package kdu_jni;

public class Kdu_flush_stats {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_flush_stats(long ptr) {
    _native_ptr = ptr;
  }
  public Kdu_flush_stats() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native boolean Equals(Kdu_flush_stats _rhs) throws KduException;
  public native int Advance(int _frame_sep) throws KduException;
  public native void Auto_advance(int _frame_sep) throws KduException;
}
