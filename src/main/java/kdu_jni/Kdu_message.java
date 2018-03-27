package kdu_jni;

public class Kdu_message {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_message(long ptr) {
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
  private native void Native_init();
  public Kdu_message() {
    this(Native_create());
    this.Native_init();
  }
  public void Put_text(String _string) throws KduException
  {
    // Override in a derived class to respond to the callback
    return;
  }
  public native void Put_text(int[] _string) throws KduException;
  public void Flush(boolean _end_of_message) throws KduException
  {
    // Override in a derived class to respond to the callback
    return;
  }
  public void Flush() throws KduException
  {
    Flush((boolean) false);
  }
  public void Start_message() throws KduException
  {
    // Override in a derived class to respond to the callback
    return;
  }
  public native boolean Set_hex_mode(boolean _new_mode) throws KduException;
}
