package kdu_jni;

public class Kdu_tiffdir {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_tiffdir(long ptr) {
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
  public Kdu_tiffdir() {
    this(Native_create());
  }
  public native boolean Exists() throws KduException;
  public native void Init(boolean _littlendian, boolean _bigtiff) throws KduException;
  public void Init(boolean _littlendian) throws KduException
  {
    Init(_littlendian,(boolean) false);
  }
  public native boolean Opendir(Kdu_compressed_source _src) throws KduException;
  public native int Write_header(Kdu_compressed_target _tgt, long _dir_offset) throws KduException;
  public native boolean Writedir(Kdu_compressed_target _tgt, long _dir_offset) throws KduException;
  public native boolean Is_littlendian() throws KduException;
  public native boolean Is_native_littlendian() throws KduException;
  public native void Close() throws KduException;
  public native long Get_dirlength() throws KduException;
  public native long Get_taglength(long _tag_type) throws KduException;
  public native int Get_fieldlength(long _tag_type) throws KduException;
  public native boolean Delete_tag(long _tag_type) throws KduException;
  public native long Open_tag(long _tag_type) throws KduException;
  public native long Read_tag(long _tag_type, long _length, byte[] _data) throws KduException;
  public native long Read_tag(long _tag_type, long _length, int[] _data) throws KduException;
  public native long Read_tag(long _tag_type, long _length, double[] _data) throws KduException;
  public native long Read_tag(long _tag_type, long _length, long[] _data) throws KduException;
  public native void Create_tag(long _tag_type) throws KduException;
  public native void Write_tag(long _tag_type, int _length, byte[] _data) throws KduException;
  public native void Write_tag(long _tag_type, int _length, int[] _data) throws KduException;
  public native void Write_tag(long _tag_type, int _length, double[] _data) throws KduException;
  public native void Write_tag(long _tag_type, int _length, long[] _data) throws KduException;
  public native void Write_tag(long _tag_type, int _val16) throws KduException;
  public native void Write_tag(long _tag_type, double _valdbl) throws KduException;
  public native void Write_tag(long _tag_type, long _val64) throws KduException;
  public native void Copy_tag(Kdu_tiffdir _src, long _tag_type) throws KduException;
}
