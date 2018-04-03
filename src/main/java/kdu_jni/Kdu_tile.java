package kdu_jni;

public class Kdu_tile {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_tile(long ptr) {
    _native_ptr = ptr;
  }
  public Kdu_tile() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native void Close(Kdu_thread_env _env, boolean _close_in_background) throws KduException;
  public void Close() throws KduException
  {
    Kdu_thread_env env = null;
    Close(env,(boolean) false);
  }
  public void Close(Kdu_thread_env _env) throws KduException
  {
    Close(_env,(boolean) false);
  }
  public native int Get_tnum() throws KduException;
  public native Kdu_coords Get_tile_idx() throws KduException;
  public native boolean Get_ycc() throws KduException;
  public native boolean Get_nlt_descriptors(int _num_comps, int[] _descriptors) throws KduException;
  public native boolean Make_nlt_table(int _comp_idx, boolean _for_analysis, float[] _dmin, float[] _dmax, int _num_entries, float[] _lut, float _nominal_range_in, float _nominal_range_out) throws KduException;
  public native void Set_components_of_interest(int _num_components_of_interest, int[] _components_of_interest) throws KduException;
  public void Set_components_of_interest() throws KduException
  {
    Set_components_of_interest((int) 0,null);
  }
  public void Set_components_of_interest(int _num_components_of_interest) throws KduException
  {
    Set_components_of_interest(_num_components_of_interest,null);
  }
  public native boolean Get_mct_block_info(int _stage_idx, int _block_idx, int[] _num_stage_inputs, int[] _num_stage_outputs, int[] _num_block_inputs, int[] _num_block_outputs, int[] _block_input_indices, int[] _block_output_indices, float[] _irrev_block_offsets, int[] _rev_block_offsets, int[] _stage_input_indices) throws KduException;
  public boolean Get_mct_block_info(int _stage_idx, int _block_idx, int[] _num_stage_inputs, int[] _num_stage_outputs, int[] _num_block_inputs, int[] _num_block_outputs) throws KduException
  {
    return Get_mct_block_info(_stage_idx,_block_idx,_num_stage_inputs,_num_stage_outputs,_num_block_inputs,_num_block_outputs,null,null,null,null,null);
  }
  public boolean Get_mct_block_info(int _stage_idx, int _block_idx, int[] _num_stage_inputs, int[] _num_stage_outputs, int[] _num_block_inputs, int[] _num_block_outputs, int[] _block_input_indices) throws KduException
  {
    return Get_mct_block_info(_stage_idx,_block_idx,_num_stage_inputs,_num_stage_outputs,_num_block_inputs,_num_block_outputs,_block_input_indices,null,null,null,null);
  }
  public boolean Get_mct_block_info(int _stage_idx, int _block_idx, int[] _num_stage_inputs, int[] _num_stage_outputs, int[] _num_block_inputs, int[] _num_block_outputs, int[] _block_input_indices, int[] _block_output_indices) throws KduException
  {
    return Get_mct_block_info(_stage_idx,_block_idx,_num_stage_inputs,_num_stage_outputs,_num_block_inputs,_num_block_outputs,_block_input_indices,_block_output_indices,null,null,null);
  }
  public boolean Get_mct_block_info(int _stage_idx, int _block_idx, int[] _num_stage_inputs, int[] _num_stage_outputs, int[] _num_block_inputs, int[] _num_block_outputs, int[] _block_input_indices, int[] _block_output_indices, float[] _irrev_block_offsets) throws KduException
  {
    return Get_mct_block_info(_stage_idx,_block_idx,_num_stage_inputs,_num_stage_outputs,_num_block_inputs,_num_block_outputs,_block_input_indices,_block_output_indices,_irrev_block_offsets,null,null);
  }
  public boolean Get_mct_block_info(int _stage_idx, int _block_idx, int[] _num_stage_inputs, int[] _num_stage_outputs, int[] _num_block_inputs, int[] _num_block_outputs, int[] _block_input_indices, int[] _block_output_indices, float[] _irrev_block_offsets, int[] _rev_block_offsets) throws KduException
  {
    return Get_mct_block_info(_stage_idx,_block_idx,_num_stage_inputs,_num_stage_outputs,_num_block_inputs,_num_block_outputs,_block_input_indices,_block_output_indices,_irrev_block_offsets,_rev_block_offsets,null);
  }
  public native boolean Get_mct_matrix_info(int _stage_idx, int _block_idx, float[] _coefficients) throws KduException;
  public boolean Get_mct_matrix_info(int _stage_idx, int _block_idx) throws KduException
  {
    return Get_mct_matrix_info(_stage_idx,_block_idx,null);
  }
  public native boolean Get_mct_rxform_info(int _stage_idx, int _block_idx, int[] _coefficients, int[] _active_outputs) throws KduException;
  public boolean Get_mct_rxform_info(int _stage_idx, int _block_idx) throws KduException
  {
    return Get_mct_rxform_info(_stage_idx,_block_idx,null,null);
  }
  public boolean Get_mct_rxform_info(int _stage_idx, int _block_idx, int[] _coefficients) throws KduException
  {
    return Get_mct_rxform_info(_stage_idx,_block_idx,_coefficients,null);
  }
  public native boolean Get_mct_dependency_info(int _stage_idx, int _block_idx, boolean[] _is_reversible, float[] _irrev_coefficients, float[] _irrev_offsets, int[] _rev_coefficients, int[] _rev_offsets, int[] _active_outputs) throws KduException;
  public boolean Get_mct_dependency_info(int _stage_idx, int _block_idx, boolean[] _is_reversible) throws KduException
  {
    return Get_mct_dependency_info(_stage_idx,_block_idx,_is_reversible,null,null,null,null,null);
  }
  public boolean Get_mct_dependency_info(int _stage_idx, int _block_idx, boolean[] _is_reversible, float[] _irrev_coefficients) throws KduException
  {
    return Get_mct_dependency_info(_stage_idx,_block_idx,_is_reversible,_irrev_coefficients,null,null,null,null);
  }
  public boolean Get_mct_dependency_info(int _stage_idx, int _block_idx, boolean[] _is_reversible, float[] _irrev_coefficients, float[] _irrev_offsets) throws KduException
  {
    return Get_mct_dependency_info(_stage_idx,_block_idx,_is_reversible,_irrev_coefficients,_irrev_offsets,null,null,null);
  }
  public boolean Get_mct_dependency_info(int _stage_idx, int _block_idx, boolean[] _is_reversible, float[] _irrev_coefficients, float[] _irrev_offsets, int[] _rev_coefficients) throws KduException
  {
    return Get_mct_dependency_info(_stage_idx,_block_idx,_is_reversible,_irrev_coefficients,_irrev_offsets,_rev_coefficients,null,null);
  }
  public boolean Get_mct_dependency_info(int _stage_idx, int _block_idx, boolean[] _is_reversible, float[] _irrev_coefficients, float[] _irrev_offsets, int[] _rev_coefficients, int[] _rev_offsets) throws KduException
  {
    return Get_mct_dependency_info(_stage_idx,_block_idx,_is_reversible,_irrev_coefficients,_irrev_offsets,_rev_coefficients,_rev_offsets,null);
  }
  public native int Get_num_components() throws KduException;
  public native int Get_num_layers() throws KduException;
  public native boolean Parse_all_relevant_packets(boolean _start_from_scratch_if_possible, Kdu_thread_env _env) throws KduException;
  public native long Get_parsed_packet_stats(int _component_idx, int _discard_levels, int _num_layers, long[] _layer_bytes, long[] _layer_packets) throws KduException;
  public long Get_parsed_packet_stats(int _component_idx, int _discard_levels, int _num_layers, long[] _layer_bytes) throws KduException
  {
    return Get_parsed_packet_stats(_component_idx,_discard_levels,_num_layers,_layer_bytes,null);
  }
  public native Kdu_tile_comp Access_component(int _component_idx) throws KduException;
  public native float Find_component_gain_info(int _comp_idx, boolean _restrict_to_interest) throws KduException;
}
