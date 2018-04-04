package kdu_jni;

public class Jpx_codestream_target {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jpx_codestream_target(long ptr) {
    _native_ptr = ptr;
  }
  public Jpx_codestream_target() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native int Get_codestream_id() throws KduException;
  public native Jp2_dimensions Access_dimensions() throws KduException;
  public native Jp2_palette Access_palette() throws KduException;
  public native void Copy_attributes(Jpx_codestream_source _src) throws KduException;
  public native Jpx_fragment_list Access_fragment_list() throws KduException;
  public native void Add_fragment(String _url_or_path, long _offset, long _length, boolean _is_path) throws KduException;
  public void Add_fragment(String _url_or_path, long _offset, long _length) throws KduException
  {
    Add_fragment(_url_or_path,_offset,_length,(boolean) false);
  }
  public native void Write_fragment_table() throws KduException;
  public native Jp2_output_box Open_stream() throws KduException;
  public native Kdu_compressed_target Access_stream() throws KduException;
}
