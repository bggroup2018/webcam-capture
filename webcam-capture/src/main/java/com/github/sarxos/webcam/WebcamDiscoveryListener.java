package com.github.sarxos.webcam;

/**
 * WebcamDiscoveryListener
 */
public interface WebcamDiscoveryListener {

	void webcamFound(WebcamDiscoveryEvent event);

	void webcamGone(WebcamDiscoveryEvent event);

}
