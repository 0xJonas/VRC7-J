package de.delphi.vrc7j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.spi.SoundbankReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class VRC7SoundbankReader extends SoundbankReader {
	
	public VRC7SoundbankReader() {
		
	}
	
	/*
	<VRC7Soundbank>
		<meta>
			<name>Name</name>
			<version>version</version>
			<author>Author</author>
			<description>Description</description>
		</meta>
		<instruments>
			<VRC7Instrument>
				<meta>
					<name>Name</name>
					<patch>patch</patch>
					<bank>Bank</bank>
				</meta>
				<Modulator
					mult=... (int 0-f)
					feedback=... (int 0-7)
					index=... (int 0-3f)
					rectify=... (bool)
					tremolo=... (bool)
					vibrato=... (bool)
					attack=... (int 0-f)
					decay=... (int 0-f)
					sustain=... (int 0-f)
					release=... (int 0-f)
					sustained=... (bool)
					keyScaleRate=... (bool)
					keyScaleLevel=... (int 0-3)
				/>
				<Carrier
					mult=... (int 0-f)
					rectify=... (bool)
					tremolo=... (bool)
					vibrato=... (bool)
					attack=... (int 0-f)
					decay=... (int 0-f)
					sustain=... (int 0-f)
					release=... (int 0-f)
					sustained=... (bool)
					keyScaleRate=... (bool)
					keyScaleLevel=... (int 0-3)
				/>
			</VRC7Instrument>
		</instruments>
	</VRC7Soundbank>
	*/

	@Override
	public Soundbank getSoundbank(URL url) throws InvalidMidiDataException, IOException {
		return getSoundbank(url.openStream());
	}

	@Override
	public Soundbank getSoundbank(File file) throws InvalidMidiDataException, IOException {
		return getSoundbank(new FileInputStream(file));
	}

	@Override
	public Soundbank getSoundbank(InputStream stream) throws InvalidMidiDataException, IOException {
		Document doc=null;
		try {
			DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(true);
			DocumentBuilder builder=factory.newDocumentBuilder();
			doc=builder.parse(stream);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: malformed XML file.");
		}
		Element root=doc.getDocumentElement();
		NodeList metas=root.getElementsByTagName("meta");
		if(metas.getLength()==0)
			throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: <meta> section missing.");
		Element meta=(Element) metas.item(0);
		
		VRC7Soundbank base=parseSoundbankMetaTag(meta);
		
		NodeList maybeInstruments=root.getElementsByTagName("instruments");
		if(maybeInstruments.getLength()==0)
			throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: <instruments> section missing.");
		NodeList instruments=maybeInstruments.item(0).getChildNodes();
		
		for(int i=0;i<instruments.getLength();i++) {
			if(!(instruments.item(i) instanceof Element))
				continue;
			Element instrumentTag=(Element) instruments.item(i);
			if(instrumentTag.getTagName().equals("VRC7Instrument")) {
				VRC7Instrument instrument=parseInstrument(base,instrumentTag);
				base.addInstrument(instrument);
			}
		}
		
		return base;
	}
	
	private VRC7Soundbank parseSoundbankMetaTag(Element meta) throws InvalidMidiDataException{
		NodeList names=meta.getElementsByTagName("name");
		if(names.getLength()==0)
			throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: soundbank name is missing.");
		String name=names.item(0).getTextContent();
		
		NodeList versions=meta.getElementsByTagName("version");
		if(versions.getLength()==0)
			throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: soundbank version is missing.");
		String version=versions.item(0).getTextContent();
		
		NodeList authors=meta.getElementsByTagName("author");
		if(authors.getLength()==0)
			throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: soundbank author is missing.");
		String author=authors.item(0).getTextContent();
		
		NodeList descriptions=meta.getElementsByTagName("description");
		String description="";
		if(descriptions.getLength()!=0)
			description=descriptions.item(0).getTextContent();
		
		return new VRC7Soundbank(name,version,author,description);
	}
	
	private VRC7Instrument parseInstrumentMetaTag(VRC7Soundbank sb,Element meta) throws InvalidMidiDataException{
		NodeList names=meta.getElementsByTagName("name"),
				patches=meta.getElementsByTagName("patch"),
				banks=meta.getElementsByTagName("bank");
		String name="";
		int patch,bank;
		if(names.getLength()==0) {
			name="";
		}else {
			name=names.item(0).getTextContent();
		}
		
		try {
			if(patches.getLength()==0)
				throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: no preset value for instrument "+name);
			patch=Integer.parseInt(patches.item(0).getTextContent());
			
			if(banks.getLength()==0)
				throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: no bank value for instrument "+name);
			bank=Integer.parseInt(banks.item(0).getTextContent());
		}catch(NumberFormatException e) {
			throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: bad preset/bank value for instrument "+name);
		}
		
		return sb.createInstrument(new Patch(bank,patch),name);
	}
	
	private int parseIntAttribute(Element node,String name,int min,int max,int defaultt) throws InvalidMidiDataException{
		String strValue=node.getAttribute(name);
		if(strValue.isEmpty())
			return defaultt;
		int value=0;
		try {
			value=Integer.parseInt(strValue.trim());
		}catch(NumberFormatException e) {
			throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: bad attribute value for '"+name+"': "+strValue);
		}
		if(value<min || value>max)
			throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: attribute '"+name+"' out of range.");
		
		return value;
	}
	
	private boolean parseBooleanAttribute(Element node,String name,boolean defaultt) throws InvalidMidiDataException{
		String strValue=node.getAttribute(name);
		if(strValue.isEmpty())
			return defaultt;
		strValue=strValue.trim();
		if(strValue.toLowerCase().equals("true"))
			return true;
		else if(strValue.toLowerCase().equals("false"))
			return false;
		else
			throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: bad attribute value for '"+name+"': "+strValue);
	}

	private VRC7Instrument parseInstrument(VRC7Soundbank sb,Element instrumentNode) throws InvalidMidiDataException{
		NodeList metaTags=instrumentNode.getElementsByTagName("meta");
		if(metaTags.getLength()==0)
			throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: <meta> tag for instrument missing.");
		VRC7Instrument base=parseInstrumentMetaTag(sb,(Element) metaTags.item(0));
		
		NodeList modulators=instrumentNode.getElementsByTagName("Modulator");
		if(modulators.getLength()==0)
			throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: <Modulator> tag for instrument "+base.getName()+" missing.");
		Element modulator=(Element) modulators.item(0);
		
		NodeList carriers=instrumentNode.getElementsByTagName("Carrier");
		if(carriers.getLength()==0)
			throw new InvalidMidiDataException("File is not a valid VRC7-J soundbank: <Carrier> tag for instrument "+base.getName()+" missing.");
		Element carrier=(Element) carriers.item(0);
		
		base.modMult=parseIntAttribute(modulator,"mult",0,0xf,1);
		base.feedback=parseIntAttribute(modulator,"feedback",0,0x7,0);
		base.index=parseIntAttribute(modulator,"index",0,0x3f,1);
		base.modAttack=parseIntAttribute(modulator,"attack",0,0xf,0);
		base.modDecay=parseIntAttribute(modulator,"decay",0,0xf,0);
		base.modSustainLevel=parseIntAttribute(modulator,"sustain",0,0xf,0);
		base.modRelease=parseIntAttribute(modulator,"release",0,0xf,0);
		base.modSustained=parseBooleanAttribute(modulator,"sustained",false);
		base.modRect=parseBooleanAttribute(modulator,"rectify",false);
		base.modTremolo=parseBooleanAttribute(modulator,"tremolo",false);
		base.modVibrato=parseBooleanAttribute(modulator,"vibrato",false);
		base.modKeyScaleRate=parseBooleanAttribute(modulator,"keyScaleRate",false);
		base.modKeyScaleLevel=parseIntAttribute(modulator,"keyScaleLevel",0,3,0);
	
		base.carMult=parseIntAttribute(carrier,"mult",0,0xf,1);
		base.carAttack=parseIntAttribute(carrier,"attack",0,0xf,0);
		base.carDecay=parseIntAttribute(carrier,"decay",0,0xf,0);
		base.carSustainLevel=parseIntAttribute(carrier,"sustain",0,0xf,0);
		base.carRelease=parseIntAttribute(carrier,"release",0,0xf,0);
		base.carSustained=parseBooleanAttribute(carrier,"sustained",false);
		base.carRect=parseBooleanAttribute(carrier,"rectify",false);
		base.carTremolo=parseBooleanAttribute(carrier,"tremolo",false);
		base.carVibrato=parseBooleanAttribute(carrier,"vibrato",false);
		base.carKeyScaleRate=parseBooleanAttribute(carrier,"keyScaleRate",false);
		base.carKeyScaleLevel=parseIntAttribute(carrier,"keyScaleLevel",0,3,0);
		
		return base;
	}
}
