package de.delphi.vrc7j;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.Patch;
import javax.sound.midi.VoiceStatus;

public class VRC7Channel implements MidiChannel {
	
	private static final int 	BANK_SELECT_MSB=0x00,
								BANK_SELECT_LSB=0x20,
								CHANNEL_VOLUME_MSB=0x07,
								DAMPER_PEDAL=0x40,
								ALL_SOUND_OFF=0x78,
								RESET_ALL_CONTROLLERS=0x79,
								ALL_NOTES_OFF=0x7b;
						
	private static final int[] FEEDBACK_SHIFT={20,7,6,5,4,3,2,1};
	
	private static final int[] MULT={1,2,4,6,8,10,12,14,16,18,20,20,24,24,30,30};
	
	private static final int[] KSL={0,64,80,90,96,102,106,110,112,116,118,120,122,124,126,127};
	
	private static final double[] octaveBounds={31.78545, 63.5709, 127.1418, 254.28361, 508.56722, 1017.13443, 2034.26886};
	
	private VRC7J synth;
	
	private int channelNum;
	
	private boolean mute=false,solo=false;
	
	private int note,velocity;
	
	//Only used for get methods
	private int pressure=0;
		
	private int program=0;

	private int pitchBend=0x1fff;
	
	private VRC7Instrument instrument;
	
	//Controllers
	private int bank=0;
	private int channelVolume=0x7f;
	private boolean sustain=false;
	
	//VRC7 stuff
	private Envelope2 modEnv,carEnv;
	
	private Operator modulator,carrier;
	
	private int feedback,index;
	
	private boolean modVibrato,modTremolo;
	
	private boolean carVibrato,carTremolo;
	
	private int modKeyScaleLevel,carKeyScaleLevel;
	
	private int modPrev;
	
	private int fNum,octave;
	
	/*pacakge*/ VRC7Channel(VRC7J synth,int channelNum){
		this.synth=synth;
		this.channelNum=channelNum;
		modulator=new Operator();
		carrier=new Operator();
		modEnv=new Envelope2();
		carEnv=new Envelope2();
	}
	
	public int getChannelNumber() {
		return channelNum;
	}
	
	public VoiceStatus getVoiceStatus() {
		VoiceStatus status=new VoiceStatus();
		status.active=note>=0;
		status.bank=bank;
		status.channel=channelNum;
		status.note=note;
		status.program=program;
		status.volume=velocity;
		return status;
	}
	
	public boolean isAudible() {
		return carEnv.getState()!=Envelope.IDLE;
	}
	
	/*package*/ int fetchSample() {
		//Calculate vibrato, tremolo and keyscale, since their values are the same for both modulator and carrier
		FMAMGenerator fmam=synth.getFMAMGenerator();
		int vibValue=fmam.getVibrato(fNum, octave);
		int tremValue=fmam.getTremolo();
		int kslValue=Math.max(KSL[fNum>>5]-0b1000+octave,0);
		
		int modFreq=0;
		//Apply feedback to modulator
		if(feedback!=0)
			modFreq=(modPrev>>FEEDBACK_SHIFT[feedback]);
		
		//Apply vibrato to modulator
		int modVib=0;
		if(modVibrato)
			modVib=vibValue;
		
		//get modulator envelope
		int modAmp=modEnv.fetchEnvelope()<<5;
		
		//get modulator attenuation.
		modAmp+=index<<5;
		
		//Apply tremolo to modulator
		if(modTremolo)
			modAmp+=tremValue;
		
		//Apply key level scaling to modulator
		if(modKeyScaleLevel!=0)
			modAmp+=(kslValue>>(0b11^modKeyScaleLevel));
		
		//Get modulator value.
		int modValue=modulator.fetchSample(modFreq,modVib, modAmp);
		if(modEnv.getState()==Envelope.IDLE)
			modValue=0;
		modPrev=modValue;
		
		//Apply modulation
		int carFreq=modValue;
		
		//Apply vibrato to carrier
		int carVib=0;
		if(carVibrato)
			carVib=vibValue;
		
		//Get carrier envelope
		int carAmp=carEnv.fetchEnvelope()<<5;
		
		//Apply MIDI note velocity
		carAmp+=velocity<<7;
		
		//Apply tremolo to carrier
		if(carTremolo)
			carAmp+=tremValue;
		
		//Apply key scaling to carrier
		if(carKeyScaleLevel!=0)
			modAmp+=(kslValue>>(0b11^carKeyScaleLevel));
		
		//Get carrier value
		int carValue=carrier.fetchSample(carFreq, carVib, carAmp);
		if(carEnv.getState()==Envelope.IDLE)
			carValue=0;

		return carValue>>5;
	}
	
