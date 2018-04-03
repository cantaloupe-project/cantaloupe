package kdu_jni;

public class Kdu_thread_entity_affinity {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_thread_entity_affinity(long ptr) {
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
  public Kdu_thread_entity_affinity() {
    this(Native_create());
  }
  public native void Reset() throws KduException;
  public native int Get_total_threads() throws KduException;
  public native int Get_num_bundles() throws KduException;
}
