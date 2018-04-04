package kdu_jni;

public class Jpx_compatibility {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jpx_compatibility(long ptr) {
    _native_ptr = ptr;
  }
  public Jpx_compatibility() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native boolean Is_jp2() throws KduException;
  public native boolean Is_jp2_compatible() throws KduException;
  public native boolean Is_jpxb_compatible() throws KduException;
  public native boolean Has_reader_requirements_box() throws KduException;
  public native boolean Check_standard_feature(int _feature_id) throws KduException;
  public native boolean Check_vendor_feature(byte[] _uuid) throws KduException;
  public native boolean Get_standard_feature(int _which, int[] _feature_id) throws KduException;
  public native boolean Get_standard_feature(int _which, int[] _feature_id, boolean[] _is_supported) throws KduException;
  public native boolean Get_vendor_feature(int _which, byte[] _uuid) throws KduException;
  public native boolean Get_vendor_feature(int _which, byte[] _uuid, boolean[] _is_supported) throws KduException;
  public native void Set_standard_feature_support(int _feature_id, boolean _is_supported) throws KduException;
  public native void Set_vendor_feature_support(byte[] _uuid, boolean _is_supported) throws KduException;
  public native boolean Test_fully_understand() throws KduException;
  public native boolean Test_decode_completely() throws KduException;
  public native void Copy(Jpx_compatibility _src) throws KduException;
  public native void Set_used_standard_feature(int _feature_id, byte _fully_understand_sub_expression, byte _decode_completely_sub_expression) throws KduException;
  public void Set_used_standard_feature(int _feature_id) throws KduException
  {
    Set_used_standard_feature(_feature_id,(byte) 255,(byte) 255);
  }
  public void Set_used_standard_feature(int _feature_id, byte _fully_understand_sub_expression) throws KduException
  {
    Set_used_standard_feature(_feature_id,_fully_understand_sub_expression,(byte) 255);
  }
  public native void Set_used_vendor_feature(byte[] _uuid, byte _fully_understand_sub_expression, byte _decode_completely_sub_expression) throws KduException;
  public void Set_used_vendor_feature(byte[] _uuid) throws KduException
  {
    Set_used_vendor_feature(_uuid,(byte) 255,(byte) 255);
  }
  public void Set_used_vendor_feature(byte[] _uuid, byte _fully_understand_sub_expression) throws KduException
  {
    Set_used_vendor_feature(_uuid,_fully_understand_sub_expression,(byte) 255);
  }
}
