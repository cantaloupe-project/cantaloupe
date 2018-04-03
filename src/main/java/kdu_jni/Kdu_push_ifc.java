package kdu_jni;

public class Kdu_push_ifc {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_push_ifc(long ptr) {
    _native_ptr = ptr;
  }
  private native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create();
  public Kdu_push_ifc() {
    this(Native_create());
  }
  public native void Destroy() throws KduException;
  public native void Start(Kdu_thread_env _env) throws KduException;
  public native boolean Exists() throws KduException;
  public native void Push(Kdu_line_buf _line, Kdu_thread_env _env) throws KduException;
  public void Push(Kdu_line_buf _line) throws KduException
  {
    Kdu_thread_env env = null;
    Push(_line,env);
  }
}
