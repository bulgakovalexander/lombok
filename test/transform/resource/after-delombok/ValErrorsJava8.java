public class ValErrorsJava8 {
	public void ternaryOperatorWithLambda() {
		final java.lang.Object e = System.currentTimeMillis() > 0 ? () -> {
		} : (Runnable) () -> {
		};
	}
}