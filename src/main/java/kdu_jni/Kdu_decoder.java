package kdu_jni;

public class Kdu_decoder extends Kdu_pull_ifc {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_decoder(long ptr) {
    super(ptr);
  }
  private native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, int _pull_offset, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _flags, float[] _postquant_scale);
  public Kdu_decoder(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, int _pull_offset, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _flags, float[] _postquant_scale) {
    this(Native_create(_subband, _allocator, _use_shorts, _normalization, _pull_offset, _env, _env_queue, _flags, _postquant_scale));
  }
  private static long Native_create(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts)
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Native_create(_subband,_allocator,_use_shorts,(float) 1.0F,(int) 0,env,env_queue,(int) 0,null);
  }
  public Kdu_decoder(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts) {
    this(Native_create(_subband, _allocator, _use_shorts));
  }
  private static long Native_create(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization)
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Native_create(_subband,_allocator,_use_shorts,_normalization,(int) 0,env,env_queue,(int) 0,null);
  }
  public Kdu_decoder(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization) {
    this(Native_create(_subband, _allocator, _use_shorts, _normalization));
  }
  private static long Native_create(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, int _pull_offset)
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Native_create(_subband,_allocator,_use_shorts,_normalization,_pull_offset,env,env_queue,(int) 0,null);
  }
  public Kdu_decoder(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, int _pull_offset) {
    this(Native_create(_subband, _allocator, _use_shorts, _normalization, _pull_offset));
  }
  private static long Native_create(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, int _pull_offset, Kdu_thread_env _env)
  {
    Kdu_thread_queue env_queue = null;
    return Native_create(_subband,_allocator,_use_shorts,_normalization,_pull_offset,_env,env_queue,(int) 0,null);
  }
  public Kdu_decoder(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, int _pull_offset, Kdu_thread_env _env) {
    this(Native_create(_subband, _allocator, _use_shorts, _normalization, _pull_offset, _env));
  }
  private static long Native_create(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, int _pull_offset, Kdu_thread_env _env, Kdu_thread_queue _env_queue)
  {
    return Native_create(_subband,_allocator,_use_shorts,_normalization,_pull_offset,_env,_env_queue,(int) 0,null);
  }
  public Kdu_decoder(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, int _pull_offset, Kdu_thread_env _env, Kdu_thread_queue _env_queue) {
    this(Native_create(_subband, _allocator, _use_shorts, _normalization, _pull_offset, _env, _env_queue));
  }
  private static long Native_create(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, int _pull_offset, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _flags)
  {
    return Native_create(_subband,_allocator,_use_shorts,_normalization,_pull_offset,_env,_env_queue,_flags,null);
  }
  public Kdu_decoder(Kdu_subband _subband, Kdu_sample_allocator _allocator, boolean _use_shorts, float _normalization, int _pull_offset, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _flags) {
    this(Native_create(_subband, _allocator, _use_shorts, _normalization, _pull_offset, _env, _env_queue, _flags));
  }
}
