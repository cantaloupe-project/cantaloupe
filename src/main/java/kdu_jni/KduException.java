package kdu_jni;

public class KduException extends Exception {
    public KduException() {
      super();
      this.kdu_exception_code = Kdu_global.KDU_NULL_EXCEPTION;
    }
    public KduException(String message) {
      super(message);
      this.kdu_exception_code = Kdu_global.KDU_NULL_EXCEPTION;
    }
    public KduException(int exc_code) {
      super();
      this.kdu_exception_code = exc_code;
    }
    public KduException(int exc_code, String message) {
      super(message);
      this.kdu_exception_code = exc_code;
    }
    public int Get_kdu_exception_code() {
      return kdu_exception_code;
    }
    public boolean Compare(int exc_code) {
      if (exc_code == kdu_exception_code)
        return true;
      else
        return false;
    }
    private int kdu_exception_code;
}
