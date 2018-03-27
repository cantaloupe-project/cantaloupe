package kdu_jni;

public class Kdu_roi_rect extends Kdu_roi_image {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_roi_rect(long ptr) {
    super(ptr);
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create(Kdu_codestream _codestream, Kdu_dims _region);
  public Kdu_roi_rect(Kdu_codestream _codestream, Kdu_dims _region) {
    this(Native_create(_codestream, _region));
  }
}
