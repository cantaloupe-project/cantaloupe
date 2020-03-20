package kdu_jni;

public class Kdu_push_pull_params {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_push_pull_params(long ptr) {
    _native_ptr = ptr;
  }
  private native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create();
  public Kdu_push_pull_params() {
    this(Native_create());
  }
  private static native long Native_create(Kdu_push_pull_params _src);
  public Kdu_push_pull_params(Kdu_push_pull_params _src) {
    this(Native_create(_src));
  }
  public native int Get_spatial_xform_dbuf_rows() throws KduException;
  public native void Set_spatial_xform_dbuf_rows(int _buffer_rows) throws KduException;
  public native int Get_num_component_frags() throws KduException;
  public native void Set_num_component_frags(int _cf) throws KduException;
  public native Kdu_cplex_share Get_cplex_share() throws KduException;
  public native void Set_cplex_share(Kdu_cplex_share _ref) throws KduException;
  public native Kdu_cplex_bkgnd Get_cplex_bkgnd() throws KduException;
  public native void Set_cplex_bkgnd(Kdu_cplex_bkgnd _ref) throws KduException;
  public native void Set_preferred_job_samples(int _log2_min_samples, int _log2_ideal_samples) throws KduException;
  public native void Set_min_jobs_across(int _min_jobs_across) throws KduException;
  public native void Set_max_block_stripes(int _max_hires_stripes, boolean _extra_lowres_stripe) throws KduException;
  public native void Copy_params(Kdu_push_pull_params _src) throws KduException;
  public native void Get_preferred_job_samples(int[] _log2_min_samples, int[] _log2_ideal_samples) throws KduException;
  public native int Get_min_jobs_across() throws KduException;
  public native int Get_max_block_stripes(int[] _extra_lowres_stripes) throws KduException;
}
