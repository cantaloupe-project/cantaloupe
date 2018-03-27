package kdu_jni;

public class Kdu_node {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_node(long ptr) {
    _native_ptr = ptr;
  }
  public Kdu_node() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native boolean Compare(Kdu_node _src) throws KduException;
  public native Kdu_node Access_child(int _child_idx) throws KduException;
  public native int Get_directions() throws KduException;
  public native int Get_num_descendants(int[] _num_leaf_descendants) throws KduException;
  public native Kdu_subband Access_subband() throws KduException;
  public native Kdu_resolution Access_resolution() throws KduException;
  public native void Get_dims(Kdu_dims _dims) throws KduException;
  public native int Get_kernel_id() throws KduException;
  public native long Get_kernel_coefficients(boolean _vertical) throws KduException;
  public native long Get_bibo_gains(int[] _num_steps, boolean _vertical) throws KduException;
}
