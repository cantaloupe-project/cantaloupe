package kdu_jni;

public class Jpx_roi {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jpx_roi(long ptr) {
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
  public Jpx_roi() {
    this(Native_create());
  }
  public native void Init_rectangle(Kdu_dims _rect, boolean _coded, byte _priority) throws KduException;
  public void Init_rectangle(Kdu_dims _rect) throws KduException
  {
    Init_rectangle(_rect,(boolean) false,(byte) 0);
  }
  public void Init_rectangle(Kdu_dims _rect, boolean _coded) throws KduException
  {
    Init_rectangle(_rect,_coded,(byte) 0);
  }
  public native void Init_quadrilateral(Kdu_coords _v1, Kdu_coords _v2, Kdu_coords _v3, Kdu_coords _v4, boolean _coded, byte _priority) throws KduException;
  public void Init_quadrilateral(Kdu_coords _v1, Kdu_coords _v2, Kdu_coords _v3, Kdu_coords _v4) throws KduException
  {
    Init_quadrilateral(_v1,_v2,_v3,_v4,(boolean) false,(byte) 0);
  }
  public void Init_quadrilateral(Kdu_coords _v1, Kdu_coords _v2, Kdu_coords _v3, Kdu_coords _v4, boolean _coded) throws KduException
  {
    Init_quadrilateral(_v1,_v2,_v3,_v4,_coded,(byte) 0);
  }
  public native void Init_ellipse(Kdu_coords _centre, Kdu_coords _extent, Kdu_coords _skew, boolean _coded, byte _priority) throws KduException;
  public void Init_ellipse(Kdu_coords _centre, Kdu_coords _extent, Kdu_coords _skew) throws KduException
  {
    Init_ellipse(_centre,_extent,_skew,(boolean) false,(byte) 0);
  }
  public void Init_ellipse(Kdu_coords _centre, Kdu_coords _extent, Kdu_coords _skew, boolean _coded) throws KduException
  {
    Init_ellipse(_centre,_extent,_skew,_coded,(byte) 0);
  }
  public native void Init_ellipse(Kdu_coords _centre, double[] _axis_extents, double _tan_theta, boolean _coded, byte _priority) throws KduException;
  public native void Clip_region() throws KduException;
  public native void Fix_inconsistencies() throws KduException;
  public native boolean Is_simple() throws KduException;
  public native void Get_bounding_rect(Kdu_dims _rect) throws KduException;
  public native boolean Get_rectangle(Kdu_dims _rectangle) throws KduException;
  public native boolean Get_quadrilateral(Kdu_coords _v1, Kdu_coords _v2, Kdu_coords _v3, Kdu_coords _v4) throws KduException;
  public native boolean Get_ellipse(Kdu_coords _centre, Kdu_coords _extent, Kdu_coords _skew) throws KduException;
  public native boolean Get_ellipse(Kdu_coords _centre, double[] _axis_extents, double[] _tan_theta) throws KduException;
  public native boolean Equals(Jpx_roi _rhs) throws KduException;
  public native boolean Check_geometry() throws KduException;
  public native boolean Check_edge_intersection(int _n, Kdu_coords _v1, Kdu_coords _v2) throws KduException;
  public native int Measure_span(double[] _width, double[] _length) throws KduException;
  public native double Measure_area(double[] _centroid_x, double[] _centroid_y) throws KduException;
  public native boolean Contains(Kdu_coords _point) throws KduException;
  public native int Find_boundary_projection(double _x0, double _y0, double[] _xp, double[] _yp, double _max_distance, double _tolerance) throws KduException;
  public int Find_boundary_projection(double _x0, double _y0, double[] _xp, double[] _yp, double _max_distance) throws KduException
  {
    return Find_boundary_projection(_x0,_y0,_xp,_yp,_max_distance,(double) 0.01);
  }
}
