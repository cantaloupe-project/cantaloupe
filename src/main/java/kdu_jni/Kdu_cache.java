package kdu_jni;

public class Kdu_cache extends Kdu_compressed_source {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_cache(long ptr) {
    super(ptr);
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create();
  public Kdu_cache() {
    this(Native_create());
  }
  public native void Attach_to(Kdu_cache _existing) throws KduException;
  public native boolean Add_to_databin(int _databin_class, long _codestream_id, long _databin_id, byte[] _data, int _offset, int _num_bytes, boolean _is_final, boolean _add_as_most_recent, boolean _mark_if_augmented) throws KduException;
  public boolean Add_to_databin(int _databin_class, long _codestream_id, long _databin_id, byte[] _data, int _offset, int _num_bytes, boolean _is_final) throws KduException
  {
    return Add_to_databin(_databin_class,_codestream_id,_databin_id,_data,_offset,_num_bytes,_is_final,(boolean) true,(boolean) false);
  }
  public boolean Add_to_databin(int _databin_class, long _codestream_id, long _databin_id, byte[] _data, int _offset, int _num_bytes, boolean _is_final, boolean _add_as_most_recent) throws KduException
  {
    return Add_to_databin(_databin_class,_codestream_id,_databin_id,_data,_offset,_num_bytes,_is_final,_add_as_most_recent,(boolean) false);
  }
  public native boolean Delete_databin(int _databin_class, long _codestream_id, long _databin_id, boolean _mark_if_nonempty) throws KduException;
  public boolean Delete_databin(int _databin_class, long _codestream_id, long _databin_id) throws KduException
  {
    return Delete_databin(_databin_class,_codestream_id,_databin_id,(boolean) true);
  }
  public native int Delete_stream_class(int _databin_class, long _codestream_id, boolean _mark_if_nonempty) throws KduException;
  public int Delete_stream_class(int _databin_class, long _codestream_id) throws KduException
  {
    return Delete_stream_class(_databin_class,_codestream_id,(boolean) true);
  }
  public native void Set_preferred_memory_limit(long _preferred_byte_limit) throws KduException;
  public native void Trim_to_preferred_memory_limit() throws KduException;
  public native void Preserve_databin(int _databin_class, long _codestream_id, long _databin_id) throws KduException;
  public native void Preserve_class_stream(int _databin_class, long _codestream_id) throws KduException;
  public native void Touch_databin(int _databin_class, long _codestream_id, long _databin_id) throws KduException;
  public native int Mark_databin(int _databin_class, long _codestream_id, long _databin_id, boolean _mark_state, int[] _length, boolean[] _is_complete) throws KduException;
  public native boolean Stream_class_marked(int _databin_class, long _codestream_id) throws KduException;
  public native void Clear_all_marks() throws KduException;
  public native void Set_all_marks() throws KduException;
  public native int Get_databin_length(int _databin_class, long _codestream_id, long _databin_id, boolean[] _is_complete) throws KduException;
  public int Get_databin_length(int _databin_class, long _codestream_id, long _databin_id) throws KduException
  {
    return Get_databin_length(_databin_class,_codestream_id,_databin_id,null);
  }
  public native boolean Scan_databins(int _scan_flags, int[] _databin_class, long[] _codestream_id, long[] _databin_id, int[] _bin_length, boolean[] _bin_complete, byte[] _buf, int _buf_len) throws KduException;
  public boolean Scan_databins(int _scan_flags, int[] _databin_class, long[] _codestream_id, long[] _databin_id, int[] _bin_length, boolean[] _bin_complete) throws KduException
  {
    return Scan_databins(_scan_flags,_databin_class,_codestream_id,_databin_id,_bin_length,_bin_complete,null,(int) 0);
  }
  public boolean Scan_databins(int _scan_flags, int[] _databin_class, long[] _codestream_id, long[] _databin_id, int[] _bin_length, boolean[] _bin_complete, byte[] _buf) throws KduException
  {
    return Scan_databins(_scan_flags,_databin_class,_codestream_id,_databin_id,_bin_length,_bin_complete,_buf,(int) 0);
  }
  public native int Set_read_scope(int _databin_class, long _codestream_id, long _databin_id, boolean[] _is_complete) throws KduException;
  public int Set_read_scope(int _databin_class, long _codestream_id, long _databin_id) throws KduException
  {
    return Set_read_scope(_databin_class,_codestream_id,_databin_id,null);
  }
  public native long Get_max_codestream_id() throws KduException;
  public native long Get_peak_cache_memory() throws KduException;
}
