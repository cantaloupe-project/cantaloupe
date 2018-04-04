package kdu_jni;

public class Kdu_pull_ifc {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_pull_ifc(long ptr) {
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
  public Kdu_pull_ifc() {
    this(Native_create());
  }
  public native void Destroy() throws KduException;
  public native boolean Start(Kdu_thread_env _env) throws KduException;
  public native boolean Exists() throws KduException;
  public native void Pull(Kdu_line_buf _line, Kdu_thread_env _env) throws KduException;
  public void Pull(Kdu_line_buf _line) throws KduException
  {
    Kdu_thread_env env = null;
    Pull(_line,env);
  }
}
