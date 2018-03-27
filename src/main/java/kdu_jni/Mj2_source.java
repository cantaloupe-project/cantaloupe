package kdu_jni;

public class Mj2_source {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Mj2_source(long ptr) {
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
  public Mj2_source() {
    this(Native_create());
  }
  public native boolean Exists() throws KduException;
  public native int Open(Jp2_family_src _src, boolean _return_if_incompatible, Kdu_membroker _membroker) throws KduException;
  public int Open(Jp2_family_src _src) throws KduException
  {
    Kdu_membroker membroker = null;
    return Open(_src,(boolean) false,membroker);
  }
  public int Open(Jp2_family_src _src, boolean _return_if_incompatible) throws KduException
  {
    Kdu_membroker membroker = null;
    return Open(_src,_return_if_incompatible,membroker);
  }
  public native void Close() throws KduException;
  public native Jp2_family_src Get_ultimate_src() throws KduException;
  public native Kdu_dims Get_movie_dims() throws KduException;
  public native long Get_next_track(long _prev_track_idx) throws KduException;
  public native int Get_track_type(long _track_idx) throws KduException;
  public native Mj2_video_source Access_video_track(long _track_idx) throws KduException;
  public native boolean Find_stream(int _stream_idx, long[] _track_idx, int[] _frame_idx, int[] _field_idx) throws KduException;
  public native boolean Count_codestreams(int[] _count) throws KduException;
}
