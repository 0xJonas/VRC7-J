package de.delphi.vrc7j;

import java.util.HashMap;

import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;

public class VRC7Soundbank implements Soundbank {
	
	private String name,version,vendor,description;
	
	private HashMap<PatchWrapper,VRC7Instrument> instruments=new HashMap<>();
	
	public VRC7Soundbank(String name,String version,String vendor,String description){
		this.name=name;
		this.version=version;
		this.vendor=vendor;
		this.description=description;
	}
	
	/*package*/ void addInstrument(VRC7Instrument instrument) {
		instruments.put(new PatchWrapper(instrument.getPatch()),instrument);
	}
	
	public VRC7Instrument createInstrument(Patch patch) {
		VRC7Instrument ins=new VRC7Instrument(this,patch,"");
		addInstrument(ins);
		return ins;
	}
	
	public VRC7Instrument createInstrument(Patch patch,String name) {
		VRC7Instrument ins=new VRC7Instrument(this,patch,name);
		addInstrument(ins);
		return ins;
	}
	
	public void setName(String name) {
		if(name==null)
			throw new NullPointerException("Name must not be null.");
		this.name=name;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setVersion(String version) {
		if(version==null)
			throw new NullPointerException("Version must not be null.");
		this.version=version;
	}
	
	@Override
	public String getVersion() {
		return version;
	}
	
	public void setVendor(String vendor) {
		if(vendor==null)
			throw new NullPointerException("Vendor must not be null.");
		this.vendor=vendor;
	}

	@Override
	public String getVendor() {
		return vendor;
	}

	public void setDescription(String description) {
		this.description=description;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public SoundbankResource[] getResources() {
		return new SoundbankResource[0];
	}

	@Override
	public Instrument[] getInstruments() {
		Instrument[] array=new Instrument[instruments.size()];
		instruments.values().toArray(array);
		return array;
	}

	@Override
	public Instrument getInstrument(Patch patch) {
		return instruments.get(new PatchWrapper(patch));
	}
}
