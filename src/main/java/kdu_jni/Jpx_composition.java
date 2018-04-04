package kdu_jni;

public class Jpx_composition {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jpx_composition(long ptr) {
    _native_ptr = ptr;
  }
  public Jpx_composition() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native void Copy(Jpx_composition _src) throws KduException;
  public native int Get_global_info(Kdu_coords _size) throws KduException;
  public native long Get_track_idx() throws KduException;
  public native long Get_timescale() throws KduException;
  public native boolean Count_tracks(long[] _count, boolean _global_only) throws KduException;
  public boolean Count_tracks(long[] _count) throws KduException
  {
    return Count_tracks(_count,(boolean) false);
  }
  public native boolean Count_track_frames(long _track_idx, int[] _count) throws KduException;
  public native boolean Count_track_time(long _track_idx, long[] _count) throws KduException;
  public native boolean Count_track_frames_before_time(long _track_idx, long _max_end_time, int[] _count) throws KduException;
  public native Jpx_frame Access_frame(long _track_idx, int _frame_idx, boolean _must_exist, boolean _include_persistents) throws KduException;
  public Jpx_frame Access_frame(long _track_idx, int _frame_idx, boolean _must_exist) throws KduException
  {
    return Access_frame(_track_idx,_frame_idx,_must_exist,(boolean) true);
  }
  public native int Find_layer_match(Jpx_frame _frame, int[] _inst_idx, long _track_idx, int[] _layers, int _num_layers, int _container_id, boolean _include_persistents, int _flags) throws KduException;
  public int Find_layer_match(Jpx_frame _frame, int[] _inst_idx, long _track_idx, int[] _layers, int _num_layers) throws KduException
  {
    return Find_layer_match(_frame,_inst_idx,_track_idx,_layers,_num_layers,(int) -1,(boolean) true,(int) 0);
  }
  public int Find_layer_match(Jpx_frame _frame, int[] _inst_idx, long _track_idx, int[] _layers, int _num_layers, int _container_id) throws KduException
  {
    return Find_layer_match(_frame,_inst_idx,_track_idx,_layers,_num_layers,_container_id,(boolean) true,(int) 0);
  }
  public int Find_layer_match(Jpx_frame _frame, int[] _inst_idx, long _track_idx, int[] _layers, int _num_layers, int _container_id, boolean _include_persistents) throws KduException
  {
    return Find_layer_match(_frame,_inst_idx,_track_idx,_layers,_num_layers,_container_id,_include_persistents,(int) 0);
  }
  public native int Find_numlist_match(Jpx_frame _frame, int[] _inst_idx, long _track_idx, Jpx_metanode _numlist, int _max_inferred_layers, boolean _include_persistents, int _flags) throws KduException;
  public int Find_numlist_match(Jpx_frame _frame, int[] _inst_idx, long _track_idx, Jpx_metanode _numlist) throws KduException
  {
    return Find_numlist_match(_frame,_inst_idx,_track_idx,_numlist,(int) 0,(boolean) true,(int) 0);
  }
  public int Find_numlist_match(Jpx_frame _frame, int[] _inst_idx, long _track_idx, Jpx_metanode _numlist, int _max_inferred_layers) throws KduException
  {
    return Find_numlist_match(_frame,_inst_idx,_track_idx,_numlist,_max_inferred_layers,(boolean) true,(int) 0);
  }
  public int Find_numlist_match(Jpx_frame _frame, int[] _inst_idx, long _track_idx, Jpx_metanode _numlist, int _max_inferred_layers, boolean _include_persistents) throws KduException
  {
    return Find_numlist_match(_frame,_inst_idx,_track_idx,_numlist,_max_inferred_layers,_include_persistents,(int) 0);
  }
  public native long Get_next_frame(long _last_frame) throws KduException;
  public native long Get_prev_frame(long _last_frame) throws KduException;
  public native Jpx_composition Access_owner(long _frame_ref) throws KduException;
  public native Jpx_frame Get_interface_for_frame(long _frame, int _iteration_idx, boolean _include_persistents) throws KduException;
  public Jpx_frame Get_interface_for_frame(long _frame, int _iteration_idx) throws KduException
  {
    return Get_interface_for_frame(_frame,_iteration_idx,(boolean) true);
  }
  public native void Get_frame_info(long _frame_ref, int[] _num_instructions, int[] _duration, int[] _repeat_count, boolean[] _is_persistent) throws KduException;
  public native long Get_last_persistent_frame(long _frame_ref) throws KduException;
  public native boolean Get_instruction(long _frame_ref, int _instruction_idx, int[] _rel_layer_idx, int[] _rel_increment, boolean[] _is_reused, Kdu_dims _source_dims, Kdu_dims _target_dims, Jpx_composited_orientation _orientation) throws KduException;
  public native boolean Get_original_iset(long _frame_ref, int _instruction_idx, int[] _iset_idx, int[] _inum_idx) throws KduException;
  public native int Map_rel_layer_idx(int _rel_layer_idx) throws KduException;
  public native long Add_frame(int _duration, int _repeat_count, boolean _is_persistent) throws KduException;
  public native int Add_instruction(long _frame_ref, int _rel_layer_idx, int _rel_increment, Kdu_dims _source_dims, Kdu_dims _target_dims, Jpx_composited_orientation _orient) throws KduException;
  public native void Set_loop_count(int _count) throws KduException;
}
