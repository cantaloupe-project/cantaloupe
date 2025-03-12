package kdu_jni;

public class Kdu_compressed_source_notifier {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_compressed_source_notifier(long ptr) {
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
  public Kdu_compressed_source_notifier() {
    this(Native_create());
  }
  public native void Note_blocking() throws KduException;
  public native void Note_unblocked() throws KduException;
  public native boolean Prepare_notify(long _notification_span) throws KduException;
  public native void Notify() throws KduException;
  public native void Will_not_notify() throws KduException;
}
