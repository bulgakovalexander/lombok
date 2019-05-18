//version 10:
//unchanged
import lombok.val;

public class ValErrorsJava10 {
	public void ternaryOperatorWithLambda() {
		val e = System.currentTimeMillis() > 0 ? () -> {
		} : (Runnable) () -> {
		};
	}
}