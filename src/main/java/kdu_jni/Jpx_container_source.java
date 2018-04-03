package kdu_jni;

public class Jpx_container_source {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jpx_container_source(long ptr) {
    _native_ptr = ptr;
  }
  public Jpx_container_source() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native int Get_container_id() throws KduException;
  public native int Get_num_top_codestreams() throws KduException;
  public native int Get_num_top_layers() throws KduException;
  public native int Get_base_codestreams(int[] _num_base_codestreams) throws KduException;
  public native int Get_base_layers(int[] _num_base_layers) throws KduException;
  public native boolean Count_repetitions(int[] _count) throws KduException;
  public native Jpx_layer_source Access_layer(int _base_idx, int _rep_idx, boolean _need_stream_headers, boolean _find_first_rep) throws KduException;
  public Jpx_layer_source Access_layer(int _base_idx, int _rep_idx) throws KduException
  {
    return Access_layer(_base_idx,_rep_idx,(boolean) true,(boolean) false);
  }
  public Jpx_layer_source Access_layer(int _base_idx, int _rep_idx, boolean _need_stream_headers) throws KduException
  {
    return Access_layer(_base_idx,_rep_idx,_need_stream_headers,(boolean) false);
  }
  public native Jpx_codestream_source Access_codestream(int _base_idx, int _rep_idx, boolean _need_main_header, boolean _find_first_rep) throws KduException;
  public Jpx_codestream_source Access_codestream(int _base_idx, int _rep_idx) throws KduException
  {
    return Access_codestream(_base_idx,_rep_idx,(boolean) true,(boolean) false);
  }
  public Jpx_codestream_source Access_codestream(int _base_idx, int _rep_idx, boolean _need_main_header) throws KduException
  {
    return Access_codestream(_base_idx,_rep_idx,_need_main_header,(boolean) false);
  }
  public native boolean Check_compatibility(int _num_codestreams, int[] _codestream_indices, int _num_compositing_layers, int[] _layer_indices, boolean _any_repetition) throws KduException;
  public boolean Check_compatibility(int _num_codestreams, int[] _codestream_indices, int _num_compositing_layers, int[] _layer_indices) throws KduException
  {
    return Check_compatibility(_num_codestreams,_codestream_indices,_num_compositing_layers,_layer_indices,(boolean) true);
  }
  public native long Get_num_tracks() throws KduException;
  public native int Get_track_base_layers(long _track_idx, int[] _num_track_base_layers) throws KduException;
  public native Jpx_composition Access_presentation_track(long _track_idx) throws KduException;
}
