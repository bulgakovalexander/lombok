//version 8:9
//unchanged
import lombok.val;

public class ValErrorsJava8 {
	public void ternaryOperatorWithLambda() {
		val e = System.currentTimeMillis() > 0 ? () -> {
		} : (Runnable) () -> {
		};
	}
}