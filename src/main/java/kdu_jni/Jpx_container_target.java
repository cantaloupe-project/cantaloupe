package kdu_jni;

public class Jpx_container_target {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jpx_container_target(long ptr) {
    _native_ptr = ptr;
  }
  public Jpx_container_target() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native int Get_container_id() throws KduException;
  public native int Get_num_top_codestreams() throws KduException;
  public native int Get_num_top_layers() throws KduException;
  public native int Get_base_codestreams(int[] _num_base_codestreams) throws KduException;
  public native int Get_base_layers(int[] _num_base_layers) throws KduException;
  public native Jpx_layer_target Access_layer(int _which) throws KduException;
  public native Jpx_codestream_target Access_codestream(int _which) throws KduException;
  public native Jpx_composition Add_presentation_track(int _track_layers) throws KduException;
}
