package kdu_jni;

public class Kdu_simple_video_source extends Kdu_compressed_video_source {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_simple_video_source(long ptr) {
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
  public Kdu_simple_video_source() {
    this(Native_create());
  }
  private static native long Native_create(String _fname, long[] _flags, Kdu_membroker _broker);
  public Kdu_simple_video_source(String _fname, long[] _flags, Kdu_membroker _broker) {
    this(Native_create(_fname, _flags, _broker));
  }
  private static long Native_create(String _fname, long[] _flags)
  {
    Kdu_membroker broker = null;
    return Native_create(_fname,_flags,broker);
  }
  public Kdu_simple_video_source(String _fname, long[] _flags) {
    this(Native_create(_fname, _flags));
  }
  public native boolean Exists() throws KduException;
  public native boolean Open(String _fname, long[] _flags, boolean _return_if_incompatible, Kdu_membroker _broker) throws KduException;
  public boolean Open(String _fname, long[] _flags) throws KduException
  {
    Kdu_membroker broker = null;
    return Open(_fname,_flags,(boolean) false,broker);
  }
  public boolean Open(String _fname, long[] _flags, boolean _return_if_incompatible) throws KduException
  {
    Kdu_membroker broker = null;
    return Open(_fname,_flags,_return_if_incompatible,broker);
  }
  public native long Get_remaining_bytes() throws KduException;
  public native long Get_image_file_pos() throws KduException;
}
