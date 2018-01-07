import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;

/**
 * WebcamDiscoveryListenerExample
 */
public class WebcamDiscoveryListenerExample implements WebcamDiscoveryListener {

	public WebcamDiscoveryListenerExample() {
		for (Webcam webcam : Webcam.getWebcams()) {
			System.out.println("Webcam detected: " + webcam.getName());
		}
		Webcam.addDiscoveryListener(this);
		System.out.println();
		System.out.println();
		System.out.println("Please connect additional webcams, or disconnect already connected ones. Listening for events...");
	}

	@Override
	public void webcamFound(WebcamDiscoveryEvent event) {
		System.out.println("Webcam connected: " + event.getWebcam().getName());
	}

	@Override
	public void webcamGone(WebcamDiscoveryEvent event) {
		System.out.println("Webcam disconnected: " + event.getWebcam().getName());
	}

	public static void main(String[] args) throws Throwable {
		new WebcamDiscoveryListenerExample();
		Thread.sleep(120000);
		System.out.println("Bye!");
	}
}
