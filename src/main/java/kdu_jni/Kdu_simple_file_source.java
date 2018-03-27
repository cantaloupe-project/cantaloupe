package kdu_jni;

public class Kdu_simple_file_source extends Kdu_compressed_source {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_simple_file_source(long ptr) {
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
  public Kdu_simple_file_source() {
    this(Native_create());
  }
  private static native long Native_create(String _fname, boolean _allow_seeks);
  public Kdu_simple_file_source(String _fname, boolean _allow_seeks) {
    this(Native_create(_fname, _allow_seeks));
  }
  private static long Native_create(String _fname)
  {
    return Native_create(_fname,(boolean) true);
  }
  public Kdu_simple_file_source(String _fname) {
    this(Native_create(_fname));
  }
  public native boolean Exists() throws KduException;
  public native boolean Open(String _fname, boolean _allow_seeks, boolean _return_on_failure, Kdu_membroker _membroker) throws KduException;
  public boolean Open(String _fname) throws KduException
  {
    Kdu_membroker membroker = null;
    return Open(_fname,(boolean) true,(boolean) false,membroker);
  }
  public boolean Open(String _fname, boolean _allow_seeks) throws KduException
  {
    Kdu_membroker membroker = null;
    return Open(_fname,_allow_seeks,(boolean) false,membroker);
  }
  public boolean Open(String _fname, boolean _allow_seeks, boolean _return_on_failure) throws KduException
  {
    Kdu_membroker membroker = null;
    return Open(_fname,_allow_seeks,_return_on_failure,membroker);
  }
}
