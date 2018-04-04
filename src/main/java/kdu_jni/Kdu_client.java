package kdu_jni;

public class Kdu_client extends Kdu_cache {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_client(long ptr) {
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
  public Kdu_client() {
    this(Native_create());
  }
  public native String Check_compatible_url(String _url, boolean _resource_component_must_exist) throws KduException;
  public native boolean Check_cache_file(String _filename, Kdu_cache_file_info _info) throws KduException;
  public native void Install_context_translator(Kdu_client_translator _translator) throws KduException;
  public native void Install_notifier(Kdu_client_notifier _notifier) throws KduException;
  public native void Set_primary_timeout(long _timeout_usecs) throws KduException;
  public native void Set_aux_tcp_timeout(long _timeout_usecs) throws KduException;
  public native int Connect(String _server, String _proxy, String _request, String _channel_transport, String _cache_dir, int _mode, String _compatible_url, int _cache_file_handling) throws KduException;
  public native int Open_with_cache_file(String _filename, String _cache_dir, int _cache_file_handling, boolean _preamble_only) throws KduException;
  public int Open_with_cache_file(String _filename, String _cache_dir, int _cache_file_handling) throws KduException
  {
    return Open_with_cache_file(_filename,_cache_dir,_cache_file_handling,(boolean) false);
  }
  public native void Set_cache_file_handling(int _handling) throws KduException;
  public native int Get_cache_file_handling() throws KduException;
  public native boolean Will_lose_data_if_closed() throws KduException;
  public native String Construct_jpip_url() throws KduException;
  public native boolean Augment_with_cache_file(String _filename) throws KduException;
  public native int Reconnect(String _channel_transport, String _proxy, boolean _clear_cache) throws KduException;
  public int Reconnect(String _channel_transport, String _proxy) throws KduException
  {
    return Reconnect(_channel_transport,_proxy,(boolean) false);
  }
  public native boolean Is_interactive() throws KduException;
  public native boolean Is_one_time_request() throws KduException;
  public native boolean Connect_request_has_non_empty_window() throws KduException;
  public native String Get_target_name() throws KduException;
  public native boolean Check_compatible_connection(String _server, String _request, int _mode, String _compatible_url) throws KduException;
  public boolean Check_compatible_connection(String _server, String _request, int _mode) throws KduException
  {
    return Check_compatible_connection(_server,_request,_mode,null);
  }
  public native int Add_queue() throws KduException;
  public native boolean Is_active() throws KduException;
  public native boolean Target_started() throws KduException;
  public native boolean Check_stateless() throws KduException;
  public native String Get_cache_identifier() throws KduException;
  public native boolean Is_reconnecting() throws KduException;
  public native boolean Target_incompatible() throws KduException;
  public native boolean Is_alive(int _queue_id) throws KduException;
  public boolean Is_alive() throws KduException
  {
    return Is_alive((int) -1);
  }
  public native boolean Is_idle(int _queue_id) throws KduException;
  public boolean Is_idle() throws KduException
  {
    return Is_idle((int) -1);
  }
  public native void Disconnect(boolean _keep_transport_open, int _timeout_milliseconds, int _queue_id, boolean _wait_for_completion) throws KduException;
  public void Disconnect() throws KduException
  {
    Disconnect((boolean) false,(int) 2000,(int) -1,(boolean) true);
  }
  public void Disconnect(boolean _keep_transport_open) throws KduException
  {
    Disconnect(_keep_transport_open,(int) 2000,(int) -1,(boolean) true);
  }
  public void Disconnect(boolean _keep_transport_open, int _timeout_milliseconds) throws KduException
  {
    Disconnect(_keep_transport_open,_timeout_milliseconds,(int) -1,(boolean) true);
  }
  public void Disconnect(boolean _keep_transport_open, int _timeout_milliseconds, int _queue_id) throws KduException
  {
    Disconnect(_keep_transport_open,_timeout_milliseconds,_queue_id,(boolean) true);
  }
  public native boolean Post_window(Kdu_window _window, int _queue_id, boolean _preemptive, Kdu_window_prefs _prefs, long _custom_id, long _service_usecs) throws KduException;
  public boolean Post_window(Kdu_window _window) throws KduException
  {
    Kdu_window_prefs prefs = null;
    return Post_window(_window,(int) 0,(boolean) true,prefs,(long) 0,(long) 0);
  }
  public boolean Post_window(Kdu_window _window, int _queue_id) throws KduException
  {
    Kdu_window_prefs prefs = null;
    return Post_window(_window,_queue_id,(boolean) true,prefs,(long) 0,(long) 0);
  }
  public boolean Post_window(Kdu_window _window, int _queue_id, boolean _preemptive) throws KduException
  {
    Kdu_window_prefs prefs = null;
    return Post_window(_window,_queue_id,_preemptive,prefs,(long) 0,(long) 0);
  }
  public boolean Post_window(Kdu_window _window, int _queue_id, boolean _preemptive, Kdu_window_prefs _prefs) throws KduException
  {
    return Post_window(_window,_queue_id,_preemptive,_prefs,(long) 0,(long) 0);
  }
  public boolean Post_window(Kdu_window _window, int _queue_id, boolean _preemptive, Kdu_window_prefs _prefs, long _custom_id) throws KduException
  {
    return Post_window(_window,_queue_id,_preemptive,_prefs,_custom_id,(long) 0);
  }
  public native long Sync_timing(int _queue_id, long _app_time_usecs, boolean _expect_preemptive) throws KduException;
  public native long Get_timed_request_horizon(int _queue_id, boolean _expect_preemptive) throws KduException;
  public native long Trim_timed_requests(int _queue_id, long[] _custom_id, boolean[] _partially_sent) throws KduException;
  public native boolean Get_window_in_progress(Kdu_window _window, int _queue_id, int[] _status_flags, long[] _custom_id, boolean _last_window_if_not_alive) throws KduException;
  public boolean Get_window_in_progress(Kdu_window _window) throws KduException
  {
    return Get_window_in_progress(_window,(int) 0,null,null,(boolean) false);
  }
  public boolean Get_window_in_progress(Kdu_window _window, int _queue_id) throws KduException
  {
    return Get_window_in_progress(_window,_queue_id,null,null,(boolean) false);
  }
  public boolean Get_window_in_progress(Kdu_window _window, int _queue_id, int[] _status_flags) throws KduException
  {
    return Get_window_in_progress(_window,_queue_id,_status_flags,null,(boolean) false);
  }
  public boolean Get_window_in_progress(Kdu_window _window, int _queue_id, int[] _status_flags, long[] _custom_id) throws KduException
  {
    return Get_window_in_progress(_window,_queue_id,_status_flags,_custom_id,(boolean) false);
  }
  public native boolean Get_window_info(int _queue_id, int[] _status_flags, long[] _custom_id, Kdu_window _window, long[] _service_usecs) throws KduException;
  public boolean Get_window_info(int _queue_id, int[] _status_flags, long[] _custom_id) throws KduException
  {
    Kdu_window window = null;
    return Get_window_info(_queue_id,_status_flags,_custom_id,window,null);
  }
  public boolean Get_window_info(int _queue_id, int[] _status_flags, long[] _custom_id, Kdu_window _window) throws KduException
  {
    return Get_window_info(_queue_id,_status_flags,_custom_id,_window,null);
  }
  public native boolean Post_oob_window(Kdu_window _window, int _caller_id, boolean _preemptive) throws KduException;
  public boolean Post_oob_window(Kdu_window _window) throws KduException
  {
    return Post_oob_window(_window,(int) 0,(boolean) true);
  }
  public boolean Post_oob_window(Kdu_window _window, int _caller_id) throws KduException
  {
    return Post_oob_window(_window,_caller_id,(boolean) true);
  }
  public native boolean Get_oob_window_in_progress(Kdu_window _window, int _caller_id, int[] _status_flags) throws KduException;
  public boolean Get_oob_window_in_progress(Kdu_window _window) throws KduException
  {
    return Get_oob_window_in_progress(_window,(int) 0,null);
  }
  public boolean Get_oob_window_in_progress(Kdu_window _window, int _caller_id) throws KduException
  {
    return Get_oob_window_in_progress(_window,_caller_id,null);
  }
  public native String Get_status(int _queue_id) throws KduException;
  public String Get_status() throws KduException
  {
    return Get_status((int) 0);
  }
  public native boolean Get_timing_info(int _queue_id, double[] _request_rtt, double[] _suggested_min_posting_interval) throws KduException;
  public boolean Get_timing_info(int _queue_id) throws KduException
  {
    return Get_timing_info(_queue_id,null,null);
  }
  public boolean Get_timing_info(int _queue_id, double[] _request_rtt) throws KduException
  {
    return Get_timing_info(_queue_id,_request_rtt,null);
  }
  public native long Get_received_bytes(int _queue_id, double[] _non_idle_seconds, double[] _seconds_since_first_active) throws KduException;
  public long Get_received_bytes() throws KduException
  {
    return Get_received_bytes((int) -1,null,null);
  }
  public long Get_received_bytes(int _queue_id) throws KduException
  {
    return Get_received_bytes(_queue_id,null,null);
  }
  public long Get_received_bytes(int _queue_id, double[] _non_idle_seconds) throws KduException
  {
    return Get_received_bytes(_queue_id,_non_idle_seconds,null);
  }
  public native void Set_preserve_window(Kdu_window _window, boolean _save_cache_files_with_preamble) throws KduException;
  public void Set_preserve_window(Kdu_window _window) throws KduException
  {
    Set_preserve_window(_window,(boolean) true);
  }
}
