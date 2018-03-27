package kdu_jni;

public class Kdu_output {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_output(long ptr) {
    _native_ptr = ptr;
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  public native int Put(byte _byte) throws KduException;
  public native int Put(int _word) throws KduException;
  public native int Put(long _word) throws KduException;
  public native int Put(float _val) throws KduException;
  public native void Write(byte[] _buf, int _count) throws KduException;
  public native void Fill_bytes(int _count, byte _byte) throws KduException;
  public native void Fill_pairs(int _count, byte _b0, byte _b1) throws KduException;
}
