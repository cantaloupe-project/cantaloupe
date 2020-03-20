package kdu_jni;

public class Kdu_platform_file_target extends Kdu_compressed_target {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_platform_file_target(long ptr) {
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
  public Kdu_platform_file_target() {
    this(Native_create());
  }
  private static native long Native_create(String _fname, boolean _append_to_existing);
  public Kdu_platform_file_target(String _fname, boolean _append_to_existing) {
    this(Native_create(_fname, _append_to_existing));
  }
  private static long Native_create(String _fname)
  {
    return Native_create(_fname,(boolean) false);
  }
  public Kdu_platform_file_target(String _fname) {
    this(Native_create(_fname));
  }
  public native boolean Exists() throws KduException;
  public native boolean Open(String _fname, boolean _append_to_existing, boolean _return_on_failure, Kdu_membroker _membroker) throws KduException;
  public boolean Open(String _fname) throws KduException
  {
    Kdu_membroker membroker = null;
    return Open(_fname,(boolean) false,(boolean) false,membroker);
  }
  public boolean Open(String _fname, boolean _append_to_existing) throws KduException
  {
    Kdu_membroker membroker = null;
    return Open(_fname,_append_to_existing,(boolean) false,membroker);
  }
  public boolean Open(String _fname, boolean _append_to_existing, boolean _return_on_failure) throws KduException
  {
    Kdu_membroker membroker = null;
    return Open(_fname,_append_to_existing,_return_on_failure,membroker);
  }
  public native boolean Strip_tail(byte[] _buf, int _num_bytes) throws KduException;
}
