import java.io.IOException;

import com.github.sarxos.webcam.Webcam;

/**
 * NonBlockingModeExample
 */
public class NonBlockingModeExample {

	// this should be set only in development phase, it shall NOT be used in
	// production due to unknown side effects
	static {
		Webcam.setHandleTermSignal(true);
	}
	
	/**
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {

		Webcam webcam = Webcam.getDefault();
		webcam.open(true);

		for (int i = 0; i < 100; i++) {
			webcam.getImage();
			Thread.sleep(100);
		}
	}
}
