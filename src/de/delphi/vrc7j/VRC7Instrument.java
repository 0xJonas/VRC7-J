package de.delphi.vrc7j;

import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;

public class VRC7Instrument extends Instrument {
	
	private String name;
	
	//=====Carrier=====
	
	/*package*/ int carMult;

	/*package*/ int carAttack,carDecay,carSustainLevel,carRelease;
	
	/*package*/ boolean carSustained;
	
	/*package*/ boolean carRect;
	
	/*package*/ boolean carTremolo,carVibrato;

	/*package*/ boolean carKeyScaleRate;

	/*package*/ int carKeyScaleLevel;
	
	//=====Modulator=====
	
	/*package*/ int modMult;
	
	/*package*/ int feedback;
	
	/*package*/ int index;
	
	/*package*/ int modAttack,modDecay,modSustainLevel,modRelease;
	
	/*package*/ boolean modSustained;
	
	/*package*/ boolean modRect;

	/*package*/ boolean modTremolo,modVibrato;
	
	/*package*/ boolean modKeyScaleRate;
	
	/*package*/ int modKeyScaleLevel;
	
	/*package*/ VRC7Instrument(VRC7Soundbank sb,Patch patch,String name){
		super(sb,patch,name,null);
		this.name=name;
	}
	
	public void setName(String name) {
		this.name=name;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object getData() {
		// TODO Maybe VRC7 register values?
		return null;
	}
	
	///////////////////
	///// Carrier /////
	///////////////////
	
	public int getCarrierMult() {
		return carMult;
	}

	public void setCarrierMult(int carMult) {
		if(carMult<0 || carMult>15)
			throw new IllegalArgumentException("Multiplier must be in range 0 to 15.");
		this.carMult = carMult;
	}

	public int getCarrierAttack() {
		return carAttack;
	}

	public void setCarrierAttack(int carAttack) {
		if(carAttack<0 || carAttack>15)
			throw new IllegalArgumentException("Attack must be in range 0 to 15.");
		this.carAttack = carAttack;
	}

	public int getCarrierDecay() {
		return carDecay;
	}

	public void setCarrierDecay(int carDecay) {
		if(carDecay<0 || carDecay>15)
			throw new IllegalArgumentException("Decay must be in range 0 to 15.");
		this.carDecay = carDecay;
	}

	public int getCarrierSustainLevel() {
		return carSustainLevel;
	}

	public void setCarrierSustainLevel(int carSustainLevel) {
		if(carSustainLevel<0 || carSustainLevel>15)
			throw new IllegalArgumentException("Sustain level must be in range 0 to 15.");
		this.carSustainLevel = carSustainLevel;
	}

	public int getCarrierRelease() {
		return carRelease;
	}

	public void setCarrierRelease(int carRelease) {
		if(carRelease<0 || carRelease>15)
			throw new IllegalArgumentException("Release must be in range 0 to 15.");
		this.carRelease = carRelease;
	}

	public boolean getCarrierSustained() {
		return carSustained;
	}

	public void setCarrierSustained(boolean carSustained) {
		this.carSustained = carSustained;
	}

	public boolean getCarrierRectify() {
		return carRect;
	}

	public void setCarrierRectify(boolean carRect) {
		this.carRect = carRect;
	}

	public boolean getCarrierTremolo() {
		return carTremolo;
	}

	public void setCarrierTremolo(boolean carTremolo) {
		this.carTremolo = carTremolo;
	}

	public boolean getCarrierVibrato() {
		return carVibrato;
	}

	public void setCarrierVibrato(boolean carVibrato) {
		this.carVibrato = carVibrato;
	}

	public boolean getCarrierKeyScaleRate() {
		return carKeyScaleRate;
	}

	public void setCarrierKeyScaleRate(boolean carKeyScaleRate) {
		this.carKeyScaleRate = carKeyScaleRate;
	}

	public int getCarrierKeyScaleLevel() {
		return carKeyScaleLevel;
	}

	public void setCarrierKeyScaleLevel(int carKeyScaleLevel) {
		if(carKeyScaleLevel<0 || carKeyScaleLevel>3)
			throw new IllegalArgumentException("Key scale level must be in range 0 to 3.");
		this.carKeyScaleLevel = carKeyScaleLevel;
	}

	/////////////////////
	///// Modulator /////
	/////////////////////
	
	public int getModulatorMult() {
		return modMult;
	}

	public void setModulatorMult(int modMult) {
		if(modMult<0 || modMult>15)
			throw new IllegalArgumentException("Multiplier must be in range 0 to 15.");
		this.modMult = modMult;
	}
	
	public int getFeedback() {
		return feedback;
	}

	public void setFeedback(int feedback) {
		if(feedback<0 || feedback>7)
			throw new IllegalArgumentException("Feedback must be in range 0 to 7.");
		this.feedback = feedback;
	}

	public int getTotalLevel() {
		return index;
	}

	public void setTotalLevel(int level) {
		if(level<0 || level>63)
			throw new IllegalArgumentException("Total level must be in range 0 to 63.");
		this.index = level;
	}

	public int getModulatorAttack() {
		return modAttack;
	}

	public void setModulatorAttack(int modAttack) {
		if(modAttack<0 || modAttack>15)
			throw new IllegalArgumentException("Attack must be in range 0 to 15.");
		this.modAttack = modAttack;
	}

	public int getModulatorDecay() {
		return modDecay;
	}

	public void setModulatorDecay(int modDecay) {
		if(modDecay<0 || modDecay>15)
			throw new IllegalArgumentException("Decay must be in range 0 to 15.");
		this.modDecay = modDecay;
	}

	public int getModulatorSustainLevel() {
		return modSustainLevel;
	}

	public void setModulatorSustainLevel(int modSustainLevel) {
		if(modSustainLevel<0 || modSustainLevel>15)
			throw new IllegalArgumentException("Sustain level must be in range 0 to 15.");
		this.modSustainLevel = modSustainLevel;
	}

	public int getModulatorRelease() {
		return modRelease;
	}

	public void setModulatorRelease(int modRelease) {
		if(modRelease<0 || modRelease>15)
			throw new IllegalArgumentException("Release must be in range 0 to 15.");
		this.modRelease = modRelease;
	}

	public boolean getModulatorSustained() {
		return modSustained;
	}

	public void setModulatorSustained(boolean modSustained) {
		this.modSustained = modSustained;
	}

	public boolean getModulatorRectify() {
		return modRect;
	}

	public void setModulatorRectify(boolean modRect) {
		this.modRect = modRect;
	}

	public boolean getModulatorTremolo() {
		return modTremolo;
	}

	public void setModulatorTremolo(boolean modTremolo) {
		this.modTremolo = modTremolo;
	}

	public boolean getModulatorVibrato() {
		return modVibrato;
	}

	public void setModulatorVibrato(boolean modVibrato) {
		this.modVibrato = modVibrato;
	}

	public boolean getModulatorKeyScaleRate() {
		return modKeyScaleRate;
	}

	public void setModulatorKeyScaleRate(boolean modKeyScaleRate) {
		this.modKeyScaleRate = modKeyScaleRate;
	}

	public int getModulatorKeyScaleLevel() {
		return modKeyScaleLevel;
	}

	public void setModulatorKeyScaleLevel(int modKeyScaleLevel) {
		if(modKeyScaleLevel<0 || modKeyScaleLevel>3)
			throw new IllegalArgumentException("Key scale level must be in range 0 to 3.");
		this.modKeyScaleLevel = modKeyScaleLevel;
	}
}
