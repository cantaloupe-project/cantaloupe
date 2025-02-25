package kdu_jni;

public class Kdu_input_codestream_monitor {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_input_codestream_monitor(long ptr) {
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
  public Kdu_input_codestream_monitor() {
    this(Native_create());
    this.Native_init();
  }
  public void Installing_monitor() throws KduException
  {
    // Override in a derived class to respond to the callback
    return;
  }
  public void Restarting() throws KduException
  {
    // Override in a derived class to respond to the callback
    return;
  }
  public void Uninstalling_monitor() throws KduException
  {
    // Override in a derived class to respond to the callback
    return;
  }
  public void Parsing_complete() throws KduException
  {
    // Override in a derived class to respond to the callback
    return;
  }
}
