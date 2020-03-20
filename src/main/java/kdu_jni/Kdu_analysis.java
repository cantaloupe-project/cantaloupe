package kdu_jni;

public class Kdu_analysis extends Kdu_push_ifc {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_analysis(long ptr) {
    super(ptr);
  }
  private native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create(Kdu_node _node, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, Kdu_roi_node _roi);
  public Kdu_analysis(Kdu_node _node, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, Kdu_roi_node _roi) {
    this(Native_create(_node, _allocator, _params, _use_shorts, _normalization, _push_offset, _roi));
  }
  private static long Native_create(Kdu_node _node, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts)
  {
    Kdu_roi_node roi = null;
    return Native_create(_node,_allocator,_params,_use_shorts,(float) 1.0F,(int) 0,roi);
  }
  public Kdu_analysis(Kdu_node _node, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts) {
    this(Native_create(_node, _allocator, _params, _use_shorts));
  }
  private static long Native_create(Kdu_node _node, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization)
  {
    Kdu_roi_node roi = null;
    return Native_create(_node,_allocator,_params,_use_shorts,_normalization,(int) 0,roi);
  }
  public Kdu_analysis(Kdu_node _node, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization) {
    this(Native_create(_node, _allocator, _params, _use_shorts, _normalization));
  }
  private static long Native_create(Kdu_node _node, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset)
  {
    Kdu_roi_node roi = null;
    return Native_create(_node,_allocator,_params,_use_shorts,_normalization,_push_offset,roi);
  }
  public Kdu_analysis(Kdu_node _node, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset) {
    this(Native_create(_node, _allocator, _params, _use_shorts, _normalization, _push_offset));
  }
  private static native long Native_create(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, Kdu_roi_node _roi, Kdu_thread_env _env, Kdu_thread_queue _env_queue);
  public Kdu_analysis(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, Kdu_roi_node _roi, Kdu_thread_env _env, Kdu_thread_queue _env_queue) {
    this(Native_create(_resolution, _allocator, _params, _use_shorts, _normalization, _push_offset, _roi, _env, _env_queue));
  }
  private static long Native_create(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts)
  {
    Kdu_roi_node roi = null;
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Native_create(_resolution,_allocator,_params,_use_shorts,(float) 1.0,(int) 0,roi,env,env_queue);
  }
  public Kdu_analysis(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts) {
    this(Native_create(_resolution, _allocator, _params, _use_shorts));
  }
  private static long Native_create(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization)
  {
    Kdu_roi_node roi = null;
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Native_create(_resolution,_allocator,_params,_use_shorts,_normalization,(int) 0,roi,env,env_queue);
  }
  public Kdu_analysis(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization) {
    this(Native_create(_resolution, _allocator, _params, _use_shorts, _normalization));
  }
  private static long Native_create(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset)
  {
    Kdu_roi_node roi = null;
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Native_create(_resolution,_allocator,_params,_use_shorts,_normalization,_push_offset,roi,env,env_queue);
  }
  public Kdu_analysis(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset) {
    this(Native_create(_resolution, _allocator, _params, _use_shorts, _normalization, _push_offset));
  }
  private static long Native_create(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, Kdu_roi_node _roi)
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Native_create(_resolution,_allocator,_params,_use_shorts,_normalization,_push_offset,_roi,env,env_queue);
  }
  public Kdu_analysis(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, Kdu_roi_node _roi) {
    this(Native_create(_resolution, _allocator, _params, _use_shorts, _normalization, _push_offset, _roi));
  }
  private static long Native_create(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, Kdu_roi_node _roi, Kdu_thread_env _env)
  {
    Kdu_thread_queue env_queue = null;
    return Native_create(_resolution,_allocator,_params,_use_shorts,_normalization,_push_offset,_roi,_env,env_queue);
  }
  public Kdu_analysis(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, Kdu_roi_node _roi, Kdu_thread_env _env) {
    this(Native_create(_resolution, _allocator, _params, _use_shorts, _normalization, _push_offset, _roi, _env));
  }
  private static native long Native_create(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, Kdu_roi_node _roi, Kdu_thread_env _env, Kdu_thread_queue _env_queue);
  public Kdu_analysis(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, Kdu_roi_node _roi, Kdu_thread_env _env, Kdu_thread_queue _env_queue) {
    this(Native_create(_resolution, _allocator, _use_shorts, _normalization, _roi, _env, _env_queue));
  }
  private static long Native_create(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, boolean _use_shorts)
  {
    Kdu_roi_node roi = null;
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Native_create(_resolution,_allocator,_use_shorts,(float) 1.0,roi,env,env_queue);
  }
  public Kdu_analysis(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, boolean _use_shorts) {
    this(Native_create(_resolution, _allocator, _use_shorts));
  }
  private static long Native_create(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization)
  {
    Kdu_roi_node roi = null;
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Native_create(_resolution,_allocator,_use_shorts,_normalization,roi,env,env_queue);
  }
  public Kdu_analysis(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization) {
    this(Native_create(_resolution, _allocator, _use_shorts, _normalization));
  }
  private static long Native_create(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, Kdu_roi_node _roi)
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Native_create(_resolution,_allocator,_use_shorts,_normalization,_roi,env,env_queue);
  }
  public Kdu_analysis(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, Kdu_roi_node _roi) {
    this(Native_create(_resolution, _allocator, _use_shorts, _normalization, _roi));
  }
  private static long Native_create(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, Kdu_roi_node _roi, Kdu_thread_env _env)
  {
    Kdu_thread_queue env_queue = null;
    return Native_create(_resolution,_allocator,_use_shorts,_normalization,_roi,_env,env_queue);
  }
  public Kdu_analysis(Kdu_resolution _resolution, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, Kdu_roi_node _roi, Kdu_thread_env _env) {
    this(Native_create(_resolution, _allocator, _use_shorts, _normalization, _roi, _env));
  }
}
