package com.github.sarxos.webcam;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * /WebcamDriverUtils
 */
public class WebcamDriverUtils {

	private static final Logger LOG = LoggerFactory.getLogger(WebcamDriverUtils.class);
	
	static Class<?> clazz = null;
	String nome = null;

	private WebcamDriverUtils() {
	}

	/**
	 * Find webcam driver. Scan packages to search drivers specified in the
	 * argument.
	 * 
	 * @param names array of driver names to search for
	 * @return Driver if found or throw exception
	 * @throw WebcamException
	 */
	protected static WebcamDriver findDriver(List<String> names, List<Class<?>> classes) {

		for (String name : names) {

			LOG.info("Searching driver {}", name);

			Class<?> ObjClazz = null;

			for (Class<?> c : classes) {
				if (c.getCanonicalName().equals(name)) {
					ObjClazz = c;
					break;
				}
			}

			MetodoDiAppoggio(name);

			if (ObjClazz == null) {
				LOG.debug("Driver {} not found", name);
				continue;
			}

			LOG.info("Webcam driver {} has been found", name);

			// create new istance 
			return newIstance(ObjClazz);
		}

		return null;
	}

	private static void MetodoDiAppoggio(String name) {
		try {
			if (clazz == null) {
				clazz = Class.forName(name);
			}
		} catch (ClassNotFoundException e) {
			LOG.trace("Class not found {}, fall thru", name);
		}
	}

	// create new istance
	private static WebcamDriver newIstance(Class<?> parameterClazz){
		try {
			return (WebcamDriver) parameterClazz.newInstance();
		} catch (InstantiationException e) { System.err.println("Error in the Instantiation of the driver class for the webcam");
			System.exit(0);
		} catch (IllegalAccessException e) { System.err.println("The application does not have access to the definition of the specified constructor");
			System.exit(0);
		}
	}
	
	/**
	 * Scans all classes accessible from the context class loader which belong
	 * to the given package and subpackages.
	 * 
	 * @param packageName The base package
	 * @param flat scan only one package level, do not dive into subdirectories
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	protected static Class<?>[] getClasses(String pkgname, boolean flat) {

		List<File> dirs = new ArrayList<File>();
		List<Class<?>> classes = new ArrayList<Class<?>>();

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String path = pkgname.replace('.', '/');

		Enumeration<URL> resources = null;
		try {
			resources = classLoader.getResources(path);
		} catch (IOException e) { System.err.println("Cannot read path " + path);
			System.exit(0);
		}

		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			dirs.add(new File(resource.getFile()));
		}
		try {
			for (File directory : dirs) {
				classes.addAll(findClasses(directory, pkgname, flat));
			}
		} catch (ClassNotFoundException e) { System.err.println("Class not found");
			System.exit(0);
		}
		return classes.toArray(new Class<?>[classes.size()]);
	}

	/**
	 * Recursive method used to find all classes in a given directory and
	 * subdirectories.
	 * 
	 * @param dir base directory
	 * @param pkgname package name for classes found inside the base directory
	 * @param flat scan only one package level, do not dive into subdirectories
	 * @return Classes list
	 * @throws ClassNotFoundException
	 */
	private static List<Class<?>> findClasses(File dir, String pkgname, boolean flat) throws ClassNotFoundException {

		List<Class<?>> classes = new ArrayList<Class<?>>();
		if (!dir.exists()) {
			return classes;
		}

		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory() && !flat) {
				classes.addAll(findClasses(file, pkgname + "." + file.getName(), flat));
			} else if (file.getName().endsWith(".class")) {
				classes.add(Class.forName(pkgname + '.' + file.getName().substring(0, file.getName().length() - 6)));
			}
		}

		return classes;
	}
}
