import lombok.val;
public class ValErrorsJava8 {
  public ValErrorsJava8() {
    super();
  }
  public void ternaryOperatorWithLambda() {
    val e = ((System.currentTimeMillis() > 0) ? () -> {
} : (Runnable) () -> {
});
  }
}