package kdu_jni;

public class Kdu_stripe_compressor {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_stripe_compressor(long ptr) {
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
  public Kdu_stripe_compressor() {
    this(Native_create());
  }
  public native void Mem_configure(Kdu_membroker _membroker, int _frag_bits) throws KduException;
  public native void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, int _min_slope_threshold, boolean _no_prediction, boolean _force_precise, boolean _record_layer_info_in_comment, double _size_tolerance, int _num_components, boolean _want_fastest, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _env_dbuf_height, int _env_tile_concurrency, boolean _trim_to_rate, int _flush_flags) throws KduException;
  public void Start(Kdu_codestream _codestream) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Start(_codestream,(int) 0,null,null,(int) 0,(boolean) false,(boolean) false,(boolean) true,(double) 0.0,(int) 0,(boolean) false,env,env_queue,(int) -1,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Start(_codestream,_num_layer_specs,null,null,(int) 0,(boolean) false,(boolean) false,(boolean) true,(double) 0.0,(int) 0,(boolean) false,env,env_queue,(int) -1,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Start(_codestream,_num_layer_specs,_layer_sizes,null,(int) 0,(boolean) false,(boolean) false,(boolean) true,(double) 0.0,(int) 0,(boolean) false,env,env_queue,(int) -1,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Start(_codestream,_num_layer_specs,_layer_sizes,_layer_slopes,(int) 0,(boolean) false,(boolean) false,(boolean) true,(double) 0.0,(int) 0,(boolean) false,env,env_queue,(int) -1,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, int _min_slope_threshold) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Start(_codestream,_num_layer_specs,_layer_sizes,_layer_slopes,_min_slope_threshold,(boolean) false,(boolean) false,(boolean) true,(double) 0.0,(int) 0,(boolean) false,env,env_queue,(int) -1,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, int _min_slope_threshold, boolean _no_prediction) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Start(_codestream,_num_layer_specs,_layer_sizes,_layer_slopes,_min_slope_threshold,_no_prediction,(boolean) false,(boolean) true,(double) 0.0,(int) 0,(boolean) false,env,env_queue,(int) -1,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, int _min_slope_threshold, boolean _no_prediction, boolean _force_precise) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Start(_codestream,_num_layer_specs,_layer_sizes,_layer_slopes,_min_slope_threshold,_no_prediction,_force_precise,(boolean) true,(double) 0.0,(int) 0,(boolean) false,env,env_queue,(int) -1,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, int _min_slope_threshold, boolean _no_prediction, boolean _force_precise, boolean _record_layer_info_in_comment) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Start(_codestream,_num_layer_specs,_layer_sizes,_layer_slopes,_min_slope_threshold,_no_prediction,_force_precise,_record_layer_info_in_comment,(double) 0.0,(int) 0,(boolean) false,env,env_queue,(int) -1,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, int _min_slope_threshold, boolean _no_prediction, boolean _force_precise, boolean _record_layer_info_in_comment, double _size_tolerance) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Start(_codestream,_num_layer_specs,_layer_sizes,_layer_slopes,_min_slope_threshold,_no_prediction,_force_precise,_record_layer_info_in_comment,_size_tolerance,(int) 0,(boolean) false,env,env_queue,(int) -1,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, int _min_slope_threshold, boolean _no_prediction, boolean _force_precise, boolean _record_layer_info_in_comment, double _size_tolerance, int _num_components) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Start(_codestream,_num_layer_specs,_layer_sizes,_layer_slopes,_min_slope_threshold,_no_prediction,_force_precise,_record_layer_info_in_comment,_size_tolerance,_num_components,(boolean) false,env,env_queue,(int) -1,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, int _min_slope_threshold, boolean _no_prediction, boolean _force_precise, boolean _record_layer_info_in_comment, double _size_tolerance, int _num_components, boolean _want_fastest) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Start(_codestream,_num_layer_specs,_layer_sizes,_layer_slopes,_min_slope_threshold,_no_prediction,_force_precise,_record_layer_info_in_comment,_size_tolerance,_num_components,_want_fastest,env,env_queue,(int) -1,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, int _min_slope_threshold, boolean _no_prediction, boolean _force_precise, boolean _record_layer_info_in_comment, double _size_tolerance, int _num_components, boolean _want_fastest, Kdu_thread_env _env) throws KduException
  {
    Kdu_thread_queue env_queue = null;
    Start(_codestream,_num_layer_specs,_layer_sizes,_layer_slopes,_min_slope_threshold,_no_prediction,_force_precise,_record_layer_info_in_comment,_size_tolerance,_num_components,_want_fastest,_env,env_queue,(int) -1,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, int _min_slope_threshold, boolean _no_prediction, boolean _force_precise, boolean _record_layer_info_in_comment, double _size_tolerance, int _num_components, boolean _want_fastest, Kdu_thread_env _env, Kdu_thread_queue _env_queue) throws KduException
  {
    Start(_codestream,_num_layer_specs,_layer_sizes,_layer_slopes,_min_slope_threshold,_no_prediction,_force_precise,_record_layer_info_in_comment,_size_tolerance,_num_components,_want_fastest,_env,_env_queue,(int) -1,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, int _min_slope_threshold, boolean _no_prediction, boolean _force_precise, boolean _record_layer_info_in_comment, double _size_tolerance, int _num_components, boolean _want_fastest, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _env_dbuf_height) throws KduException
  {
    Start(_codestream,_num_layer_specs,_layer_sizes,_layer_slopes,_min_slope_threshold,_no_prediction,_force_precise,_record_layer_info_in_comment,_size_tolerance,_num_components,_want_fastest,_env,_env_queue,_env_dbuf_height,(int) -1,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, int _min_slope_threshold, boolean _no_prediction, boolean _force_precise, boolean _record_layer_info_in_comment, double _size_tolerance, int _num_components, boolean _want_fastest, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _env_dbuf_height, int _env_tile_concurrency) throws KduException
  {
    Start(_codestream,_num_layer_specs,_layer_sizes,_layer_slopes,_min_slope_threshold,_no_prediction,_force_precise,_record_layer_info_in_comment,_size_tolerance,_num_components,_want_fastest,_env,_env_queue,_env_dbuf_height,_env_tile_concurrency,(boolean) true,(int) 0);
  }
  public void Start(Kdu_codestream _codestream, int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, int _min_slope_threshold, boolean _no_prediction, boolean _force_precise, boolean _record_layer_info_in_comment, double _size_tolerance, int _num_components, boolean _want_fastest, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _env_dbuf_height, int _env_tile_concurrency, boolean _trim_to_rate) throws KduException
  {
    Start(_codestream,_num_layer_specs,_layer_sizes,_layer_slopes,_min_slope_threshold,_no_prediction,_force_precise,_record_layer_info_in_comment,_size_tolerance,_num_components,_want_fastest,_env,_env_queue,_env_dbuf_height,_env_tile_concurrency,_trim_to_rate,(int) 0);
  }
  public native long Get_set_next_queue_sequence(long _min_val) throws KduException;
  public native boolean Finish(int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes, Kdu_thread_env _alt_env) throws KduException;
  public boolean Finish() throws KduException
  {
    Kdu_thread_env alt_env = null;
    return Finish((int) 0,null,null,alt_env);
  }
  public boolean Finish(int _num_layer_specs) throws KduException
  {
    Kdu_thread_env alt_env = null;
    return Finish(_num_layer_specs,null,null,alt_env);
  }
  public boolean Finish(int _num_layer_specs, long[] _layer_sizes) throws KduException
  {
    Kdu_thread_env alt_env = null;
    return Finish(_num_layer_specs,_layer_sizes,null,alt_env);
  }
  public boolean Finish(int _num_layer_specs, long[] _layer_sizes, int[] _layer_slopes) throws KduException
  {
    Kdu_thread_env alt_env = null;
    return Finish(_num_layer_specs,_layer_sizes,_layer_slopes,alt_env);
  }
  public native void Reset(boolean _free_memory) throws KduException;
  public void Reset() throws KduException
  {
    Reset((boolean) true);
  }
  public native boolean Get_recommended_stripe_heights(int _preferred_min_height, int _absolute_max_height, int[] _stripe_heights, int[] _max_stripe_heights) throws KduException;
  public native boolean Get_next_stripe_heights(int _preferred_min_height, int _absolute_max_height, int[] _cur_stripe_heights, int[] _next_stripe_heights) throws KduException;
  public native boolean Push_stripe(byte[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, int _flush_period) throws KduException;
  public boolean Push_stripe(byte[] _buffer, int[] _stripe_heights) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,null,null,null,null,(int) 0);
  }
  public boolean Push_stripe(byte[] _buffer, int[] _stripe_heights, int[] _sample_offsets) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,null,null,null,(int) 0);
  }
  public boolean Push_stripe(byte[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,null,null,(int) 0);
  }
  public boolean Push_stripe(byte[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,null,(int) 0);
  }
  public boolean Push_stripe(byte[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,(int) 0);
  }
  public native boolean Push_stripe(short[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed, int _flush_period) throws KduException;
  public boolean Push_stripe(short[] _buffer, int[] _stripe_heights) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,null,null,null,null,null,(int) 0);
  }
  public boolean Push_stripe(short[] _buffer, int[] _stripe_heights, int[] _sample_offsets) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,null,null,null,null,(int) 0);
  }
  public boolean Push_stripe(short[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,null,null,null,(int) 0);
  }
  public boolean Push_stripe(short[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,null,null,(int) 0);
  }
  public boolean Push_stripe(short[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,null,(int) 0);
  }
  public boolean Push_stripe(short[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,_is_signed,(int) 0);
  }
  public native boolean Push_stripe(int[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed, int _flush_period) throws KduException;
  public boolean Push_stripe(int[] _buffer, int[] _stripe_heights) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,null,null,null,null,null,(int) 0);
  }
  public boolean Push_stripe(int[] _buffer, int[] _stripe_heights, int[] _sample_offsets) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,null,null,null,null,(int) 0);
  }
  public boolean Push_stripe(int[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,null,null,null,(int) 0);
  }
  public boolean Push_stripe(int[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,null,null,(int) 0);
  }
  public boolean Push_stripe(int[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,null,(int) 0);
  }
  public boolean Push_stripe(int[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,_is_signed,(int) 0);
  }
  public native boolean Push_stripe(float[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed, int _flush_period) throws KduException;
  public boolean Push_stripe(float[] _buffer, int[] _stripe_heights) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,null,null,null,null,null,(int) 0);
  }
  public boolean Push_stripe(float[] _buffer, int[] _stripe_heights, int[] _sample_offsets) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,null,null,null,null,(int) 0);
  }
  public boolean Push_stripe(float[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,null,null,null,(int) 0);
  }
  public boolean Push_stripe(float[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,null,null,(int) 0);
  }
  public boolean Push_stripe(float[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,null,(int) 0);
  }
  public boolean Push_stripe(float[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed) throws KduException
  {
    return Push_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,_is_signed,(int) 0);
  }
}
