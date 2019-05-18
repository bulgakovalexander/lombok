import lombok.val;
public class ValErrorsJava10 {
  public ValErrorsJava10() {
    super();
  }
  public void ternaryOperatorWithLambda() {
    val e = ((System.currentTimeMillis() > 0) ? () -> {
} : (Runnable) () -> {
});
  }
}