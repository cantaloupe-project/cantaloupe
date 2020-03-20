package kdu_jni;

public class Kdu_cplex_bkgnd {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_cplex_bkgnd(long ptr) {
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
  public Kdu_cplex_bkgnd() {
    this(Native_create());
  }
  public native boolean Set_read_only() throws KduException;
  public native boolean Get_read_only() throws KduException;
  public native boolean Configure(int _num_comps, long _mct_hash) throws KduException;
  public native int Get_num_components(long[] _mct_hash) throws KduException;
  public native boolean Get_info(long[] _num_levels, long[] _incompatible_non_ll_subband_tally, long[] _incompatible_component_tally, long[] _incompatible_get_rel_stats_calls, long[] _store_new_stats_calls, long[] _incompatible_store_new_stats_calls) throws KduException;
}
