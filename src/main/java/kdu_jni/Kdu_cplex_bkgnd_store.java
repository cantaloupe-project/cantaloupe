package kdu_jni;

public class Kdu_cplex_bkgnd_store extends Kdu_cplex_bkgnd {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_cplex_bkgnd_store(long ptr) {
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
  public Kdu_cplex_bkgnd_store() {
    this(Native_create());
  }
  public native boolean Load(String _import_pathname, boolean _read_only) throws KduException;
  public native boolean Save(String _export_pathname) throws KduException;
}
