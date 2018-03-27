package kdu_jni;

public class Jpx_frame_expander {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jpx_frame_expander(long ptr) {
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
  public Jpx_frame_expander() {
    this(Native_create());
  }
  public native void Reset() throws KduException;
  public native boolean Construct(Jpx_source _source, Jpx_frame _frame, Kdu_dims _region_of_interest) throws KduException;
  public native boolean Construct(Jpx_source _source, long _frame_ref, int _iteration_idx, boolean _follow_persistence, Kdu_dims _region_of_interest) throws KduException;
  public native int Test_codestream_visibility(Jpx_source _source, Jpx_frame _frame, int _codestream_idx, Jpx_metanode _numlist, int[] _layer_indices, int _num_layers, Kdu_dims _composition_region, Kdu_dims _codestream_roi, boolean _ignore_use_in_alpha, int _initial_matches_to_skip) throws KduException;
  public int Test_codestream_visibility(Jpx_source _source, Jpx_frame _frame, int _codestream_idx, Jpx_metanode _numlist, int[] _layer_indices, int _num_layers, Kdu_dims _composition_region, Kdu_dims _codestream_roi) throws KduException
  {
    return Test_codestream_visibility(_source,_frame,_codestream_idx,_numlist,_layer_indices,_num_layers,_composition_region,_codestream_roi,(boolean) true,(int) 0);
  }
  public int Test_codestream_visibility(Jpx_source _source, Jpx_frame _frame, int _codestream_idx, Jpx_metanode _numlist, int[] _layer_indices, int _num_layers, Kdu_dims _composition_region, Kdu_dims _codestream_roi, boolean _ignore_use_in_alpha) throws KduException
  {
    return Test_codestream_visibility(_source,_frame,_codestream_idx,_numlist,_layer_indices,_num_layers,_composition_region,_codestream_roi,_ignore_use_in_alpha,(int) 0);
  }
  public native int Test_codestream_visibility(Jpx_source _source, long _frame, int _iteration_idx, boolean _follow_persistence, int _codestream_idx, int[] _layer_indices, int _num_layers, Kdu_dims _composition_region, Kdu_dims _codestream_roi, boolean _ignore_use_in_alpha, int _initial_matches_to_skip) throws KduException;
  public int Test_codestream_visibility(Jpx_source _source, long _frame, int _iteration_idx, boolean _follow_persistence, int _codestream_idx, int[] _layer_indices, int _num_layers, Kdu_dims _composition_region, Kdu_dims _codestream_roi) throws KduException
  {
    return Test_codestream_visibility(_source,_frame,_iteration_idx,_follow_persistence,_codestream_idx,_layer_indices,_num_layers,_composition_region,_codestream_roi,(boolean) true,(int) 0);
  }
  public int Test_codestream_visibility(Jpx_source _source, long _frame, int _iteration_idx, boolean _follow_persistence, int _codestream_idx, int[] _layer_indices, int _num_layers, Kdu_dims _composition_region, Kdu_dims _codestream_roi, boolean _ignore_use_in_alpha) throws KduException
  {
    return Test_codestream_visibility(_source,_frame,_iteration_idx,_follow_persistence,_codestream_idx,_layer_indices,_num_layers,_composition_region,_codestream_roi,_ignore_use_in_alpha,(int) 0);
  }
  public native boolean Has_non_covering_members() throws KduException;
  public native int Get_num_members() throws KduException;
  public native int Get_member(int _which, int[] _layer_idx, boolean[] _covers_composition, Kdu_dims _source_dims, Kdu_dims _target_dims, Jpx_composited_orientation _orientation) throws KduException;
  public native long Get_member(int _which, int[] _instruction_idx, int[] _layer_idx, boolean[] _covers_composition, Kdu_dims _source_dims, Kdu_dims _target_dims, Jpx_composited_orientation _orientation) throws KduException;
}
