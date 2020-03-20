package kdu_jni;

public class Kdu_block_decoder extends Kdu_block_decoder_base {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_block_decoder(long ptr) {
    super(ptr);
  }
  private native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create();
  public Kdu_block_decoder() {
    this(Native_create());
  }
  public native void Speedpack_config(Kdu_coords _nominal_block_size, int _K_max_prime) throws KduException;
  public native void Decode(Kdu_block _block) throws KduException;
}
