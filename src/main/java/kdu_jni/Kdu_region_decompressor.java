package kdu_jni;

public class Kdu_region_decompressor {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_region_decompressor(long ptr) {
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
  public Kdu_region_decompressor() {
    this(Native_create());
  }
  public native void Get_safe_expansion_factors(Kdu_codestream _codestream, Kdu_channel_mapping _mapping, int _single_component, int _discard_levels, double[] _min_prod, double[] _max_x, double[] _max_y, int _access_mode) throws KduException;
  public native Kdu_dims Find_render_dims(Kdu_dims _codestream_region, Kdu_coords _ref_comp_subs, Kdu_coords _ref_comp_expand_numerator, Kdu_coords _ref_comp_expand_denominator) throws KduException;
  public native Kdu_coords Find_codestream_point(Kdu_coords _render_point, Kdu_coords _ref_comp_subs, Kdu_coords _ref_comp_expand_numerator, Kdu_coords _ref_comp_expand_denominator, boolean _allow_fractional_mapping) throws KduException;
  public Kdu_coords Find_codestream_point(Kdu_coords _render_point, Kdu_coords _ref_comp_subs, Kdu_coords _ref_comp_expand_numerator, Kdu_coords _ref_comp_expand_denominator) throws KduException
  {
    return Find_codestream_point(_render_point,_ref_comp_subs,_ref_comp_expand_numerator,_ref_comp_expand_denominator,(boolean) false);
  }
  public native Kdu_coords Find_render_point(Kdu_coords _codestream_point, Kdu_coords _ref_comp_subs, Kdu_coords _ref_comp_expand_numerator, Kdu_coords _ref_comp_expand_denominator, boolean _allow_fractional_mapping) throws KduException;
  public Kdu_coords Find_render_point(Kdu_coords _codestream_point, Kdu_coords _ref_comp_subs, Kdu_coords _ref_comp_expand_numerator, Kdu_coords _ref_comp_expand_denominator) throws KduException
  {
    return Find_render_point(_codestream_point,_ref_comp_subs,_ref_comp_expand_numerator,_ref_comp_expand_denominator,(boolean) false);
  }
  public native Kdu_dims Find_render_cover_dims(Kdu_dims _codestream_dims, Kdu_coords _ref_comp_subs, Kdu_coords _ref_comp_expand_numerator, Kdu_coords _ref_comp_expand_denominator, boolean _allow_fractional_mapping) throws KduException;
  public native Kdu_dims Find_codestream_cover_dims(Kdu_dims _render_dims, Kdu_coords _ref_comp_subs, Kdu_coords _ref_comp_expand_numerator, Kdu_coords _ref_comp_expand_denominator, boolean _allow_fractional_mapping) throws KduException;
  public Kdu_dims Find_codestream_cover_dims(Kdu_dims _render_dims, Kdu_coords _ref_comp_subs, Kdu_coords _ref_comp_expand_numerator, Kdu_coords _ref_comp_expand_denominator) throws KduException
  {
    return Find_codestream_cover_dims(_render_dims,_ref_comp_subs,_ref_comp_expand_numerator,_ref_comp_expand_denominator,(boolean) false);
  }
  public native Kdu_dims Get_rendered_image_dims(Kdu_codestream _codestream, Kdu_channel_mapping _mapping, int _single_component, int _discard_levels, Kdu_coords _expand_numerator, Kdu_coords _expand_denominator, int _access_mode) throws KduException;
  public native Kdu_dims Get_rendered_image_dims() throws KduException;
  public native void Set_true_scaling(boolean _true_zero, boolean _true_max) throws KduException;
  public native void Set_white_stretch(int _white_stretch_precision) throws KduException;
  public native void Set_interpolation_behaviour(float _max_overshoot, int _zero_overshoot_threshold) throws KduException;
  public native void Mem_configure(Kdu_membroker _membroker, int _frag_bits) throws KduException;
  public native void Set_quality_limiting(Kdu_quality_limiter _limiter, float _hor_ppi, float _vert_ppi) throws KduException;
  public native boolean Start(Kdu_codestream _codestream, Kdu_channel_mapping _mapping, int _single_component, int _discard_levels, int _max_layers, Kdu_dims _region, Kdu_coords _expand_numerator, Kdu_coords _expand_denominator, boolean _precise, int _access_mode, boolean _fastest, Kdu_thread_env _env, Kdu_thread_queue _env_queue) throws KduException;
  public boolean Start(Kdu_codestream _codestream, Kdu_channel_mapping _mapping, int _single_component, int _discard_levels, int _max_layers, Kdu_dims _region, Kdu_coords _expand_numerator, Kdu_coords _expand_denominator, boolean _precise, int _access_mode) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Start(_codestream,_mapping,_single_component,_discard_levels,_max_layers,_region,_expand_numerator,_expand_denominator,_precise,_access_mode,(boolean) false,env,env_queue);
  }
  public boolean Start(Kdu_codestream _codestream, Kdu_channel_mapping _mapping, int _single_component, int _discard_levels, int _max_layers, Kdu_dims _region, Kdu_coords _expand_numerator, Kdu_coords _expand_denominator, boolean _precise, int _access_mode, boolean _fastest) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Start(_codestream,_mapping,_single_component,_discard_levels,_max_layers,_region,_expand_numerator,_expand_denominator,_precise,_access_mode,_fastest,env,env_queue);
  }
  public boolean Start(Kdu_codestream _codestream, Kdu_channel_mapping _mapping, int _single_component, int _discard_levels, int _max_layers, Kdu_dims _region, Kdu_coords _expand_numerator, Kdu_coords _expand_denominator, boolean _precise, int _access_mode, boolean _fastest, Kdu_thread_env _env) throws KduException
  {
    Kdu_thread_queue env_queue = null;
    return Start(_codestream,_mapping,_single_component,_discard_levels,_max_layers,_region,_expand_numerator,_expand_denominator,_precise,_access_mode,_fastest,_env,env_queue);
  }
  public native boolean Process(int[] _buffer, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region) throws KduException;
  public native boolean Process(byte[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, int _precision_bits, boolean _measure_row_gap_in_pixels, int _expand_monochrome, int _fill_alpha, int _max_colour_channels) throws KduException;
  public boolean Process(byte[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,(int) 8,(boolean) true,(int) 0,(int) 0,(int) 0);
  }
  public boolean Process(byte[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, int _precision_bits) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,_precision_bits,(boolean) true,(int) 0,(int) 0,(int) 0);
  }
  public boolean Process(byte[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, int _precision_bits, boolean _measure_row_gap_in_pixels) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,_precision_bits,_measure_row_gap_in_pixels,(int) 0,(int) 0,(int) 0);
  }
  public boolean Process(byte[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, int _precision_bits, boolean _measure_row_gap_in_pixels, int _expand_monochrome) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,_precision_bits,_measure_row_gap_in_pixels,_expand_monochrome,(int) 0,(int) 0);
  }
  public boolean Process(byte[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, int _precision_bits, boolean _measure_row_gap_in_pixels, int _expand_monochrome, int _fill_alpha) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,_precision_bits,_measure_row_gap_in_pixels,_expand_monochrome,_fill_alpha,(int) 0);
  }
  public native boolean Process(int[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, int _precision_bits, boolean _measure_row_gap_in_pixels, int _expand_monochrome, int _fill_alpha, int _max_colour_channels) throws KduException;
  public boolean Process(int[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,(int) 16,(boolean) true,(int) 0,(int) 0,(int) 0);
  }
  public boolean Process(int[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, int _precision_bits) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,_precision_bits,(boolean) true,(int) 0,(int) 0,(int) 0);
  }
  public boolean Process(int[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, int _precision_bits, boolean _measure_row_gap_in_pixels) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,_precision_bits,_measure_row_gap_in_pixels,(int) 0,(int) 0,(int) 0);
  }
  public boolean Process(int[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, int _precision_bits, boolean _measure_row_gap_in_pixels, int _expand_monochrome) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,_precision_bits,_measure_row_gap_in_pixels,_expand_monochrome,(int) 0,(int) 0);
  }
  public boolean Process(int[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, int _precision_bits, boolean _measure_row_gap_in_pixels, int _expand_monochrome, int _fill_alpha) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,_precision_bits,_measure_row_gap_in_pixels,_expand_monochrome,_fill_alpha,(int) 0);
  }
  public native boolean Process(float[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, boolean _normalize, boolean _measure_row_gap_in_pixels, int _expand_monochrome, int _fill_alpha, int _max_colour_channels, boolean _always_clip_outputs) throws KduException;
  public boolean Process(float[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,(boolean) true,(boolean) true,(int) 0,(int) 0,(int) 0,(boolean) true);
  }
  public boolean Process(float[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, boolean _normalize) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,_normalize,(boolean) true,(int) 0,(int) 0,(int) 0,(boolean) true);
  }
  public boolean Process(float[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, boolean _normalize, boolean _measure_row_gap_in_pixels) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,_normalize,_measure_row_gap_in_pixels,(int) 0,(int) 0,(int) 0,(boolean) true);
  }
  public boolean Process(float[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, boolean _normalize, boolean _measure_row_gap_in_pixels, int _expand_monochrome) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,_normalize,_measure_row_gap_in_pixels,_expand_monochrome,(int) 0,(int) 0,(boolean) true);
  }
  public boolean Process(float[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, boolean _normalize, boolean _measure_row_gap_in_pixels, int _expand_monochrome, int _fill_alpha) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,_normalize,_measure_row_gap_in_pixels,_expand_monochrome,_fill_alpha,(int) 0,(boolean) true);
  }
  public boolean Process(float[] _buffer, int[] _channel_offsets, int _pixel_gap, Kdu_coords _buffer_origin, int _row_gap, int _suggested_increment, int _max_region_pixels, Kdu_dims _incomplete_region, Kdu_dims _new_region, boolean _normalize, boolean _measure_row_gap_in_pixels, int _expand_monochrome, int _fill_alpha, int _max_colour_channels) throws KduException
  {
    return Process(_buffer,_channel_offsets,_pixel_gap,_buffer_origin,_row_gap,_suggested_increment,_max_region_pixels,_incomplete_region,_new_region,_normalize,_measure_row_gap_in_pixels,_expand_monochrome,_fill_alpha,_max_colour_channels,(boolean) true);
  }
  public native boolean Finish(int[] _failure_exception, boolean _do_cs_terminate) throws KduException;
  public boolean Finish() throws KduException
  {
    return Finish(null,(boolean) true);
  }
  public boolean Finish(int[] _failure_exception) throws KduException
  {
    return Finish(_failure_exception,(boolean) true);
  }
  public native void Reset() throws KduException;
}
