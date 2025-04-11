package kdu_jni;

public class Kdu_ovlp_stream_decompressor {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_ovlp_stream_decompressor(long ptr) {
    _native_ptr = ptr;
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  public native void Set_service_successor(Kdu_ovlp_stream_decompressor _successor) throws KduException;
  public native void Set_consumer_successor(Kdu_ovlp_stream_decompressor _successor) throws KduException;
  public native void Set_resilience_opts(boolean _fussy, boolean _resilient, boolean _expect_ubiquitous_sop, boolean _allow_parsing_errors, boolean _propagate_to_consumers) throws KduException;
  public void Set_resilience_opts(boolean _fussy, boolean _resilient, boolean _expect_ubiquitous_sop, boolean _allow_parsing_errors) throws KduException
  {
    Set_resilience_opts(_fussy,_resilient,_expect_ubiquitous_sop,_allow_parsing_errors,(boolean) true);
  }
  public native void Set_proc_opts(int _preferred_min_stripe_height, int _max_stripe_height, boolean _force_precise, boolean _want_fastest, int _dbuf_height, Kdu_push_pull_params _extra_params, boolean _propagate_to_consumers) throws KduException;
  public void Set_proc_opts(int _preferred_min_stripe_height, int _max_stripe_height, boolean _force_precise, boolean _want_fastest) throws KduException
  {
    Kdu_push_pull_params extra_params = null;
    Set_proc_opts(_preferred_min_stripe_height,_max_stripe_height,_force_precise,_want_fastest,(int) -1,extra_params,(boolean) true);
  }
  public void Set_proc_opts(int _preferred_min_stripe_height, int _max_stripe_height, boolean _force_precise, boolean _want_fastest, int _dbuf_height) throws KduException
  {
    Kdu_push_pull_params extra_params = null;
    Set_proc_opts(_preferred_min_stripe_height,_max_stripe_height,_force_precise,_want_fastest,_dbuf_height,extra_params,(boolean) true);
  }
  public void Set_proc_opts(int _preferred_min_stripe_height, int _max_stripe_height, boolean _force_precise, boolean _want_fastest, int _dbuf_height, Kdu_push_pull_params _extra_params) throws KduException
  {
    Set_proc_opts(_preferred_min_stripe_height,_max_stripe_height,_force_precise,_want_fastest,_dbuf_height,_extra_params,(boolean) true);
  }
  public native void Set_mem_opts(Kdu_membroker _membroker, int _frag_bits, boolean _propagate_to_consumers) throws KduException;
  public void Set_mem_opts(Kdu_membroker _membroker, int _frag_bits) throws KduException
  {
    Set_mem_opts(_membroker,_frag_bits,(boolean) true);
  }
  public native void Set_live_mode(boolean _is_live, boolean _service_propagate) throws KduException;
  public void Set_live_mode(boolean _is_live) throws KduException
  {
    Set_live_mode(_is_live,(boolean) true);
  }
  public native void Set_post_discard_mode(boolean _allow_post_discard, boolean _service_propagate) throws KduException;
  public void Set_post_discard_mode(boolean _allow_post_discard) throws KduException
  {
    Set_post_discard_mode(_allow_post_discard,(boolean) true);
  }
  public native boolean Enter_service(boolean _service_propagate) throws KduException;
  public boolean Enter_service() throws KduException
  {
    return Enter_service((boolean) true);
  }
  public native void Service_finished(boolean _service_propagate) throws KduException;
  public void Service_finished() throws KduException
  {
    Service_finished((boolean) true);
  }
  public native void Install_stream_filter(Kdu_ovlp_stream_filter _filter, boolean _service_propagate) throws KduException;
  public void Install_stream_filter(Kdu_ovlp_stream_filter _filter) throws KduException
  {
    Install_stream_filter(_filter,(boolean) true);
  }
  public native void Install_ovlp_service_observer(Kdu_ovlp_service_observer _observer, boolean _service_propagate) throws KduException;
  public void Install_ovlp_service_observer(Kdu_ovlp_service_observer _observer) throws KduException
  {
    Install_ovlp_service_observer(_observer,(boolean) true);
  }
  public native void Stop_all_fetching(boolean _service_propagate) throws KduException;
  public void Stop_all_fetching() throws KduException
  {
    Stop_all_fetching((boolean) true);
  }
  public native long Consumer_get_max_stripe_heights() throws KduException;
  public native long Consumer_get_next_stripe_heights() throws KduException;
  public native int Consumer_get_num_components() throws KduException;
  public native Kdu_dims Consumer_get_dims(int _comp_idx) throws KduException;
  public native Kdu_dims Consumer_get_full_dims(int _comp_idx, boolean _respect_discarded_levels) throws KduException;
  public native Kdu_coords Consumer_get_subsampling(int _comp_idx) throws KduException;
  public native Kdu_coords Consumer_get_min_subsampling(int _comp_idx) throws KduException;
  public native Kdu_coords Consumer_get_registration(int _comp_idx, Kdu_coords _scale) throws KduException;
  public native boolean Consumer_get_precision(int _comp_idx, int[] _precision, boolean[] _is_signed) throws KduException;
  public native int Consumer_get_input_restrictions(int[] _discard_levels, int[] _max_layers, int[] _access_mode) throws KduException;
  public native boolean Consumer_get_appearance(boolean[] _transpose, boolean[] _vflip, boolean[] _hflip) throws KduException;
  public native void Consumer_shutdown(Kdu_thread_env _env, boolean _propagate_to_consumers) throws KduException;
  public void Consumer_shutdown(Kdu_thread_env _env) throws KduException
  {
    Consumer_shutdown(_env,(boolean) true);
  }
  public native void Install_stream_transform(Kdu_ovlp_stream_transform _transform, boolean _consumer_propagate) throws KduException;
  public void Install_stream_transform(Kdu_ovlp_stream_transform _transform) throws KduException
  {
    Install_stream_transform(_transform,(boolean) true);
  }
}
