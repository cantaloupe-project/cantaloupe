package kdu_jni;

public class Kdu_encoder extends Kdu_push_ifc {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_encoder(long ptr) {
    super(ptr);
  }
  private native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create(Kdu_subband _subband, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, Kdu_roi_node _roi);
  public Kdu_encoder(Kdu_subband _subband, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, Kdu_roi_node _roi) {
    this(Native_create(_subband, _allocator, _params, _use_shorts, _normalization, _push_offset, _roi));
  }
  private static long Native_create(Kdu_subband _subband, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts)
  {
    Kdu_roi_node roi = null;
    return Native_create(_subband,_allocator,_params,_use_shorts,(float) 1.0F,(int) 0,roi);
  }
  public Kdu_encoder(Kdu_subband _subband, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts) {
    this(Native_create(_subband, _allocator, _params, _use_shorts));
  }
  private static long Native_create(Kdu_subband _subband, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization)
  {
    Kdu_roi_node roi = null;
    return Native_create(_subband,_allocator,_params,_use_shorts,_normalization,(int) 0,roi);
  }
  public Kdu_encoder(Kdu_subband _subband, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization) {
    this(Native_create(_subband, _allocator, _params, _use_shorts, _normalization));
  }
  private static long Native_create(Kdu_subband _subband, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset)
  {
    Kdu_roi_node roi = null;
    return Native_create(_subband,_allocator,_params,_use_shorts,_normalization,_push_offset,roi);
  }
  public Kdu_encoder(Kdu_subband _subband, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset) {
    this(Native_create(_subband, _allocator, _params, _use_shorts, _normalization, _push_offset));
  }
}
