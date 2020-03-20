package kdu_jni;

public class Jp2_channels {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected long _native_param = 0;
  protected Jp2_channels(long ptr, long param) {
    _native_ptr = ptr;
    _native_param = param;
  }
  public Jp2_channels() {
      this(0,0);
  }
  public native boolean Exists() throws KduException;
  public native void Copy(Jp2_channels _src, int[] _stream_map, int _stream_map_elts, int _stream_base) throws KduException;
  public void Copy(Jp2_channels _src) throws KduException
  {
    Copy(_src,null,(int) 0,(int) 0);
  }
  public void Copy(Jp2_channels _src, int[] _stream_map) throws KduException
  {
    Copy(_src,_stream_map,(int) 0,(int) 0);
  }
  public void Copy(Jp2_channels _src, int[] _stream_map, int _stream_map_elts) throws KduException
  {
    Copy(_src,_stream_map,_stream_map_elts,(int) 0);
  }
  public native void Init(int _num_colours, int _num_non_colours) throws KduException;
  public void Init(int _num_colours) throws KduException
  {
    Init(_num_colours,(int) 0);
  }
  public native boolean Set_colour_mapping(int _colour_idx, int _codestream_component, int _lut_idx, int _codestream_idx, int _data_format, int[] _format_params) throws KduException;
  public boolean Set_colour_mapping(int _colour_idx, int _codestream_component, int _lut_idx, int _codestream_idx, int _data_format) throws KduException
  {
    return Set_colour_mapping(_colour_idx,_codestream_component,_lut_idx,_codestream_idx,_data_format,null);
  }
  public native boolean Set_non_colour_mapping(int _idx, int _asoc_key, int _codestream_component, int _lut_idx, int _codestream_idx, int _data_format, int[] _format_params) throws KduException;
  public boolean Set_non_colour_mapping(int _idx, int _asoc_key, int _codestream_component, int _lut_idx, int _codestream_idx, int _data_format) throws KduException
  {
    return Set_non_colour_mapping(_idx,_asoc_key,_codestream_component,_lut_idx,_codestream_idx,_data_format,null);
  }
  public native boolean Set_opacity_mapping(int _colour_idx, int _codestream_component, int _lut_idx, int _codestream_idx, int _data_format, int[] _format_params) throws KduException;
  public boolean Set_opacity_mapping(int _colour_idx, int _codestream_component, int _lut_idx, int _codestream_idx, int _data_format) throws KduException
  {
    return Set_opacity_mapping(_colour_idx,_codestream_component,_lut_idx,_codestream_idx,_data_format,null);
  }
  public native boolean Set_premult_mapping(int _colour_idx, int _codestream_component, int _lut_idx, int _codestream_idx, int _data_format, int[] _format_params) throws KduException;
  public boolean Set_premult_mapping(int _colour_idx, int _codestream_component, int _lut_idx, int _codestream_idx, int _data_format) throws KduException
  {
    return Set_premult_mapping(_colour_idx,_codestream_component,_lut_idx,_codestream_idx,_data_format,null);
  }
  public native boolean Set_chroma_key(int _colour_idx, int _key_val) throws KduException;
  public native int Get_num_colours() throws KduException;
  public native int Get_num_non_colours() throws KduException;
  public native int Get_codestreams_used(int[] _min_stream_idx, int[] _max_stream_idx, int[] _stream_indices, int _max_indices) throws KduException;
  public int Get_codestreams_used(int[] _min_stream_idx, int[] _max_stream_idx) throws KduException
  {
    return Get_codestreams_used(_min_stream_idx,_max_stream_idx,null,(int) 0);
  }
  public int Get_codestreams_used(int[] _min_stream_idx, int[] _max_stream_idx, int[] _stream_indices) throws KduException
  {
    return Get_codestreams_used(_min_stream_idx,_max_stream_idx,_stream_indices,(int) 0);
  }
  public native boolean Get_colour_mapping(int _colour_idx, int[] _codestream_component, int[] _lut_idx, int[] _codestream_idx, int[] _data_format, int[] _format_params) throws KduException;
  public boolean Get_colour_mapping(int _colour_idx, int[] _codestream_component, int[] _lut_idx, int[] _codestream_idx, int[] _data_format) throws KduException
  {
    return Get_colour_mapping(_colour_idx,_codestream_component,_lut_idx,_codestream_idx,_data_format,null);
  }
  public native boolean Get_non_colour_mapping(int _idx, int[] _asoc_key, int[] _codestream_component, int[] _lut_idx, int[] _codestream_idx, int[] _data_format, int[] _format_params) throws KduException;
  public boolean Get_non_colour_mapping(int _idx, int[] _asoc_key, int[] _codestream_component, int[] _lut_idx, int[] _codestream_idx, int[] _data_format) throws KduException
  {
    return Get_non_colour_mapping(_idx,_asoc_key,_codestream_component,_lut_idx,_codestream_idx,_data_format,null);
  }
  public native boolean Get_opacity_mapping(int _colour_idx, int[] _codestream_component, int[] _lut_idx, int[] _codestream_idx, int[] _data_format, int[] _format_params) throws KduException;
  public boolean Get_opacity_mapping(int _colour_idx, int[] _codestream_component, int[] _lut_idx, int[] _codestream_idx, int[] _data_format) throws KduException
  {
    return Get_opacity_mapping(_colour_idx,_codestream_component,_lut_idx,_codestream_idx,_data_format,null);
  }
  public native boolean Get_premult_mapping(int _colour_idx, int[] _codestream_component, int[] _lut_idx, int[] _codestream_idx, int[] _data_format, int[] _format_params) throws KduException;
  public boolean Get_premult_mapping(int _colour_idx, int[] _codestream_component, int[] _lut_idx, int[] _codestream_idx, int[] _data_format) throws KduException
  {
    return Get_premult_mapping(_colour_idx,_codestream_component,_lut_idx,_codestream_idx,_data_format,null);
  }
  public native boolean Get_chroma_key(int _colour_idx, int[] _key_val) throws KduException;
}
