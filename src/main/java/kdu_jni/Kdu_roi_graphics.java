package kdu_jni;

public class Kdu_roi_graphics extends Kdu_roi_image {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_roi_graphics(long ptr) {
    super(ptr);
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create(Kdu_codestream _codestream, String _fname, float _threshold);
  public Kdu_roi_graphics(Kdu_codestream _codestream, String _fname, float _threshold) {
    this(Native_create(_codestream, _fname, _threshold));
  }
}
