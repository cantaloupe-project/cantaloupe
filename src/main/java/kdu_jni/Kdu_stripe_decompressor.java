package kdu_jni;

public class Kdu_stripe_decompressor {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_stripe_decompressor(long ptr) {
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
  public Kdu_stripe_decompressor() {
    this(Native_create());
  }
  public native void Mem_configure(Kdu_membroker _membroker, int _frag_bits) throws KduException;
  public native void Start(Kdu_codestream _codestream, boolean _force_precise, boolean _want_fastest, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _env_dbuf_height, int _env_tile_concurrency, Kdu_push_pull_params _multi_xform_extra_params) throws KduException;
  public void Start(Kdu_codestream _codestream) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Kdu_push_pull_params multi_xform_extra_params = null;
    Start(_codestream,(boolean) false,(boolean) false,env,env_queue,(int) -1,(int) -1,multi_xform_extra_params);
  }
  public void Start(Kdu_codestream _codestream, boolean _force_precise) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Kdu_push_pull_params multi_xform_extra_params = null;
    Start(_codestream,_force_precise,(boolean) false,env,env_queue,(int) -1,(int) -1,multi_xform_extra_params);
  }
  public void Start(Kdu_codestream _codestream, boolean _force_precise, boolean _want_fastest) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    Kdu_push_pull_params multi_xform_extra_params = null;
    Start(_codestream,_force_precise,_want_fastest,env,env_queue,(int) -1,(int) -1,multi_xform_extra_params);
  }
  public void Start(Kdu_codestream _codestream, boolean _force_precise, boolean _want_fastest, Kdu_thread_env _env) throws KduException
  {
    Kdu_thread_queue env_queue = null;
    Kdu_push_pull_params multi_xform_extra_params = null;
    Start(_codestream,_force_precise,_want_fastest,_env,env_queue,(int) -1,(int) -1,multi_xform_extra_params);
  }
  public void Start(Kdu_codestream _codestream, boolean _force_precise, boolean _want_fastest, Kdu_thread_env _env, Kdu_thread_queue _env_queue) throws KduException
  {
    Kdu_push_pull_params multi_xform_extra_params = null;
    Start(_codestream,_force_precise,_want_fastest,_env,_env_queue,(int) -1,(int) -1,multi_xform_extra_params);
  }
  public void Start(Kdu_codestream _codestream, boolean _force_precise, boolean _want_fastest, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _env_dbuf_height) throws KduException
  {
    Kdu_push_pull_params multi_xform_extra_params = null;
    Start(_codestream,_force_precise,_want_fastest,_env,_env_queue,_env_dbuf_height,(int) -1,multi_xform_extra_params);
  }
  public void Start(Kdu_codestream _codestream, boolean _force_precise, boolean _want_fastest, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _env_dbuf_height, int _env_tile_concurrency) throws KduException
  {
    Kdu_push_pull_params multi_xform_extra_params = null;
    Start(_codestream,_force_precise,_want_fastest,_env,_env_queue,_env_dbuf_height,_env_tile_concurrency,multi_xform_extra_params);
  }
  public native boolean Finish() throws KduException;
  public native void Reset(boolean _free_memory) throws KduException;
  public void Reset() throws KduException
  {
    Reset((boolean) true);
  }
  public native boolean Get_recommended_stripe_heights(int _preferred_min_height, int _absolute_max_height, int[] _stripe_heights, int[] _max_stripe_heights) throws KduException;
  public native boolean Pull_stripe(byte[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, int[] _pad_flags, int _vectorized_store_prefs) throws KduException;
  public boolean Pull_stripe(byte[] _buffer, int[] _stripe_heights) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,null,null,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(byte[] _buffer, int[] _stripe_heights, int[] _sample_offsets) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,null,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(byte[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(byte[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,null,null,(int) 0);
  }
  public boolean Pull_stripe(byte[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,null,(int) 0);
  }
  public boolean Pull_stripe(byte[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, int[] _pad_flags) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,_pad_flags,(int) 0);
  }
  public native boolean Pull_stripe(short[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed, int[] _pad_flags, int _vectorized_store_prefs) throws KduException;
  public boolean Pull_stripe(short[] _buffer, int[] _stripe_heights) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,null,null,null,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(short[] _buffer, int[] _stripe_heights, int[] _sample_offsets) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,null,null,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(short[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,null,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(short[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(short[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,null,null,(int) 0);
  }
  public boolean Pull_stripe(short[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,_is_signed,null,(int) 0);
  }
  public boolean Pull_stripe(short[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed, int[] _pad_flags) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,_is_signed,_pad_flags,(int) 0);
  }
  public native boolean Pull_stripe(int[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed, int[] _pad_flags, int _vectorized_store_prefs) throws KduException;
  public boolean Pull_stripe(int[] _buffer, int[] _stripe_heights) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,null,null,null,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(int[] _buffer, int[] _stripe_heights, int[] _sample_offsets) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,null,null,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(int[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,null,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(int[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(int[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,null,null,(int) 0);
  }
  public boolean Pull_stripe(int[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,_is_signed,null,(int) 0);
  }
  public boolean Pull_stripe(int[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed, int[] _pad_flags) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,_is_signed,_pad_flags,(int) 0);
  }
  public native boolean Pull_stripe(float[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed, int[] _pad_flags, int _vectorized_store_prefs) throws KduException;
  public boolean Pull_stripe(float[] _buffer, int[] _stripe_heights) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,null,null,null,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(float[] _buffer, int[] _stripe_heights, int[] _sample_offsets) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,null,null,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(float[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,null,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(float[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,null,null,null,(int) 0);
  }
  public boolean Pull_stripe(float[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,null,null,(int) 0);
  }
  public boolean Pull_stripe(float[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,_is_signed,null,(int) 0);
  }
  public boolean Pull_stripe(float[] _buffer, int[] _stripe_heights, int[] _sample_offsets, int[] _sample_gaps, int[] _row_gaps, int[] _precisions, boolean[] _is_signed, int[] _pad_flags) throws KduException
  {
    return Pull_stripe(_buffer,_stripe_heights,_sample_offsets,_sample_gaps,_row_gaps,_precisions,_is_signed,_pad_flags,(int) 0);
  }
}
