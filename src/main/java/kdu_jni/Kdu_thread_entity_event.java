package kdu_jni;

public class Kdu_thread_entity_event {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_thread_entity_event(long ptr) {
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
  public Kdu_thread_entity_event() {
    this(Native_create());
    this.Native_init();
  }
  public native boolean Signal() throws KduException;
  public void Handle_signal(Kdu_thread_entity _caller) throws KduException
  {
    // Override in a derived class to respond to the callback
    return;
  }
  public native int Deregister(Kdu_thread_entity _caller, boolean _in_destructor, boolean _inside_thread_group_lock) throws KduException;
  public int Deregister(Kdu_thread_entity _caller) throws KduException
  {
    return Deregister(_caller,(boolean) false,(boolean) false);
  }
  public int Deregister(Kdu_thread_entity _caller, boolean _in_destructor) throws KduException
  {
    return Deregister(_caller,_in_destructor,(boolean) false);
  }
}
