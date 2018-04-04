package kdu_jni;

public class Kdu_run_queue extends Kdu_thread_queue {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_run_queue(long ptr) {
    super(ptr);
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create();
  public Kdu_run_queue() {
    this(Native_create());
  }
  public native void Activate() throws KduException;
  public native void Deactivate(Kdu_thread_entity _caller) throws KduException;
}
