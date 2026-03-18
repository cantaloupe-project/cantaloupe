package kdu_jni;

public class Enc_params extends Kdu_params implements AutoCloseable {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Enc_params(long ptr) {
    super(ptr);
  }
  public native void Native_destroy();
  @Override
  public void close() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }

  private static native long Native_create();
  public Enc_params() {
    this(Native_create());
  }
  public native boolean Is_visual_ctype(int _ctp) throws KduException;
  public native boolean Is_chroma_ctype(int _ctp) throws KduException;
  public native boolean Is_luma_ctype(int _ctp) throws KduException;
  public native float Modulation_for_qfactor(float _qf) throws KduException;
}
