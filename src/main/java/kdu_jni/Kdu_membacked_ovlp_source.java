package kdu_jni;

public class Kdu_membacked_ovlp_source extends Kdu_membacked_compressed_source {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_membacked_ovlp_source(long ptr) {
    super(ptr);
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  public native int Ovlp_open(boolean _blocking, long _queue_sequence_idx, Kdu_thread_env _env) throws KduException;
  public native boolean Ovlp_close(Kdu_thread_env _env) throws KduException;
  public native void Ovlp_shutdown(Kdu_thread_env _env) throws KduException;
  public native void Ovlp_service_all_finished() throws KduException;
  public boolean Post_open(long _queue_sequence_idx, Kdu_thread_queue _queue, Kdu_thread_env _env) throws KduException
  {
    // Override in a derived class to respond to the callback
    return false;
  }
}
