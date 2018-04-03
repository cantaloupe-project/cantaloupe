package kdu_jni;

public class Kdu_message_queue extends Kdu_thread_safe_message {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_message_queue(long ptr) {
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
  public Kdu_message_queue() {
    this(Native_create());
  }
  public native void Configure(int _max_queued_messages, boolean _auto_pop, boolean _throw_exceptions, int _exception_val) throws KduException;
  public native void Put_text(String _string) throws KduException;
  public native void Start_message() throws KduException;
  public native void Flush(boolean _end_of_message) throws KduException;
  public void Flush() throws KduException
  {
    Flush((boolean) false);
  }
  public native String Pop_message() throws KduException;
}
