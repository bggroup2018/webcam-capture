package com.github.sarxos.webcam.ds.ipcam;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.ds.ipcam.impl.IpCamDescriptor;


@XmlRootElement(name = "storage")
@XmlAccessorType(XmlAccessType.FIELD)
public class IpCamStorage {

	private static final Class<?>[] CLASSES = new Class<?>[] {
		IpCamStorage.class,
		IpCamDescriptor.class,
	};

	private static final JAXBContext CTX;
	static {
		JAXBContext c = null;
		try {
			c = JAXBContext.newInstance(CLASSES);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		} finally {
			CTX = c;
		}
	}

	@XmlElement(name = "ipcam")
	private List<IpCamDescriptor> descriptors = null;

	private transient File file = null;

	protected IpCamStorage() {
	}

	public IpCamStorage(String file) {
		this(new File(file));
	}

	public IpCamStorage(File file) {
		this.file = file;
	}

	protected List<IpCamDescriptor> getDescriptors() {
		return descriptors;
	}

	public void open() throws XMLStreamException {

		IpCamStorage storage = null;
		try {
			
			JAXBContext jc = JAXBContext.newInstance(IpCamStorage.class);

			XMLInputFactory xif = XMLInputFactory.newFactory();
			xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
			xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			XMLStreamReader xsr = xif.createXMLStreamReader(new StreamSource("file"));
			
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			storage = (IpCamStorage) unmarshaller.unmarshal(xsr);
			
		} catch (JAXBException e) {
			throw new WebcamException(e);
		}

		for (IpCamDescriptor d : storage.getDescriptors()) {
			IpCamDeviceRegistry.register(d.getName(), d.getURL(), d.getMode(), d.getAuth());
		}
	}
}
