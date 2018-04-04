package kdu_jni;

public class Kdu_region_compositor {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_region_compositor(long ptr) {
    _native_ptr = ptr;
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create(Kdu_thread_env _env, Kdu_thread_queue _env_queue);
  private native void Native_init();
  public Kdu_region_compositor(Kdu_thread_env _env, Kdu_thread_queue _env_queue) {
    this(Native_create(_env, _env_queue));
    this.Native_init();
  }
  private static long Native_create()
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Native_create(env,env_queue);
  }
  public Kdu_region_compositor() {
    this(Native_create());
    this.Native_init();
  }
  private static long Native_create(Kdu_thread_env _env)
  {
    Kdu_thread_queue env_queue = null;
    return Native_create(_env,env_queue);
  }
  public Kdu_region_compositor(Kdu_thread_env _env) {
    this(Native_create(_env));
    this.Native_init();
  }
  private static native long Native_create(Kdu_compressed_source _source, int _persistent_cache_threshold);
  public Kdu_region_compositor(Kdu_compressed_source _source, int _persistent_cache_threshold) {
    this(Native_create(_source, _persistent_cache_threshold));
    this.Native_init();
  }
  private static long Native_create(Kdu_compressed_source _source)
  {
    return Native_create(_source,(int) 256000);
  }
  public Kdu_region_compositor(Kdu_compressed_source _source) {
    this(Native_create(_source));
    this.Native_init();
  }
  private static native long Native_create(Jpx_source _source, int _persistent_cache_threshold);
  public Kdu_region_compositor(Jpx_source _source, int _persistent_cache_threshold) {
    this(Native_create(_source, _persistent_cache_threshold));
    this.Native_init();
  }
  private static long Native_create(Jpx_source _source)
  {
    return Native_create(_source,(int) 256000);
  }
  public Kdu_region_compositor(Jpx_source _source) {
    this(Native_create(_source));
    this.Native_init();
  }
  private static native long Native_create(Mj2_source _source, int _persistent_cache_threshold);
  public Kdu_region_compositor(Mj2_source _source, int _persistent_cache_threshold) {
    this(Native_create(_source, _persistent_cache_threshold));
    this.Native_init();
  }
  private static long Native_create(Mj2_source _source)
  {
    return Native_create(_source,(int) 256000);
  }
  public Kdu_region_compositor(Mj2_source _source) {
    this(Native_create(_source));
    this.Native_init();
  }
  public native void Pre_destroy() throws KduException;
  public native void Create(Kdu_compressed_source _source, int _persistent_cache_threshold) throws KduException;
  public void Create(Kdu_compressed_source _source) throws KduException
  {
    Create(_source,(int) 256000);
  }
  public native void Create(Jpx_source _source, int _persistent_cache_threshold) throws KduException;
  public void Create(Jpx_source _source) throws KduException
  {
    Create(_source,(int) 256000);
  }
  public native void Create(Mj2_source _source, int _persistent_cache_threshold) throws KduException;
  public void Create(Mj2_source _source) throws KduException
  {
    Create(_source,(int) 256000);
  }
  public native boolean Mem_configure(Kdu_membroker _membroker, int _frag_bits) throws KduException;
  public native void Set_colour_order(int _colour_order) throws KduException;
  public native void Set_error_level(int _error_level) throws KduException;
  public native void Configure_scaling_params(float _min_resampling_factor, float _max_interp_overshoot, int _bilinear_expansion_threshold) throws KduException;
  public void Configure_scaling_params() throws KduException
  {
    Configure_scaling_params((float) 0.6f,(float) 0.4f,(int) 2);
  }
  public void Configure_scaling_params(float _min_resampling_factor) throws KduException
  {
    Configure_scaling_params(_min_resampling_factor,(float) 0.4f,(int) 2);
  }
  public void Configure_scaling_params(float _min_resampling_factor, float _max_interp_overshoot) throws KduException
  {
    Configure_scaling_params(_min_resampling_factor,_max_interp_overshoot,(int) 2);
  }
  public native void Configure_intensity_scaling(boolean _true_zero, boolean _true_max) throws KduException;
  public native void Set_quality_limiting(Kdu_quality_limiter _limiter, float _hor_ppi, float _vert_ppi) throws KduException;
  public native void Set_process_aggregation_threshold(float _threshold) throws KduException;
  public native void Set_surface_initialization_mode(boolean _pre_initialize) throws KduException;
  public native Kdu_ilayer_ref Add_ilayer(int _layer_src, Kdu_dims _full_source_dims, Kdu_dims _full_target_dims, boolean _transpose, boolean _vflip, boolean _hflip, int _frame_idx, int _field_handling) throws KduException;
  public Kdu_ilayer_ref Add_ilayer(int _layer_src, Kdu_dims _full_source_dims, Kdu_dims _full_target_dims) throws KduException
  {
    return Add_ilayer(_layer_src,_full_source_dims,_full_target_dims,(boolean) false,(boolean) false,(boolean) false,(int) 0,(int) 2);
  }
  public Kdu_ilayer_ref Add_ilayer(int _layer_src, Kdu_dims _full_source_dims, Kdu_dims _full_target_dims, boolean _transpose) throws KduException
  {
    return Add_ilayer(_layer_src,_full_source_dims,_full_target_dims,_transpose,(boolean) false,(boolean) false,(int) 0,(int) 2);
  }
  public Kdu_ilayer_ref Add_ilayer(int _layer_src, Kdu_dims _full_source_dims, Kdu_dims _full_target_dims, boolean _transpose, boolean _vflip) throws KduException
  {
    return Add_ilayer(_layer_src,_full_source_dims,_full_target_dims,_transpose,_vflip,(boolean) false,(int) 0,(int) 2);
  }
  public Kdu_ilayer_ref Add_ilayer(int _layer_src, Kdu_dims _full_source_dims, Kdu_dims _full_target_dims, boolean _transpose, boolean _vflip, boolean _hflip) throws KduException
  {
    return Add_ilayer(_layer_src,_full_source_dims,_full_target_dims,_transpose,_vflip,_hflip,(int) 0,(int) 2);
  }
  public Kdu_ilayer_ref Add_ilayer(int _layer_src, Kdu_dims _full_source_dims, Kdu_dims _full_target_dims, boolean _transpose, boolean _vflip, boolean _hflip, int _frame_idx) throws KduException
  {
    return Add_ilayer(_layer_src,_full_source_dims,_full_target_dims,_transpose,_vflip,_hflip,_frame_idx,(int) 2);
  }
  public native boolean Change_ilayer_frame(Kdu_ilayer_ref _ilayer_ref, int _frame_idx) throws KduException;
  public native Kdu_ilayer_ref Add_primitive_ilayer(int _stream_src, int[] _single_component_idx, int _single_access_mode, Kdu_dims _full_source_dims, Kdu_dims _full_target_dims, boolean _transpose, boolean _vflip, boolean _hflip) throws KduException;
  public Kdu_ilayer_ref Add_primitive_ilayer(int _stream_src, int[] _single_component_idx, int _single_access_mode, Kdu_dims _full_source_dims, Kdu_dims _full_target_dims) throws KduException
  {
    return Add_primitive_ilayer(_stream_src,_single_component_idx,_single_access_mode,_full_source_dims,_full_target_dims,(boolean) false,(boolean) false,(boolean) false);
  }
  public Kdu_ilayer_ref Add_primitive_ilayer(int _stream_src, int[] _single_component_idx, int _single_access_mode, Kdu_dims _full_source_dims, Kdu_dims _full_target_dims, boolean _transpose) throws KduException
  {
    return Add_primitive_ilayer(_stream_src,_single_component_idx,_single_access_mode,_full_source_dims,_full_target_dims,_transpose,(boolean) false,(boolean) false);
  }
  public Kdu_ilayer_ref Add_primitive_ilayer(int _stream_src, int[] _single_component_idx, int _single_access_mode, Kdu_dims _full_source_dims, Kdu_dims _full_target_dims, boolean _transpose, boolean _vflip) throws KduException
  {
    return Add_primitive_ilayer(_stream_src,_single_component_idx,_single_access_mode,_full_source_dims,_full_target_dims,_transpose,_vflip,(boolean) false);
  }
  public native boolean Remove_ilayer(Kdu_ilayer_ref _ilayer_ref, boolean _permanent) throws KduException;
  public native void Cull_inactive_ilayers(int _max_inactive) throws KduException;
  public native void Set_frame(Jpx_frame_expander _expander, Kdu_coords _offset) throws KduException;
  public native boolean Add_frame(Jpx_frame_expander _expander, Kdu_coords _offset) throws KduException;
  public native boolean Waiting_for_stream_headers() throws KduException;
  public native void Set_scale(boolean _transpose, boolean _vflip, boolean _hflip, float _scale, float _rendering_scale_adjustment) throws KduException;
  public void Set_scale(boolean _transpose, boolean _vflip, boolean _hflip, float _scale) throws KduException
  {
    Set_scale(_transpose,_vflip,_hflip,_scale,(float) 1.0f);
  }
  public native float Find_optimal_scale(Kdu_dims _region, float _scale_anchor, float _min_scale, float _max_scale, Kdu_istream_ref _istream_ref, int[] _component_idx, boolean _avoid_subsampling) throws KduException;
  public float Find_optimal_scale(Kdu_dims _region, float _scale_anchor, float _min_scale, float _max_scale, Kdu_istream_ref _istream_ref) throws KduException
  {
    return Find_optimal_scale(_region,_scale_anchor,_min_scale,_max_scale,_istream_ref,null,(boolean) false);
  }
  public float Find_optimal_scale(Kdu_dims _region, float _scale_anchor, float _min_scale, float _max_scale, Kdu_istream_ref _istream_ref, int[] _component_idx) throws KduException
  {
    return Find_optimal_scale(_region,_scale_anchor,_min_scale,_max_scale,_istream_ref,_component_idx,(boolean) false);
  }
  public native void Set_buffer_surface(Kdu_dims _region, int _background) throws KduException;
  public void Set_buffer_surface(Kdu_dims _region) throws KduException
  {
    Set_buffer_surface(_region,(int) -1);
  }
  public native int Check_invalid_scale_code() throws KduException;
  public native boolean Get_total_composition_dims(Kdu_dims _dims) throws KduException;
  public native Kdu_compositor_buf Get_composition_buffer(Kdu_dims _region, boolean _working_only) throws KduException;
  public Kdu_compositor_buf Get_composition_buffer(Kdu_dims _region) throws KduException
  {
    return Get_composition_buffer(_region,(boolean) false);
  }
  public native boolean Push_composition_buffer(long _custom_stamp, int _custom_id_val) throws KduException;
  public native boolean Replace_composition_queue_tail(long _custom_stamp, int _custom_id_val) throws KduException;
  public native boolean Pop_composition_buffer() throws KduException;
  public native Kdu_compositor_buf Inspect_composition_queue(int _elt, long[] _custom_stamp, int[] _custom_id_val, Kdu_dims _region) throws KduException;
  public native void Flush_composition_queue() throws KduException;
  public native void Set_max_quality_layers(int _quality_layers) throws KduException;
  public native int Get_max_available_quality_layers() throws KduException;
  public native Kdu_thread_env Set_thread_env(Kdu_thread_env _env, Kdu_thread_queue _env_queue) throws KduException;
  public native boolean Process(int _suggested_increment, Kdu_dims _new_rendered_region, int _flags) throws KduException;
  public boolean Process(int _suggested_increment, Kdu_dims _new_rendered_region) throws KduException
  {
    return Process(_suggested_increment,_new_rendered_region,(int) 0);
  }
  public native boolean Is_processing_complete() throws KduException;
  public native boolean Is_codestream_processing_complete() throws KduException;
  public native boolean Refresh(boolean[] _new_imagery) throws KduException;
  public boolean Refresh() throws KduException
  {
    return Refresh(null);
  }
  public native void Invalidate_rect(Kdu_dims _invalidated_region) throws KduException;
  public native void Halt_processing() throws KduException;
  public native int Get_num_ilayers() throws KduException;
  public native Kdu_ilayer_ref Get_next_ilayer(Kdu_ilayer_ref _last_ilayer_ref, int _layer_src, int _direct_codestream_idx) throws KduException;
  public Kdu_ilayer_ref Get_next_ilayer(Kdu_ilayer_ref _last_ilayer_ref) throws KduException
  {
    return Get_next_ilayer(_last_ilayer_ref,(int) -1,(int) -1);
  }
  public Kdu_ilayer_ref Get_next_ilayer(Kdu_ilayer_ref _last_ilayer_ref, int _layer_src) throws KduException
  {
    return Get_next_ilayer(_last_ilayer_ref,_layer_src,(int) -1);
  }
  public native Kdu_istream_ref Get_next_istream(Kdu_istream_ref _last_istream_ref, boolean _only_active_istreams, boolean _no_duplicates, int _codestream_idx) throws KduException;
  public Kdu_istream_ref Get_next_istream(Kdu_istream_ref _last_istream_ref) throws KduException
  {
    return Get_next_istream(_last_istream_ref,(boolean) true,(boolean) false,(int) -1);
  }
  public Kdu_istream_ref Get_next_istream(Kdu_istream_ref _last_istream_ref, boolean _only_active_istreams) throws KduException
  {
    return Get_next_istream(_last_istream_ref,_only_active_istreams,(boolean) false,(int) -1);
  }
  public Kdu_istream_ref Get_next_istream(Kdu_istream_ref _last_istream_ref, boolean _only_active_istreams, boolean _no_duplicates) throws KduException
  {
    return Get_next_istream(_last_istream_ref,_only_active_istreams,_no_duplicates,(int) -1);
  }
  public native Kdu_ilayer_ref Get_next_visible_ilayer(Kdu_ilayer_ref _last_ilayer_ref, Kdu_dims _region) throws KduException;
  public native Kdu_codestream Access_codestream(Kdu_istream_ref _istream_ref) throws KduException;
  public native int Get_istream_info(Kdu_istream_ref _istream_ref, int[] _codestream_idx, Kdu_ilayer_ref _ilayer_ref, int[] _components_in_use, int _max_components_in_use, int[] _principle_component_idx, float[] _principle_component_scale_x, float[] _principle_component_scale_y, boolean[] _transpose, boolean[] _vflip, boolean[] _hflip) throws KduException;
  public int Get_istream_info(Kdu_istream_ref _istream_ref, int[] _codestream_idx, Kdu_ilayer_ref _ilayer_ref) throws KduException
  {
    return Get_istream_info(_istream_ref,_codestream_idx,_ilayer_ref,null,(int) 4,null,null,null,null,null,null);
  }
  public int Get_istream_info(Kdu_istream_ref _istream_ref, int[] _codestream_idx, Kdu_ilayer_ref _ilayer_ref, int[] _components_in_use) throws KduException
  {
    return Get_istream_info(_istream_ref,_codestream_idx,_ilayer_ref,_components_in_use,(int) 4,null,null,null,null,null,null);
  }
  public int Get_istream_info(Kdu_istream_ref _istream_ref, int[] _codestream_idx, Kdu_ilayer_ref _ilayer_ref, int[] _components_in_use, int _max_components_in_use) throws KduException
  {
    return Get_istream_info(_istream_ref,_codestream_idx,_ilayer_ref,_components_in_use,_max_components_in_use,null,null,null,null,null,null);
  }
  public int Get_istream_info(Kdu_istream_ref _istream_ref, int[] _codestream_idx, Kdu_ilayer_ref _ilayer_ref, int[] _components_in_use, int _max_components_in_use, int[] _principle_component_idx) throws KduException
  {
    return Get_istream_info(_istream_ref,_codestream_idx,_ilayer_ref,_components_in_use,_max_components_in_use,_principle_component_idx,null,null,null,null,null);
  }
  public int Get_istream_info(Kdu_istream_ref _istream_ref, int[] _codestream_idx, Kdu_ilayer_ref _ilayer_ref, int[] _components_in_use, int _max_components_in_use, int[] _principle_component_idx, float[] _principle_component_scale_x) throws KduException
  {
    return Get_istream_info(_istream_ref,_codestream_idx,_ilayer_ref,_components_in_use,_max_components_in_use,_principle_component_idx,_principle_component_scale_x,null,null,null,null);
  }
  public int Get_istream_info(Kdu_istream_ref _istream_ref, int[] _codestream_idx, Kdu_ilayer_ref _ilayer_ref, int[] _components_in_use, int _max_components_in_use, int[] _principle_component_idx, float[] _principle_component_scale_x, float[] _principle_component_scale_y) throws KduException
  {
    return Get_istream_info(_istream_ref,_codestream_idx,_ilayer_ref,_components_in_use,_max_components_in_use,_principle_component_idx,_principle_component_scale_x,_principle_component_scale_y,null,null,null);
  }
  public int Get_istream_info(Kdu_istream_ref _istream_ref, int[] _codestream_idx, Kdu_ilayer_ref _ilayer_ref, int[] _components_in_use, int _max_components_in_use, int[] _principle_component_idx, float[] _principle_component_scale_x, float[] _principle_component_scale_y, boolean[] _transpose) throws KduException
  {
    return Get_istream_info(_istream_ref,_codestream_idx,_ilayer_ref,_components_in_use,_max_components_in_use,_principle_component_idx,_principle_component_scale_x,_principle_component_scale_y,_transpose,null,null);
  }
  public int Get_istream_info(Kdu_istream_ref _istream_ref, int[] _codestream_idx, Kdu_ilayer_ref _ilayer_ref, int[] _components_in_use, int _max_components_in_use, int[] _principle_component_idx, float[] _principle_component_scale_x, float[] _principle_component_scale_y, boolean[] _transpose, boolean[] _vflip) throws KduException
  {
    return Get_istream_info(_istream_ref,_codestream_idx,_ilayer_ref,_components_in_use,_max_components_in_use,_principle_component_idx,_principle_component_scale_x,_principle_component_scale_y,_transpose,_vflip,null);
  }
  public native int Get_ilayer_info(Kdu_ilayer_ref _ilayer_ref, int[] _layer_src, int[] _direct_codestream_idx, boolean[] _is_opaque, int[] _frame_idx, int[] _field_handling) throws KduException;
  public int Get_ilayer_info(Kdu_ilayer_ref _ilayer_ref, int[] _layer_src, int[] _direct_codestream_idx, boolean[] _is_opaque) throws KduException
  {
    return Get_ilayer_info(_ilayer_ref,_layer_src,_direct_codestream_idx,_is_opaque,null,null);
  }
  public int Get_ilayer_info(Kdu_ilayer_ref _ilayer_ref, int[] _layer_src, int[] _direct_codestream_idx, boolean[] _is_opaque, int[] _frame_idx) throws KduException
  {
    return Get_ilayer_info(_ilayer_ref,_layer_src,_direct_codestream_idx,_is_opaque,_frame_idx,null);
  }
  public native Kdu_istream_ref Get_ilayer_stream(Kdu_ilayer_ref _ilayer_ref, int _which, int _codestream_idx) throws KduException;
  public Kdu_istream_ref Get_ilayer_stream(Kdu_ilayer_ref _ilayer_ref, int _which) throws KduException
  {
    return Get_ilayer_stream(_ilayer_ref,_which,(int) -1);
  }
  public native boolean Get_codestream_packets(Kdu_istream_ref _istream_ref, Kdu_dims _region, long[] _visible_precinct_samples, long[] _visible_packet_samples, long[] _max_visible_packet_samples, int _max_region_layers) throws KduException;
  public boolean Get_codestream_packets(Kdu_istream_ref _istream_ref, Kdu_dims _region, long[] _visible_precinct_samples, long[] _visible_packet_samples, long[] _max_visible_packet_samples) throws KduException
  {
    return Get_codestream_packets(_istream_ref,_region,_visible_precinct_samples,_visible_packet_samples,_max_visible_packet_samples,(int) 0);
  }
  public native Kdu_ilayer_ref Find_point(Kdu_coords _point, int _enumerator, float _visibility_threshold) throws KduException;
  public Kdu_ilayer_ref Find_point(Kdu_coords _point) throws KduException
  {
    return Find_point(_point,(int) 0,(float) -1.0F);
  }
  public Kdu_ilayer_ref Find_point(Kdu_coords _point, int _enumerator) throws KduException
  {
    return Find_point(_point,_enumerator,(float) -1.0F);
  }
  public native Kdu_istream_ref Map_region(Kdu_dims _region, Kdu_istream_ref _istream_ref) throws KduException;
  public native Kdu_dims Inverse_map_region(Kdu_dims _region, Kdu_istream_ref _istream_ref) throws KduException;
  public native Kdu_dims Find_ilayer_region(Kdu_ilayer_ref _ilayer_ref, boolean _apply_cropping) throws KduException;
  public native Kdu_dims Find_istream_region(Kdu_istream_ref _istream_ref, boolean _apply_cropping) throws KduException;
  public native boolean Find_compatible_jpip_window(Kdu_coords _fsiz, Kdu_dims _roi_dims, int[] _round_direction, Kdu_dims _region) throws KduException;
  public native boolean Load_metadata_matches() throws KduException;
  public native int Generate_metareq(Kdu_window _client_window, int _anchor_flags, Kdu_dims _region, int _num_box_types, long[] _box_types, int _num_descend_box_types, long[] _descend_box_types, boolean _priority, int _max_descend_depth) throws KduException;
  public native void Configure_overlays(boolean _enable, int _min_display_size, float _blending_factor, int _max_painting_border, Jpx_metanode _dependency, int _dependency_effect, long[] _aux_params, int _num_aux_params) throws KduException;
  public void Configure_overlays(boolean _enable, int _min_display_size, float _blending_factor, int _max_painting_border, Jpx_metanode _dependency) throws KduException
  {
    Configure_overlays(_enable,_min_display_size,_blending_factor,_max_painting_border,_dependency,(int) 0,null,(int) 0);
  }
  public void Configure_overlays(boolean _enable, int _min_display_size, float _blending_factor, int _max_painting_border, Jpx_metanode _dependency, int _dependency_effect) throws KduException
  {
    Configure_overlays(_enable,_min_display_size,_blending_factor,_max_painting_border,_dependency,_dependency_effect,null,(int) 0);
  }
  public void Configure_overlays(boolean _enable, int _min_display_size, float _blending_factor, int _max_painting_border, Jpx_metanode _dependency, int _dependency_effect, long[] _aux_params) throws KduException
  {
    Configure_overlays(_enable,_min_display_size,_blending_factor,_max_painting_border,_dependency,_dependency_effect,_aux_params,(int) 0);
  }
  public native void Update_overlays(boolean _start_from_scratch) throws KduException;
  public native Jpx_metanode Search_overlays(Kdu_coords _point, Kdu_istream_ref _istream_ref, float _visibility_threshold) throws KduException;
  public native boolean Get_overlay_info(int[] _total_roi_nodes, int[] _hidden_roi_nodes) throws KduException;
  public boolean Custom_paint_overlay(Kdu_compositor_buf _buffer, Kdu_dims _buffer_region, Kdu_dims _bounding_region, Jpx_metanode _node, Kdu_overlay_params _painting_params, Kdu_coords _image_offset, Kdu_coords _subsampling, boolean _transpose, boolean _vflip, boolean _hflip, Kdu_coords _expansion_numerator, Kdu_coords _expansion_denominator, Kdu_coords _compositing_offset, int _colour_order) throws KduException
  {
    // Override in a derived class to respond to the callback
    return false;
  }
  public Kdu_compositor_buf Allocate_buffer(Kdu_coords _min_size, Kdu_coords _actual_size, boolean _read_access_required) throws KduException
  {
    // Override in a derived class to respond to the callback
    return null;
  }
  public void Delete_buffer(Kdu_compositor_buf _buf) throws KduException
  {
    // Override in a derived class to respond to the callback
    return;
  }
}
