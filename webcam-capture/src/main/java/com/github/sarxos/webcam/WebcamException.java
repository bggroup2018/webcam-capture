package com.github.sarxos.webcam;
/**
 * WebcamException
 */
public class WebcamException extends Exception {

	private static final long serialVersionUID = 4305046981807594375L;

	public WebcamException(String message) {
		super(message);
	}

	public WebcamException(String message, Throwable cause) {
		super(message, cause);
	}

	public WebcamException(Throwable cause) {
		super(cause);
	}

}
