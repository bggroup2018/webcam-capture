import java.io.IOException;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamMotionDetector;


/**
 * Detect motion. This example demonstrates of how to use build-in motion
 * detector feature without the motion listener. We simply run separate thread
 * and check every 1000 milliseconds if there was a motion. In this example
 * motion once detected is valid for 2 seconds.
 * 
 * @author Bartosz Firyn (SarXos)
 */
public class DetectMotionExample2 {

	public DetectMotionExample2() {

		final WebcamMotionDetector detector = new WebcamMotionDetector(Webcam.getDefault());
		detector.setInterval(500); // one check per 500 ms
		detector.start();

		Thread t = new Thread("motion-printer") {

			@Override
			public void run() {
				if(detector != null){
					try {
						do {
							System.out.println(detector.isMotion());
							Thread.sleep(1000);
						} while (true);
					} catch (InterruptedException e) {
						System.out.println("The thread has been interrupted");
					}
				}
			}
		};

		t.setDaemon(true);
		t.start();
	}

	public static void main(String[] args) throws IOException {
		new DetectMotionExample2();
		System.in.read(); // keep program open
	}
}