	private static int MidiToVRC7Volume(int velocity,int channelVolume) {
		double volume=15.0-velocity/100.0*15;	//Use velocity/volume 100 as 100%
		volume+=15.0-channelVolume/100.0*15;
		volume=Math.min(Math.max(volume,0.0),15.0);
		return (int) Math.round(volume);
	}
	
	private static int freqToFNum(double freq,int octave) {
		int val=(int) (freq*(1<<18)/Operator.OPERATOR_CLOCK/(1<<(octave-1)));
		if(val>0x1ff)
			return 0x1ff;
		return val;
	}
	
	private static int freqToOctave(double freq) {
		int octave=7;
		for(int i=6;i>=0;i--) {
			if(freq>octaveBounds[i])
				break;
			octave--;
		}
		return octave;
	}
	
	private static int restrict7Bit(int val) {
		if(val>127)
			return 127;
		else if(val<0)
			return 0;
		else
			return val;
	}
	
	private void reloadInstrument() {
		feedback=instrument.feedback;
		index=instrument.index;
		
		modVibrato=instrument.modVibrato;
		modTremolo=instrument.modTremolo;
		carVibrato=instrument.carVibrato;
		carTremolo=instrument.carTremolo;
		
		modKeyScaleLevel=instrument.modKeyScaleLevel;
		carKeyScaleLevel=instrument.carKeyScaleLevel;
		
		//Update modulator params
		modulator.setRectify(instrument.modRect);
		modulator.setMultiplier(MULT[instrument.modMult]);

		//Update carrier params
		carrier.setRectify(instrument.carRect);
		carrier.setMultiplier(MULT[instrument.carMult]);
		
		//Update modulator envelope
		modEnv.setRates(
				instrument.modAttack,
				instrument.modDecay,
				instrument.modSustainLevel,
				instrument.modRelease,
				instrument.modSustained,
				instrument.modKeyScaleRate
			);
		
		//Update carrier Envelope
		carEnv.setRates(
				instrument.carAttack,
				instrument.carDecay,
				instrument.carSustainLevel,
				instrument.carRelease,
				instrument.carSustained,
				instrument.carKeyScaleRate
			);
	}

	@Override
	public void noteOn(int noteNumber, int vel) {
		noteNumber=restrict7Bit(noteNumber);
		vel=restrict7Bit(vel);
		if(vel==0) {
			noteOff(noteNumber);
			return;
		}
		if(note>114) {	//Out of range for VRC7 when using standard pitch (fnum would be >9bit)
			return;
		}
		pressure=vel;
		
		this.note=noteNumber;
		this.velocity=MidiToVRC7Volume(vel,channelVolume);

		double delta=((double) pitchBend/0x1fff-1.0)*synth.getPitchBendRange();
		double freq=synth.getTuning()*Math.pow(2, (noteNumber-69)/12.0);
		freq=freq*Math.pow(2, delta/1200);
		
		//Calculate F-number and octave bits
		octave=freqToOctave(freq);
		fNum=freqToFNum(freq,octave);
		
		reloadInstrument();
		
		//Reset channel components
		modPrev=0;
		
		modulator.setFNumber(fNum, octave);
		modEnv.setFNumber(fNum, octave);
		modEnv.start();
		modulator.start();
		
		carrier.setFNumber(fNum, octave);
		carEnv.setFNumber(fNum, octave);
		carEnv.start();
		carrier.start();
	}

	@Override
	public void noteOff(int noteNumber, int velocity) {
		noteOff(noteNumber);
	}

	@Override
	public void noteOff(int noteNumber) {
		noteNumber=restrict7Bit(noteNumber);
		if(noteNumber==note) {
			if(instrument.modSustained) {
				modEnv.release();
			}
			carEnv.release();
			note=-1;
		}
	}

