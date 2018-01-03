package com.github.sarxos.webcam;

import java.awt.image.BufferedImage;

/**
 * WebcamImageTransformer
 */
public interface WebcamImageTransformer {

	BufferedImage transform(BufferedImage image);

}
