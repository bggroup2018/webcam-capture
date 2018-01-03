package com.github.sarxos.webcam.ds.ffmpegcli;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sarxos.webcam.WebcamDevice;

/**
 *FFmpegCliDevice
 */
public class FFmpegCliDevice implements WebcamDevice, WebcamDevice.BufferAccess {

	private static final Logger LOG = LoggerFactory.getLogger(FFmpegCliDevice.class);
	private static final Runtime RT = Runtime.getRuntime();

	private File vfile = null;
	private String name = null;
	private Dimension[] resolutions = null;
	private Dimension resolution = null;
	private Process process = null;
	private File pipe = null;
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private DataInputStream dis = null;

	private AtomicBoolean open = new AtomicBoolean(false);
	private AtomicBoolean disposed = new AtomicBoolean(false);

	protected FFmpegCliDevice(File vfile, String vinfo) {

		String[] parts = vinfo.split(" : ");

		this.vfile = vfile;
		this.name = vfile.getAbsolutePath();
		this.resolutions = readResolutions(parts[3].trim());
	}

	private Dimension[] readResolutions(String res) {

		List<Dimension> resolutions = new ArrayList<Dimension>();
		String[] parts = res.split(" ");

		for (String part : parts) {
			String[] xy = part.split("x");
			resolutions.add(new Dimension(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
		}

		return resolutions.toArray(new Dimension[resolutions.size()]);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Dimension[] getResolutions() {
		return resolutions;
	}

	@Override
	public Dimension getResolution() {
		if (resolution == null) {
			resolution = getResolutions()[0];
		}
		return resolution;
	}

	private String getResolutionString() {
		Dimension d = getResolution();
		return String.format("%dx%d", d.width, d.height);
	}

	@Override
	public void setResolution(Dimension resolution) {
		this.resolution = resolution;
	}
	
	private final void CiclomaticComlexityReduced(int v){
		try {// read until EOI
			boolean founded = false;
			do {
				baos.write(v = dis.readUnsignedByte());
				if (v == 0xFF) {
					baos.write(v = dis.readUnsignedByte());
					if (v == 0xD9) {
						founded = true; // EOI found
					}
				}
			} while (!founded);
	
		} catch (IOException e) {
			throw new RuntimeException("failed or interrupted I/O operation");
		}		
	}

	private byte[] readBytes() {
		synchronized (this) {
			if (!open.get()) {
				return null;
			}

			baos.reset();

			int b=0, c=0;
			try {// search for SOI
				boolean founded = false;
				do {
					if (((b = dis.readUnsignedByte()) == 0xFF) && ((c = dis.readUnsignedByte()) == 0xD8)) {
							baos.write(b);
							baos.write(c);
							founded = true; //SOI found
					}
				} while (!founded);

				// read until EOI
				CiclomaticComlexityReduced(c);
				
			} catch (IOException e) {
				throw new RuntimeException("failed or interrupted I/O operations");
			}

			return baos.toByteArray();	
		}
	}

	@Override
	public ByteBuffer getImageBytes() {
		synchronized (this){	
			if (!open.get()) {
				return null;
			}
			return ByteBuffer.wrap(readBytes());
		}
	}

	@Override
	public void getImageBytes(ByteBuffer buffer) {
		if (!open.get()) {
			return;
		}
		buffer.put(readBytes());
	}

	@Override
	public BufferedImage getImage() throws IOException {

		if (!open.get()) {
			return null;
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(readBytes());
		try {
			return ImageIO.read(bais);
		} catch (IOException e) {
			throw new RuntimeException("failed or interrupted I/O operations");
		} finally {
				bais.close();
		}
	}

	@Override
	public void open() {
		synchronized(this){
			if (disposed.get()) {
				return;
			}
	
			if (!open.compareAndSet(false, true)) {
				return;
			}
		}

		pipe = new File(File.separator + "tmp" + File.separator + vfile.getName() + ".mjpeg");
		
		//@formatter:off
		String[] cmd = new String[] { 
			"ffmpeg", 
			"-y",                          // overwrite output file 
			"-f", "video4linux2",          // format
			"-input_format", "mjpeg",      // input format
			"-r", "50",                    // requested FPS
			"-s", getResolutionString(),   // frame dimension
			"-i", vfile.getAbsolutePath(), // input file 
			"-vcodec", "copy",             // video codec to be used
			pipe.getAbsolutePath(),        // output file
		};
		//@formatter:on

		if (LOG.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			for (String c : cmd) {
				sb.append(c).append(' ');
			}
			LOG.debug("Executing command: {}", sb.toString());
		}

		try {
			RT.exec(new String[] { "mkfifo", pipe.getAbsolutePath() }).waitFor();
			process = RT.exec(cmd);
			dis = new DataInputStream(new FileInputStream(pipe));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("failed or interrupted I/O operations");
		} catch (IOException e) {
			throw new RuntimeException("failed or interrupted I/O operations");
		} catch (InterruptedException e) {
			throw new RuntimeException("failed or interrupted I/O operations");
		}
	}

	@Override
	public void close() {
		synchronized(this){
			if (!open.compareAndSet(true, false)) {
				return;
			}
	
			try {
				dis.close();
			} catch (IOException e) {
				throw new RuntimeException("failed or interrupted I/O operations");
			}
	
			process.destroy();
	
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				throw new RuntimeException("failed or interrupted I/O operations");
			}
	
			if (!pipe.delete()) {
				pipe.deleteOnExit();
			}
		}
	}

	@Override
	public void dispose() {
		if (disposed.compareAndSet(false, true) && open.get()) {
			close();
		}
	}

	@Override
	public boolean isOpen() {
		return open.get();
	}

	@Override
	public String toString() {
		return "video device " + name;
	}
}
