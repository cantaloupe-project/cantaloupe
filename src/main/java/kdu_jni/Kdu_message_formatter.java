package kdu_jni;

public class Kdu_message_formatter extends Kdu_message {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected Kdu_message_formatter(long ptr) {
    super(ptr);
  }
  public native void Native_destroy();
  public void finalize() {
    if ((_native_ptr & 1) != 0)
      { // Resource created and not donated
        Native_destroy();
      }
  }
  private static native long Native_create(Kdu_message _output, int _max_line);
  public Kdu_message_formatter(Kdu_message _output, int _max_line) {
    this(Native_create(_output, _max_line));
  }
  private static long Native_create(Kdu_message _output)
  {
    return Native_create(_output,(int) 79);
  }
  public Kdu_message_formatter(Kdu_message _output) {
    this(Native_create(_output));
  }
  public native void Set_master_indent(int _val) throws KduException;
  public native void Put_text(String _string) throws KduException;
  public native void Flush(boolean _end_of_message) throws KduException;
  public void Flush() throws KduException
  {
    Flush((boolean) false);
  }
  public native void Start_message() throws KduException;
}
