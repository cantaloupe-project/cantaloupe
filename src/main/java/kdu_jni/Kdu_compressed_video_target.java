package kdu_jni;

public class Kdu_compressed_video_target extends Kdu_compressed_target {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_compressed_video_target(long ptr) {
    super(ptr);
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  public native void Open_image() throws KduException;
  public native void Close_image(Kdu_codestream _codestream) throws KduException;
}
