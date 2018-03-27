package kdu_jni;

public class Kdu_window {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_window(long ptr) {
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
  public Kdu_window() {
    this(Native_create());
  }
  private static native long Native_create(Kdu_window _src);
  public Kdu_window(Kdu_window _src) {
    this(Native_create(_src));
  }
  public native void Init() throws KduException;
  public native boolean Is_empty() throws KduException;
  public native void Copy_from(Kdu_window _src, boolean _copy_expansions) throws KduException;
  public void Copy_from(Kdu_window _src) throws KduException
  {
    Copy_from(_src,(boolean) false);
  }
  public native void Copy_metareq_from(Kdu_window _src) throws KduException;
  public native boolean Metareq_contains(Kdu_window _rhs) throws KduException;
  public native boolean Imagery_contains(Kdu_window _rhs) throws KduException;
  public native boolean Contains(Kdu_window _rhs) throws KduException;
  public native boolean Imagery_equals(Kdu_window _rhs) throws KduException;
  public native boolean Equals(Kdu_window _rhs) throws KduException;
  public native Kdu_coords Get_resolution() throws KduException;
  public native void Set_resolution(Kdu_coords _resolution) throws KduException;
  public native int Get_round_direction() throws KduException;
  public native void Set_round_direction(int _direction) throws KduException;
  public native Kdu_dims Get_region() throws KduException;
  public native void Set_region(Kdu_dims _region) throws KduException;
  public native Kdu_range_set Access_components() throws KduException;
  public native Kdu_range_set Access_codestreams() throws KduException;
  public native Kdu_range_set Access_contexts() throws KduException;
  public native Kdu_range_set Create_context_expansion(int _which) throws KduException;
  public native Kdu_range_set Access_context_expansion(int _which) throws KduException;
  public native String Parse_context(String _string) throws KduException;
  public native int Get_max_layers() throws KduException;
  public native void Set_max_layers(int _val) throws KduException;
  public native boolean Get_metadata_only() throws KduException;
  public native void Set_metadata_only(boolean _val) throws KduException;
  public native Kdu_metareq Get_metareq(int _index) throws KduException;
  public native void Init_metareq() throws KduException;
  public native void Add_metareq(long _box_type, int _qualifier, boolean _priority, int _byte_limit, boolean _recurse, long _root_bin_id, int _max_depth) throws KduException;
  public native String Parse_metareq(String _string) throws KduException;
}
