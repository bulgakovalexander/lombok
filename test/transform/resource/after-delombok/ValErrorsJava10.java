public class ValErrorsJava10 {
	public void ternaryOperatorWithLambda() {
		final java.lang.Object e = System.currentTimeMillis() > 0 ? () -> {
		} : (Runnable) () -> {
		};
	}
}