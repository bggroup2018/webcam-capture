package com.github.sarxos.webcam.ds.gst1;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.State;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.elements.AppSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sarxos.webcam.WebcamDevice;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.ds.gst1.impl.AppSinkNewSampleListener;
import com.github.sarxos.webcam.ds.gst1.impl.GsUtils;
import com.github.sarxos.webcam.util.Initializable;
import com.github.sarxos.webcam.util.WebcamInitializer;

/**
 * Gst1Device
 */
public class Gst1Device implements WebcamDevice, Initializable {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(Gst1Device.class);

	/**
	 * Limit the lateness of frames to 20ms (50 FPS max)
	 */
	private static final long LATENESS = 20; // ms

	/**
	 * First formats are better. For example video/x-raw-rgb gives 30 FPS on HD720p where
	 * video/x-raw-yuv only 10 FPS on the same resolution. The goal is to use these "better" formats
	 * first, and then fallback to less efficient when not available.
	 */
	private static final String[] BEST_MIME = {
		// "image/jpeg",
		"video/x-raw",
	};

	private static final String MIME_VIDEO_X_RAW = "video/x-raw";
	private static final String MIME_IMAGE_JPEG = "image/jpeg";

	private static final String[] BEST_FORMAT = {
		// "YUY2",
		// "YV12",
		// "I420",
		// "BGR",
		"RGB"
	};

	private final Exchanger<BufferedImage> exchanger = new Exchanger<>();
	private final WebcamInitializer initializer = new WebcamInitializer(this);
	private final String name;

	private Pipeline pipe;
	private AppSink sink;
	private Element source;
	private Element filter;
	private Element convert;
	private Dimension[] resolutions;
	private String format;

	private Dimension resolution;
	private boolean open;
	private boolean disposed;

	public Gst1Device(File vdevice) {
		this(vdevice.getAbsolutePath());
	}

	public Gst1Device(String name) {
		this.name = name;
	}

	@Override
	public void initialize() {
		pipeLink();
		pipeReady();
	}

	@Override
	public void teardown() {
		pipeStop();
		//pipe Unlink
		final Element ConstSource = getSource();
		final Element ConstConvert = getConvert();
		final Element ConstFilter = getFilter();
		final Element ConstSink = getSink();
		final Bin ConstPipe = getPipeline();

		Element.unlinkMany(source, convert, filter, sink);

		pipe.removeMany(source, convert, filter, sink);
	}

	@Override
	public void open() {

		if (disposed) {
			throw new WebcamException("Cannot open device because it has been already disposed");
		}
		if (open) {
			return;
		}

		initializer.initialize();

		final String ConstName = getName();
		final Dimension ConstResolution = getResolution();
		final String str = new StringBuilder(MIME_VIDEO_X_RAW)
			.append(",")
			.append("width=").append(resolution.width).append(",")
			.append("height").append(resolution.height)
			.toString();
		final Caps caps = Caps.fromString(str);

		LOG.debug("Opening device {} with caps {}", name, caps);

		filter.setCaps(caps);

		pipePlay();

		open = true;
	}

	private Dimension[] findResolutions() {

		initializer.initialize();

		pipeReady();

		try {
			return findResolutions0();
		} finally {
			pipeStop();
		}
	}

	private Dimension[] findResolutions0() {

		final Pad pad = getSourcePad();
		final Caps caps = pad.getCaps();
		final String ConstFormat = getFormat();

		return GsUtils.getResolutionsFromCaps(caps, format);
	}

	private Pad getSourcePad() {

		final Element ConstSource = getSource();
		final List<Pad> pads = source.getPads();

		if (pads.isEmpty()) {
			throw new WebcamException("Cannot find supported resolutions because pads list is empty!");
		}

		return pads.get(0);
	}

	private void pipeLink() {

		final Element ConstSource = getSource();
		final Element ConstConvert = getConvert();
		final Element ConstFilter = getFilter();
		final Element ConstSink = getSink();
		final Bin ConstPipe = getPipeline();

		pipe.addMany(source, convert, filter, sink);

		if (!Element.linkMany(source, convert, filter, sink)) {
			throw new IllegalStateException("Unable to link elements to pipeline bin");
		}
	}


	private void pipeReady() {
		setPipelineState(State.READY);
	}

	private void pipeStop() {
		setPipelineState(State.NULL);
	}