	@Override
	public void setPolyPressure(int noteNumber, int pressure) {
		noteNumber=restrict7Bit(noteNumber);
		pressure=restrict7Bit(pressure);
		setChannelPressure(pressure);	//Same as channel pressure since there is always only one note
	}

	@Override
	public int getPolyPressure(int noteNumber) {
		return getChannelPressure();
	}

	@Override
	public void setChannelPressure(int pressure) {
		pressure=restrict7Bit(pressure);
		this.pressure=pressure;
		velocity=MidiToVRC7Volume(pressure,channelVolume);
	}

	@Override
	public int getChannelPressure() {
		return pressure;
	}

	@Override
	public void controlChange(int controller, int value) {
		value=restrict7Bit(value);
		
		switch(controller) {
		case BANK_SELECT_MSB:{
			bank=value<<7;
			break;
		}case BANK_SELECT_LSB:{
			bank+=value;
			break;
		}case CHANNEL_VOLUME_MSB:{
			channelVolume=value;
			break;
		}case DAMPER_PEDAL:{
			sustain=value>=64;
			modEnv.setSustainNote(sustain);
			carEnv.setSustainNote(sustain);
			break;
		}case ALL_SOUND_OFF:{
			allSoundOff();
			break;
		}case RESET_ALL_CONTROLLERS:{
			resetAllControllers();
			break;
		}case ALL_NOTES_OFF:{
			allNotesOff();
			break;
		}
		}
	}

	@Override
	public int getController(int controller) {
		switch(controller) {
		case BANK_SELECT_MSB:{
			return (bank>>7) & 0x7f;
		}case BANK_SELECT_LSB:{
			return bank & 0x7f;
		}case CHANNEL_VOLUME_MSB:{
			return channelVolume;
		}case DAMPER_PEDAL:{
			return sustain ? 127:0;
		}
		}
		return 0;
	}

	@Override
	public void programChange(int program) {
		programChange(bank,program);
	}

	@Override
	public void programChange(int bank, int program) {
		bank=restrict7Bit(bank);
		program=restrict7Bit(program);
		//Load new instrument
		VRC7Instrument newInstrument=(VRC7Instrument) synth.getInstrument(new Patch(bank,program));
		
		if(newInstrument!=null) {
			this.bank=bank;
			this.program=program;
			instrument=newInstrument;
		}
	}

	@Override
	public int getProgram() {
		return program;
	}

	@Override
	public void setPitchBend(int bend) {
		if(bend>0x3fff)
			bend=0x3fff;
		else if(bend<0)
			bend=0;
		this.pitchBend=bend;

		double delta=((double) pitchBend/0x1fff-1.0)*synth.getPitchBendRange();
		
		double freq=synth.getTuning()*Math.pow(2, (note-69)/12.0);
		freq=freq*Math.pow(2, delta/1200);
		
		octave=freqToOctave(freq);
		fNum=freqToFNum(freq,octave);
		
		modulator.setFNumber(fNum,octave);
		modEnv.setFNumber(fNum, octave);
		carrier.setFNumber(fNum,octave);
		carEnv.setFNumber(fNum, octave);
	}

	@Override
	public synchronized int getPitchBend() {
		return pitchBend;
	}

	@Override
	public synchronized void resetAllControllers() {
		bank=0;
		channelVolume=0;
		sustain=false;
	}

	@Override
	public void allNotesOff() {
		noteOff(note,0);	//Only one note, so...
	}

	@Override
	public void allSoundOff() {
		allNotesOff();		//TODO
	}

	@Override
	public boolean localControl(boolean on) {
		return false;	//Always off
	}

	@Override
	public void setMono(boolean on) {
		//Mono is always on
	}

	@Override
	public boolean getMono() {
		return true;
	}

	@Override
	public void setOmni(boolean on) {
		//Always off
	}

	@Override
	public boolean getOmni() {
		return false;
	}

	@Override
	public void setMute(boolean mute) {
		this.mute=mute;
	}

	@Override
	public boolean getMute() {
		return mute;
	}

	@Override
	public void setSolo(boolean solo) {
		if(solo)
			synth.addSoloChannel(channelNum);
		else
			synth.removeSoloChannel(channelNum);
		this.solo=solo;
	}

	@Override
	public boolean getSolo() {
		return solo;
	}

}
