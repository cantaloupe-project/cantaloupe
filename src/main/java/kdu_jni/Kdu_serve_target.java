package kdu_jni;

public class Kdu_serve_target {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_serve_target(long ptr) {
    _native_ptr = ptr;
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  public native long Get_codestream_ranges(int[] _num_ranges, int _compositing_layer_idx) throws KduException;
  public long Get_codestream_ranges(int[] _num_ranges) throws KduException
  {
    return Get_codestream_ranges(_num_ranges,(int) -1);
  }
  public native boolean Get_codestream_siz_info(int _codestream_id, Kdu_dims _image_dims, Kdu_dims _tile_partition, Kdu_dims _tile_indices, int[] _num_components, int[] _num_output_components, int[] _max_discard_levels, int[] _max_quality_layers, Kdu_coords _component_subs, Kdu_coords _output_component_subs) throws KduException;
  public native boolean Get_codestream_rd_info(int _codestream_id, int[] _num_layer_slopes, int[] _num_layer_lengths, int[] _layer_log_slopes, long[] _layer_lengths) throws KduException;
  public boolean Get_codestream_rd_info(int _codestream_id, int[] _num_layer_slopes, int[] _num_layer_lengths) throws KduException
  {
    return Get_codestream_rd_info(_codestream_id,_num_layer_slopes,_num_layer_lengths,null,null);
  }
  public boolean Get_codestream_rd_info(int _codestream_id, int[] _num_layer_slopes, int[] _num_layer_lengths, int[] _layer_log_slopes) throws KduException
  {
    return Get_codestream_rd_info(_codestream_id,_num_layer_slopes,_num_layer_lengths,_layer_log_slopes,null);
  }
  public native Kdu_codestream Attach_to_codestream(int _codestream_id, long _thread_handle) throws KduException;
  public native void Detach_from_codestream(int _codestream_id, long _thread_handle) throws KduException;
  public native void Lock_codestreams(int _num_codestreams, int[] _codestream_indices, long _thread_handle) throws KduException;
  public native void Release_codestreams(int _num_codestreams, int[] _codestream_indices, long _thread_handle) throws KduException;
  public native Kdu_window_context Access_context(int _context_type, int _context_idx, int[] _remapping_ids) throws KduException;
  public native int Find_roi(int _stream_id, String _roi_name) throws KduException;
  public int Find_roi(int _stream_id) throws KduException
  {
    return Find_roi(_stream_id,null);
  }
  public native String Get_roi_details(int _index, Kdu_coords _resolution, Kdu_dims _region) throws KduException;
}
