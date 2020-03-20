package kdu_jni;

public class Kdu_analysis_fusion extends Kdu_push_ifc {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_analysis_fusion(long ptr) {
    super(ptr);
  }
  private native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create(Kdu_node _node, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, int _push_width, int _invalid_extent_left, int _invalid_extent_right, Kdu_roi_node _roi);
  public Kdu_analysis_fusion(Kdu_node _node, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, int _push_width, int _invalid_extent_left, int _invalid_extent_right, Kdu_roi_node _roi) {
    this(Native_create(_node, _allocator, _params, _use_shorts, _normalization, _push_offset, _push_width, _invalid_extent_left, _invalid_extent_right, _roi));
  }
  private static long Native_create(Kdu_node _node, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, int _push_width, int _invalid_extent_left, int _invalid_extent_right)
  {
    Kdu_roi_node roi = null;
    return Native_create(_node,_allocator,_params,_use_shorts,_normalization,_push_offset,_push_width,_invalid_extent_left,_invalid_extent_right,roi);
  }
  public Kdu_analysis_fusion(Kdu_node _node, Kdu_sample_allocator _allocator, Kdu_push_pull_params _params, boolean _use_shorts, float _normalization, int _push_offset, int _push_width, int _invalid_extent_left, int _invalid_extent_right) {
    this(Native_create(_node, _allocator, _params, _use_shorts, _normalization, _push_offset, _push_width, _invalid_extent_left, _invalid_extent_right));
  }
}
