package kdu_jni;

public class Kdu_roi_image implements AutoCloseable {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_roi_image(long ptr) {
    _native_ptr = ptr;
  }
  public native void Native_destroy();
  @Override
  public void close() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }

  public native Kdu_roi_node Acquire_node(int _component, Kdu_dims _tile_region) throws KduException;
}