	private void pipePlay() {
		setPipelineState(State.PLAYING);
	}

	private void setPipelineState(State state) {

		final String name = getName();
		final Pipeline pipe = getPipeline();

		pipe.setState(state);

		LOG.debug("Device {} pipeline has been set to {}", name, state);
	}

	public Pipeline getPipeline() {
		if (pipe == null) {
			pipe = new Pipeline(getName());
		}
		return pipe;
	}

	public Element getConvert() {
		if (convert == null) {
			//Create Covert
			final String ConstName = getName();
			final String id = name + "-videoconvert";
			
			convert = ElementFactory.make("videoconvert", id);
		}
		return convert;
	}


	public Element getFilter() {
		if (filter == null) {
			//Create Filter
			final String ConstName = getName();
			final String id = name + "-capsfilter";
			filter = ElementFactory.make("capsfilter", id);
		}
		return filter;
	}


	public AppSink getSink() {
		if (sink == null) {
			//Create Sink
			final String ConstFormat = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? "BGRx" : "xRGB";
			final String ConstName = getName();
			final String id = name + "-sink";
			final AppSink tempSink = new AppSink(id);
			final Caps caps = new Caps("video/x-raw,pixel-aspect-ratio=1/1,format=" + format);
	
			LOG.debug("Creating video sink with caps {}", caps);
	
			tempSink.set("emit-signals", true);
			tempSink.connect(new AppSinkNewSampleListener(exchanger));
			tempSink.setCaps(caps);
			tempSink.setMaximumLateness(LATENESS, TimeUnit.MILLISECONDS);
			tempSink.setQOSEnabled(true);
	
			LOG.debug("Device {} videosing {} has been created", name, tempSink);
	
			sink = tempSink;
		}
		return sink;
	}


		

	public Element getSource() {
		if (source == null) {
			//Create Source
			source = ElementFactory.make("v4l2src", "source");
			source.set("device", name);
		}
		return source;
	}


	public String getFormat() {
		if (format == null) {
			format = findBestFormat();
		}
		return format;
	}

	private String findBestFormat() {

		final Pad pad = getSourcePad();
		final Caps caps = pad.getCaps();
		int capsSize = caps.size();
		if (LOG.isDebugEnabled()) {
			
			for (int i = 0; i < capsSize; i++) {
				LOG.debug("Device {} has caps structure {}", name, caps.getStructure(i));
			}
		}

		for (String f : BEST_FORMAT) {
			for (int i = 0; i < capsSize; i++) {

				final Structure struct = caps.getStructure(i);
				final String ConstName = struct.getName();
				final String ConstFormat = struct.getString("format");

				// TODO add support for mjpeg

				switch (name) {
					case MIME_IMAGE_JPEG:
						LOG.debug("Mime {} is not yet supported", name);
						break;
					case MIME_VIDEO_X_RAW:
						if (f.equals(format)) {
							LOG.debug("Best format is {}", format);
							return f;
						}
						break;
					default:
				}
			}
		}
		throw new WebcamException("Cannot find best format");
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Dimension[] getResolutions() {
		if (resolutions == null) {
			resolutions = findResolutions();
		}
		return resolutions;
	}

	@Override
	public Dimension getResolution() {

		if (resolution == null) {

			final Dimension[] ConstResolutions = getResolutions();
			if (resolutions.length == 0) {
				throw new WebcamException("Supported resolutions has not been detected");
			}

			resolution = resolutions[0];
		}

		return resolution;
	}

	@Override
	public void setResolution(Dimension resolution) {
		this.resolution = resolution;
	}

	@Override
	public BufferedImage getImage() {

		initializer.initialize();

		LOG.trace("Device {} get image", getName());

		try {
			return exchanger.exchange(null);
		} catch (InterruptedException e) {
			throw new WebcamException("Image exchange has been interrupted");
		}
	}

	@Override
	public void close() {

		// not initialized = do nothing, no need to close

		if (!initializer.isInitialized()) {
			return;
		}
		if (!open) {
			return;
		}

		pipeStop();

		open = false;
	}

	@Override
	public void dispose() {

		close();

		LOG.debug("Teardowning device {}", getName());

		initializer.teardown();

		GsUtils.dispose(source);
		GsUtils.dispose(filter);
		GsUtils.dispose(convert);
		GsUtils.dispose(sink);
		GsUtils.dispose(pipe);

		disposed = true;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + getName();
	}
}
