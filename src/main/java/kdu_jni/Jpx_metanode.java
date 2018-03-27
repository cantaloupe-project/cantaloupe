package kdu_jni;

public class Jpx_metanode {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jpx_metanode(long ptr) {
    _native_ptr = ptr;
  }
  public Jpx_metanode() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native boolean Get_numlist_info(int[] _num_codestreams, int[] _num_layers, boolean[] _applies_to_rendered_result) throws KduException;
  public native int Get_container_id() throws KduException;
  public native int Get_container_lmap(int[] _base, int[] _span) throws KduException;
  public int Get_container_lmap() throws KduException
  {
    return Get_container_lmap(null,null);
  }
  public int Get_container_lmap(int[] _base) throws KduException
  {
    return Get_container_lmap(_base,null);
  }
  public native int Get_container_cmap(int[] _base, int[] _span) throws KduException;
  public int Get_container_cmap() throws KduException
  {
    return Get_container_cmap(null,null);
  }
  public int Get_container_cmap(int[] _base) throws KduException
  {
    return Get_container_cmap(_base,null);
  }
  public native int Get_container_codestream_rep(int _stream_idx) throws KduException;
  public native int Get_container_layer_rep(int _layer_idx) throws KduException;
  public native long Get_numlist_codestreams() throws KduException;
  public native long Get_numlist_layers() throws KduException;
  public native boolean Count_numlist_codestreams(int[] _count) throws KduException;
  public native boolean Count_numlist_layers(int[] _count) throws KduException;
  public native int Get_numlist_codestream(int _which, int _rep_idx) throws KduException;
  public int Get_numlist_codestream(int _which) throws KduException
  {
    return Get_numlist_codestream(_which,(int) 0);
  }
  public native int Get_numlist_layer(int _which, int _rep_idx) throws KduException;
  public int Get_numlist_layer(int _which) throws KduException
  {
    return Get_numlist_layer(_which,(int) 0);
  }
  public native int Find_numlist_codestream(int _stream_idx) throws KduException;
  public native int Find_numlist_layer(int _layer_idx) throws KduException;
  public native boolean Test_numlist_stream(int _codestream_idx) throws KduException;
  public native boolean Test_numlist_layer(int _layer_idx) throws KduException;
  public native Jpx_metanode Find_next_identical_numlist() throws KduException;
  public native Jpx_metanode Find_first_identical_numlist() throws KduException;
  public native Jpx_metanode Get_numlist_container() throws KduException;
  public native int Compare_numlists(Jpx_metanode _rhs) throws KduException;
  public native boolean Count_numlist_descendants(int[] _count) throws KduException;
  public native int Get_num_regions() throws KduException;
  public native Jpx_roi Get_region(int _which) throws KduException;
  public native int Get_width() throws KduException;
  public native Kdu_dims Get_bounding_box() throws KduException;
  public native boolean Test_region(Kdu_dims _region) throws KduException;
  public native boolean Has_dependent_roi_nodes() throws KduException;
  public native long Get_box_type() throws KduException;
  public native String Get_label() throws KduException;
  public native boolean Get_uuid(byte[] _uuid) throws KduException;
  public native boolean Is_xmp_uuid() throws KduException;
  public native boolean Is_iptc_uuid() throws KduException;
  public native boolean Is_geojp2_uuid() throws KduException;
  public native long Get_cross_reference(Jpx_fragment_list _frags) throws KduException;
  public native Jpx_metanode Get_link(int[] _link_type, boolean _try_to_resolve) throws KduException;
  public Jpx_metanode Get_link(int[] _link_type) throws KduException
  {
    return Get_link(_link_type,(boolean) false);
  }
  public native Jpx_metanode Enum_linkers(Jpx_metanode _last_linker) throws KduException;
  public native boolean Open_existing(Jp2_input_box _box) throws KduException;
  public native boolean Count_descendants(int[] _count) throws KduException;
  public native Jpx_metanode Get_descendant(int _which) throws KduException;
  public native Jpx_metanode Find_descendant_by_type(int _which, int _num_box_types_of_interest, long[] _box_types_of_interest) throws KduException;
  public native boolean Check_descendants_complete(int _num_box_types_of_interest, long[] _box_types_of_interest) throws KduException;
  public native Jpx_metanode Get_next_descendant(Jpx_metanode _ref, int _limit_cmd, long[] _box_types) throws KduException;
  public Jpx_metanode Get_next_descendant(Jpx_metanode _ref) throws KduException
  {
    return Get_next_descendant(_ref,(int) -1,null);
  }
  public Jpx_metanode Get_next_descendant(Jpx_metanode _ref, int _limit_cmd) throws KduException
  {
    return Get_next_descendant(_ref,_limit_cmd,null);
  }
  public native Jpx_metanode Get_prev_descendant(Jpx_metanode _ref, int _limit_cmd, long[] _box_types) throws KduException;
  public Jpx_metanode Get_prev_descendant(Jpx_metanode _ref) throws KduException
  {
    return Get_prev_descendant(_ref,(int) -1,null);
  }
  public Jpx_metanode Get_prev_descendant(Jpx_metanode _ref, int _limit_cmd) throws KduException
  {
    return Get_prev_descendant(_ref,_limit_cmd,null);
  }
  public native long Get_sequence_index() throws KduException;
  public native Jpx_metanode Get_parent() throws KduException;
  public native Jpx_metanode Find_path_to(Jpx_metanode _target, int _descending_flags, int _ascending_flags, int _num_exclusion_categories, long[] _exclusion_box_types, int[] _exclusion_flags, boolean _unify_groups) throws KduException;
  public Jpx_metanode Find_path_to(Jpx_metanode _target, int _descending_flags, int _ascending_flags, int _num_exclusion_categories, long[] _exclusion_box_types, int[] _exclusion_flags) throws KduException
  {
    return Find_path_to(_target,_descending_flags,_ascending_flags,_num_exclusion_categories,_exclusion_box_types,_exclusion_flags,(boolean) false);
  }
  public native boolean Change_parent(Jpx_metanode _new_parent) throws KduException;
  public native Jpx_metanode Add_numlist(int _num_codestreams, int[] _codestream_indices, int _num_compositing_layers, int[] _layer_indices, boolean _applies_to_rendered_result, int _container_id) throws KduException;
  public Jpx_metanode Add_numlist(int _num_codestreams, int[] _codestream_indices, int _num_compositing_layers, int[] _layer_indices, boolean _applies_to_rendered_result) throws KduException
  {
    return Add_numlist(_num_codestreams,_codestream_indices,_num_compositing_layers,_layer_indices,_applies_to_rendered_result,(int) -1);
  }
  public native Jpx_metanode Add_regions(int _num_regions, Jpx_roi _regions) throws KduException;
  public native Jpx_metanode Add_label(String _text) throws KduException;
  public native void Change_to_label(String _text) throws KduException;
  public native Jpx_metanode Add_delayed(long _box_type, int _i_param) throws KduException;
  public native void Change_to_delayed(long _box_type, int _i_param) throws KduException;
  public native Jpx_metanode Add_link(Jpx_metanode _target, int _link_type, boolean _avoid_duplicates) throws KduException;
  public Jpx_metanode Add_link(Jpx_metanode _target, int _link_type) throws KduException
  {
    return Add_link(_target,_link_type,(boolean) true);
  }
  public native void Change_to_link(Jpx_metanode _target, int _link_type) throws KduException;
  public native void Preserve_for_links() throws KduException;
  public native Jpx_metanode Add_copy(Jpx_metanode _src, boolean _recursive, boolean _link_to_internal_copies) throws KduException;
  public Jpx_metanode Add_copy(Jpx_metanode _src, boolean _recursive) throws KduException
  {
    return Add_copy(_src,_recursive,(boolean) false);
  }
  public native void Delete_node() throws KduException;
  public native boolean Is_changed() throws KduException;
  public native boolean Ancestor_changed() throws KduException;
  public native boolean Is_deleted() throws KduException;
  public native boolean Child_removed() throws KduException;
  public native void Touch() throws KduException;
  public native long Get_state_ref() throws KduException;
  public native int Generate_metareq(Kdu_window _client_window, int _num_box_types_of_interest, long[] _box_types_of_interest, int _num_descend_box_types, long[] _descend_box_types, boolean _priority, int _max_descend_depth, int _qualifier) throws KduException;
  public native int Generate_link_metareq(Kdu_window _client_window, int _num_box_types_of_interest, long[] _box_types_of_interest, int _num_descend_box_types, long[] _descend_box_types, boolean _priority, int _max_descend_depth, int _qualifier) throws KduException;
}
