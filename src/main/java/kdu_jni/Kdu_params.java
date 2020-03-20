package kdu_jni;

public class Kdu_params {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_params(long ptr) {
    _native_ptr = ptr;
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create(String _cluster_name, boolean _allow_tile_diversity, boolean _allow_component_diversity, boolean _allow_instance_diversity, boolean _force_component_specific_forms, boolean _treat_instances_like_components);
  public Kdu_params(String _cluster_name, boolean _allow_tile_diversity, boolean _allow_component_diversity, boolean _allow_instance_diversity, boolean _force_component_specific_forms, boolean _treat_instances_like_components) {
    this(Native_create(_cluster_name, _allow_tile_diversity, _allow_component_diversity, _allow_instance_diversity, _force_component_specific_forms, _treat_instances_like_components));
  }
  public native Kdu_params Link(Kdu_params _existing, int _tile_idx, int _comp_idx, int _num_tiles, int _num_comps) throws KduException;
  public native Kdu_params New_instance() throws KduException;
  public native void Copy_from(Kdu_params _source, int _source_tile, int _target_tile, int _instance, int _skip_components, int _discard_levels, boolean _transpose, boolean _vflip, boolean _hflip) throws KduException;
  public void Copy_from(Kdu_params _source, int _source_tile, int _target_tile) throws KduException
  {
    Copy_from(_source,_source_tile,_target_tile,(int) -1,(int) 0,(int) 0,(boolean) false,(boolean) false,(boolean) false);
  }
  public void Copy_from(Kdu_params _source, int _source_tile, int _target_tile, int _instance) throws KduException
  {
    Copy_from(_source,_source_tile,_target_tile,_instance,(int) 0,(int) 0,(boolean) false,(boolean) false,(boolean) false);
  }
  public void Copy_from(Kdu_params _source, int _source_tile, int _target_tile, int _instance, int _skip_components) throws KduException
  {
    Copy_from(_source,_source_tile,_target_tile,_instance,_skip_components,(int) 0,(boolean) false,(boolean) false,(boolean) false);
  }
  public void Copy_from(Kdu_params _source, int _source_tile, int _target_tile, int _instance, int _skip_components, int _discard_levels) throws KduException
  {
    Copy_from(_source,_source_tile,_target_tile,_instance,_skip_components,_discard_levels,(boolean) false,(boolean) false,(boolean) false);
  }
  public void Copy_from(Kdu_params _source, int _source_tile, int _target_tile, int _instance, int _skip_components, int _discard_levels, boolean _transpose) throws KduException
  {
    Copy_from(_source,_source_tile,_target_tile,_instance,_skip_components,_discard_levels,_transpose,(boolean) false,(boolean) false);
  }
  public void Copy_from(Kdu_params _source, int _source_tile, int _target_tile, int _instance, int _skip_components, int _discard_levels, boolean _transpose, boolean _vflip) throws KduException
  {
    Copy_from(_source,_source_tile,_target_tile,_instance,_skip_components,_discard_levels,_transpose,_vflip,(boolean) false);
  }
  public native void Copy_all(Kdu_params _source, int _skip_components, int _discard_levels, boolean _transpose, boolean _vflip, boolean _hflip) throws KduException;
  public void Copy_all(Kdu_params _source) throws KduException
  {
    Copy_all(_source,(int) 0,(int) 0,(boolean) false,(boolean) false,(boolean) false);
  }
  public void Copy_all(Kdu_params _source, int _skip_components) throws KduException
  {
    Copy_all(_source,_skip_components,(int) 0,(boolean) false,(boolean) false,(boolean) false);
  }
  public void Copy_all(Kdu_params _source, int _skip_components, int _discard_levels) throws KduException
  {
    Copy_all(_source,_skip_components,_discard_levels,(boolean) false,(boolean) false,(boolean) false);
  }
  public void Copy_all(Kdu_params _source, int _skip_components, int _discard_levels, boolean _transpose) throws KduException
  {
    Copy_all(_source,_skip_components,_discard_levels,_transpose,(boolean) false,(boolean) false);
  }
  public void Copy_all(Kdu_params _source, int _skip_components, int _discard_levels, boolean _transpose, boolean _vflip) throws KduException
  {
    Copy_all(_source,_skip_components,_discard_levels,_transpose,_vflip,(boolean) false);
  }
  public native String Identify_cluster() throws KduException;
  public native Kdu_params Access_cluster(String _cluster_name) throws KduException;
  public native Kdu_params Access_cluster(int _sequence_idx) throws KduException;
  public native int Get_instance() throws KduException;
  public native int Get_num_comps() throws KduException;
  public native int Get_num_tiles() throws KduException;
  public native Kdu_params Access_relation(int _tile_idx, int _comp_idx, int _inst_idx, boolean _read_only) throws KduException;
  public Kdu_params Access_relation(int _tile_idx, int _comp_idx) throws KduException
  {
    return Access_relation(_tile_idx,_comp_idx,(int) 0,(boolean) false);
  }
  public Kdu_params Access_relation(int _tile_idx, int _comp_idx, int _inst_idx) throws KduException
  {
    return Access_relation(_tile_idx,_comp_idx,_inst_idx,(boolean) false);
  }
  public native Kdu_params Access_unique(int _tile_idx, int _comp_idx, int _inst_idx) throws KduException;
  public Kdu_params Access_unique(int _tile_idx, int _comp_idx) throws KduException
  {
    return Access_unique(_tile_idx,_comp_idx,(int) 0);
  }
  public native Kdu_params Access_next_inst() throws KduException;
  public native void Clear_marks(boolean _for_reading) throws KduException;
  public native boolean Any_changes() throws KduException;
  public native boolean Check_typical_tile(int _tile_idx, String _excluded_clusters) throws KduException;
  public boolean Check_typical_tile(int _tile_idx) throws KduException
  {
    return Check_typical_tile(_tile_idx,null);
  }
  public native int Translate_marker_segment(int _code, int _num_bytes, byte[] _bytes, int _which_tile, int _tpart_idx) throws KduException;
  public native int Generate_marker_segments(Kdu_output _out, int _which_tile, int _tpart_idx) throws KduException;
  public native int Generate_marker_segments(Kdu_output _out, int _which_tile, int _tpart_idx, int[] _class_flags) throws KduException;
  public native boolean Get(String _name, int _record_idx, int _field_idx, int[] _value, boolean _allow_inherit, boolean _allow_extend, boolean _allow_derived) throws KduException;
  public boolean Get(String _name, int _record_idx, int _field_idx, int[] _value) throws KduException
  {
    return Get(_name,_record_idx,_field_idx,_value,(boolean) true,(boolean) true,(boolean) true);
  }
  public boolean Get(String _name, int _record_idx, int _field_idx, int[] _value, boolean _allow_inherit) throws KduException
  {
    return Get(_name,_record_idx,_field_idx,_value,_allow_inherit,(boolean) true,(boolean) true);
  }
  public boolean Get(String _name, int _record_idx, int _field_idx, int[] _value, boolean _allow_inherit, boolean _allow_extend) throws KduException
  {
    return Get(_name,_record_idx,_field_idx,_value,_allow_inherit,_allow_extend,(boolean) true);
  }
  public native boolean Get(String _name, int _record_idx, int _field_idx, boolean[] _value, boolean _allow_inherit, boolean _allow_extend, boolean _allow_derived) throws KduException;
  public boolean Get(String _name, int _record_idx, int _field_idx, boolean[] _value) throws KduException
  {
    return Get(_name,_record_idx,_field_idx,_value,(boolean) true,(boolean) true,(boolean) true);
  }
  public boolean Get(String _name, int _record_idx, int _field_idx, boolean[] _value, boolean _allow_inherit) throws KduException
  {
    return Get(_name,_record_idx,_field_idx,_value,_allow_inherit,(boolean) true,(boolean) true);
  }
  public boolean Get(String _name, int _record_idx, int _field_idx, boolean[] _value, boolean _allow_inherit, boolean _allow_extend) throws KduException
  {
    return Get(_name,_record_idx,_field_idx,_value,_allow_inherit,_allow_extend,(boolean) true);
  }
  public native boolean Get(String _name, int _record_idx, int _field_idx, float[] _value, boolean _allow_inherit, boolean _allow_extend, boolean _allow_derived) throws KduException;
  public boolean Get(String _name, int _record_idx, int _field_idx, float[] _value) throws KduException
  {
    return Get(_name,_record_idx,_field_idx,_value,(boolean) true,(boolean) true,(boolean) true);
  }
  public boolean Get(String _name, int _record_idx, int _field_idx, float[] _value, boolean _allow_inherit) throws KduException
  {
    return Get(_name,_record_idx,_field_idx,_value,_allow_inherit,(boolean) true,(boolean) true);
  }
  public boolean Get(String _name, int _record_idx, int _field_idx, float[] _value, boolean _allow_inherit, boolean _allow_extend) throws KduException
  {
    return Get(_name,_record_idx,_field_idx,_value,_allow_inherit,_allow_extend,(boolean) true);
  }
  public native int Count_records(String _name, boolean _allow_inherit, boolean _allow_derived) throws KduException;
  public int Count_records(String _name) throws KduException
  {
    return Count_records(_name,(boolean) true,(boolean) true);
  }
  public int Count_records(String _name, boolean _allow_inherit) throws KduException
  {
    return Count_records(_name,_allow_inherit,(boolean) true);
  }
  public native boolean Compare(String _name, int _record_idx, int _field_idx, int _value) throws KduException;
  public native boolean Compare(String _name, int _record_idx, int _field_idx, boolean _value) throws KduException;
  public native boolean Compare(String _name, int _record_idx, int _field_idx, float _value) throws KduException;
  public native void Set(String _name, int _record_idx, int _field_idx, int _value) throws KduException;
  public native void Set(String _name, int _record_idx, int _field_idx, boolean _value) throws KduException;
  public native void Set(String _name, int _record_idx, int _field_idx, double _value, boolean _preserve_derived) throws KduException;
  public void Set(String _name, int _record_idx, int _field_idx, double _value) throws KduException
  {
    Set(_name,_record_idx,_field_idx,_value,(boolean) false);
  }
  public native void Set_derived(String _name) throws KduException;
  public native boolean Parse_string(String _string) throws KduException;
  public native boolean Parse_string(String _string, int _tile_idx) throws KduException;
  public native void Textualize_attributes(Kdu_message _output, boolean _skip_derived) throws KduException;
  public void Textualize_attributes(Kdu_message _output) throws KduException
  {
    Textualize_attributes(_output,(boolean) true);
  }
  public native void Textualize_attributes(Kdu_message _output, int _min_tile, int _max_tile, boolean _skip_derived) throws KduException;
  public void Textualize_attributes(Kdu_message _output, int _min_tile, int _max_tile) throws KduException
  {
    Textualize_attributes(_output,_min_tile,_max_tile,(boolean) true);
  }
  public native void Describe_attributes(Kdu_message _output, boolean _include_comments) throws KduException;
  public void Describe_attributes(Kdu_message _output) throws KduException
  {
    Describe_attributes(_output,(boolean) true);
  }
  public native void Describe_attribute(String _name, Kdu_message _output, boolean _include_comments) throws KduException;
  public void Describe_attribute(String _name, Kdu_message _output) throws KduException
  {
    Describe_attribute(_name,_output,(boolean) true);
  }
  public native void Delete_unparsed_attribute(String _name) throws KduException;
  public native boolean Truncate_records(String _name, int _max_records) throws KduException;
  public native int Custom_parse_field(String _string, String _name, int _field_idx, int[] _val) throws KduException;
  public native void Custom_textualize_field(Kdu_message _output, String _name, int _field_idx, int _val) throws KduException;
  public native void Finalize(boolean _after_reading) throws KduException;
  public void Finalize() throws KduException
  {
    Finalize((boolean) false);
  }
  public native void Finalize_all(boolean _after_reading) throws KduException;
  public void Finalize_all() throws KduException
  {
    Finalize_all((boolean) false);
  }
  public native void Finalize_all(int _tile_idx, boolean _after_reading) throws KduException;
  public void Finalize_all(int _tile_idx) throws KduException
  {
    Finalize_all(_tile_idx,(boolean) false);
  }
}
