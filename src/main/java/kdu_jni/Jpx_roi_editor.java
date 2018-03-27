package kdu_jni;

public class Jpx_roi_editor {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Jpx_roi_editor(long ptr) {
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
  public Jpx_roi_editor() {
    this(Native_create());
  }
  public native void Set_max_undo_history(int _history) throws KduException;
  public native boolean Is_empty() throws KduException;
  public native boolean Equals(Jpx_roi_editor _rhs) throws KduException;
  public native void Copy_from(Jpx_roi_editor _rhs) throws KduException;
  public native void Reset() throws KduException;
  public native Kdu_dims Set_mode(int _mode) throws KduException;
  public native void Init(Jpx_roi _regions, int _num_regions) throws KduException;
  public native Jpx_roi Get_regions(int[] _num_regions) throws KduException;
  public native boolean Modify_region(int _idx, Jpx_roi _src) throws KduException;
  public native boolean Get_bounding_box(Kdu_dims _bb, boolean _include_scribble) throws KduException;
  public boolean Get_bounding_box(Kdu_dims _bb) throws KduException
  {
    return Get_bounding_box(_bb,(boolean) true);
  }
  public native boolean Contains_encoded_regions() throws KduException;
  public native boolean Is_simple() throws KduException;
  public native int Get_history_info(int[] _available_undo_elts, boolean[] _can_redo) throws KduException;
  public native boolean Find_nearest_anchor(Kdu_coords _point, boolean _modify_for_selection) throws KduException;
  public native boolean Find_nearest_boundary_point(Kdu_coords _point, boolean _exclude_selected_region) throws KduException;
  public native boolean Find_nearest_guide_point(Kdu_coords _point) throws KduException;
  public native Kdu_dims Select_anchor(Kdu_coords _point, boolean _advance) throws KduException;
  public native Kdu_dims Drag_selected_anchor(Kdu_coords _new_point) throws KduException;
  public native boolean Can_move_selected_anchor(Kdu_coords _new_point, boolean _check_roid_limit) throws KduException;
  public native Kdu_dims Move_selected_anchor(Kdu_coords _new_point) throws KduException;
  public native Kdu_dims Cancel_drag() throws KduException;
  public native Kdu_dims Cancel_selection() throws KduException;
  public native Kdu_dims Add_region(boolean _ellipses, Kdu_dims _visible_frame) throws KduException;
  public native Kdu_dims Delete_selected_region() throws KduException;
  public native double Measure_complexity() throws KduException;
  public native Kdu_dims Clear_scribble_points() throws KduException;
  public native Kdu_dims Add_scribble_point(Kdu_coords _point) throws KduException;
  public native Kdu_coords Get_scribble_points(int[] _num_points) throws KduException;
  public native boolean Get_scribble_point(Kdu_coords _point, int _which) throws KduException;
  public native Kdu_dims Convert_scribble_path(boolean _replace_content, int _conversion_flags, double _accuracy) throws KduException;
  public native Kdu_dims Split_selected_anchor() throws KduException;
  public native Kdu_dims Set_path_thickness(int _thickness, boolean[] _success) throws KduException;
  public native Kdu_dims Fill_closed_paths(boolean[] _success, int _required_member_idx) throws KduException;
  public Kdu_dims Fill_closed_paths(boolean[] _success) throws KduException
  {
    return Fill_closed_paths(_success,(int) -1);
  }
  public native Kdu_dims Undo() throws KduException;
  public native Kdu_dims Redo() throws KduException;
  public native int Get_selection(Kdu_coords _point, int[] _num_point_instances) throws KduException;
  public native int Enum_paths(long[] _path_flags, byte[] _path_members, Kdu_coords _path_start, Kdu_coords _path_end) throws KduException;
  public native Jpx_roi Get_region(int _idx) throws KduException;
  public native boolean Get_path_segment_for_region(int _idx, Kdu_coords _ep1, Kdu_coords _ep2) throws KduException;
  public native int Get_anchor(Kdu_coords _point, int _which, boolean _selected_region_only, boolean _dragged) throws KduException;
  public native int Get_edge(Kdu_coords _from, Kdu_coords _to, int _which, boolean _selected_region_only, boolean _dragged, boolean _want_shared_flag) throws KduException;
  public native int Get_curve(Kdu_coords _centre, Kdu_coords _extent, Kdu_coords _skew, int _which, boolean _selected_region_only, boolean _dragged) throws KduException;
  public native int Get_path_segment(Kdu_coords _from, Kdu_coords _to, int _which) throws KduException;
}
