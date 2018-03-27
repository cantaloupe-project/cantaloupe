package kdu_jni;

public class Kdu_client_notifier {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_client_notifier(long ptr) {
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
  public Kdu_client_notifier() {
    this(Native_create());
    this.Native_init();
  }
  public void Notify() throws KduException
  {
    // Override in a derived class to respond to the callback
    return;
  }
}
