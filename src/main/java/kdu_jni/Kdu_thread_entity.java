package kdu_jni;

public class Kdu_thread_entity {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_thread_entity(long ptr) {
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
  public Kdu_thread_entity() {
    this(Native_create());
  }
  public native Kdu_thread_entity New_instance() throws KduException;
  public native boolean Exists() throws KduException;
  public native boolean Is_group_owner() throws KduException;
  public native int Get_thread_id() throws KduException;
  public native boolean Check_current_thread() throws KduException;
  public native boolean Change_group_owner_thread() throws KduException;
  public native void Create() throws KduException;
  public native boolean Destroy() throws KduException;
  public native boolean Set_cpu_affinity(Kdu_thread_entity_affinity _affinity) throws KduException;
  public native void Set_min_thread_concurrency(int _min_concurrency) throws KduException;
  public void Set_min_thread_concurrency() throws KduException
  {
    Set_min_thread_concurrency((int) 0);
  }
  public native int Get_num_threads(String _domain_name) throws KduException;
  public int Get_num_threads() throws KduException
  {
    return Get_num_threads(null);
  }
  public native boolean Add_thread(String _domain_name) throws KduException;
  public boolean Add_thread() throws KduException
  {
    return Add_thread(null);
  }
  public native boolean Declare_first_owner_wait_safe(boolean _is_safe) throws KduException;
  public native void Set_yield_frequency(int _worker_yield_freq) throws KduException;
  public native long Get_job_count_stats(long[] _group_owner_job_count) throws KduException;
  public native boolean Attach_queue(Kdu_thread_queue _queue, Kdu_thread_queue _super_queue, String _domain_name, long _min_sequencing_idx, int _queue_flags) throws KduException;
  public boolean Attach_queue(Kdu_thread_queue _queue, Kdu_thread_queue _super_queue, String _domain_name) throws KduException
  {
    return Attach_queue(_queue,_super_queue,_domain_name,(long) 0,(int) 0);
  }
  public boolean Attach_queue(Kdu_thread_queue _queue, Kdu_thread_queue _super_queue, String _domain_name, long _min_sequencing_idx) throws KduException
  {
    return Attach_queue(_queue,_super_queue,_domain_name,_min_sequencing_idx,(int) 0);
  }
  public native void Advance_work_domains() throws KduException;
  public native Kdu_thread_entity_condition Get_condition() throws KduException;
  public native void Wait_for_condition(String _debug_text) throws KduException;
  public void Wait_for_condition() throws KduException
  {
    Wait_for_condition(null);
  }
  public native void Signal_condition(Kdu_thread_entity_condition _cond, boolean _foreign_caller) throws KduException;
  public void Signal_condition(Kdu_thread_entity_condition _cond) throws KduException
  {
    Signal_condition(_cond,(boolean) false);
  }
  public native boolean Join(Kdu_thread_queue _root_queue, boolean _descendants_only, int[] _exc_code) throws KduException;
  public boolean Join(Kdu_thread_queue _root_queue) throws KduException
  {
    return Join(_root_queue,(boolean) false,null);
  }
  public boolean Join(Kdu_thread_queue _root_queue, boolean _descendants_only) throws KduException
  {
    return Join(_root_queue,_descendants_only,null);
  }
  public native boolean Terminate(Kdu_thread_queue _root_queue, boolean _descendants_only, int[] _exc_code) throws KduException;
  public boolean Terminate(Kdu_thread_queue _root_queue) throws KduException
  {
    return Terminate(_root_queue,(boolean) false,null);
  }
  public boolean Terminate(Kdu_thread_queue _root_queue, boolean _descendants_only) throws KduException
  {
    return Terminate(_root_queue,_descendants_only,null);
  }
  public native void Handle_exception(int _exc_code) throws KduException;
}
