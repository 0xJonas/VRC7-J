package de.delphi.vrc7j;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import de.delphi.vrc7j.modes.MidiMode;
import de.delphi.vrc7j.modes.PolyphonicMode;
import de.delphi.vrc7j.modes.StandardMode;

public class VRC7J implements Synthesizer {
		
	public static final MidiDevice.Info DEVICE_INFO=new VRC7Info();
	
	public static final int STANDARD=0,
							STRICT=1,
							POLYPHONIC=2,
							POLYPHONIC_STRICT=3;
	
	//Output sample rate
	private int sampleRate=44100;
	
	//Output gain. 1.0=100% (normal) 2.0=200% (max) anything above 2.0 can cause clipping.
	private double volume=1.0;
	
	private int mode=STANDARD;
	
	private int polyChannel=0;
	
	private MidiMode midiMode;
	
	//variables to convert VRC7 sample rate to selected rate
	private double cyclesPerSample=1.0,vrc7Clock=0.0;
	
	private long clock=0,startClock=0;
	
	private long messageCounter=0;
	
	private PriorityBlockingQueue<TimeStampedMessage> pendingMessages=new PriorityBlockingQueue<>();
	
	private FMAMGenerator fmam;
	
	private boolean opened=false,closed=false;
	
	private ArrayList<VRC7Receiver> receivers=new ArrayList<>();
	
	private ArrayList<Integer> soloChannels=new ArrayList<>();
	
	private VRC7Soundbank defaultSoundbank;
	
	private HashMap<PatchWrapper,Instrument> loadedInstruments=new HashMap<>(16);
	
	private VRC7Channel[] channels=new VRC7Channel[16];
	
	private Thread synthThread;
	
	private SourceDataLine outLine;
	
	private Resampler resampler;
	
	private DigitalFilter vrc7Hybrid,famicomFilter;
	
	private byte[] buffer;
	
	private int bufferPointer=0;
	
	private int bufferSize=1024;
	
	private int pitchBendRange=200;
	
	private double tuning=440.0;
	
	/*package*/ VRC7J() {
		this(44100);
	}
	
	/*package*/ VRC7J(int sampleRate){
		this.sampleRate=sampleRate;
		
		fmam=new FMAMGenerator();
		
		for(Instrument ins:getAvailableInstruments()) {
			loadInstrument(ins);
		}
		for(int i=0;i<channels.length;i++) {
			channels[i]=new VRC7Channel(this,i);
			channels[i].programChange(0,0);
		}
		
		//Low-pass at 2289.3Hz
		double alpha=2.0/14427.66871*Operator.OPERATOR_CLOCK;
		vrc7Hybrid=new DigitalFilter(new double[]{1/(1+alpha),1/(1+alpha)},new double[]{-(1-alpha)/(1+alpha)});
		
		//High-pass at 8Hz
		alpha=2.0/16/Math.PI;
		double beta=1.0/Operator.OPERATOR_CLOCK;
		famicomFilter=new DigitalFilter(new double[] {alpha/(alpha+beta),-alpha/(alpha+beta)},new double[] {-(beta-alpha)/(beta+alpha)});
	}
	
	/*package*/ void handleMessage(MidiMessage message,long timeStamp) {
		pendingMessages.add(new TimeStampedMessage(message,timeStamp,messageCounter));
		messageCounter++;
	}
	
	/*package*/ void addSoloChannel(int chNum) {
		if(!soloChannels.contains(chNum))
			soloChannels.add(chNum);
	}
	
	/*package*/ void removeSoloChannel(int chNum) {
		soloChannels.remove((Object) chNum);
	}
	
	/*package*/ Instrument getInstrument(Patch patch) {
		return loadedInstruments.get(new PatchWrapper(patch));
	}

	/*package*/ FMAMGenerator getFMAMGenerator() {
		return fmam;
	}
	
	private int fetchSample() {
		int sample=0;
		int num=0;
		for(int i=0;i<channels.length;i++) {
			boolean solo=!soloChannels.isEmpty();
			if((!solo && !channels[i].getMute()) || (solo && channels[i].getSolo())) {
				sample+=channels[i].fetchSample();
				num++;
			}else {	//discard output but still update the channel's state
				channels[i].fetchSample();
			}
		}
		fmam.update();
		return sample*4/num;	//TODO improve maybe?
	}
	
	//=============================
	//===== VRC7J Parameters ======
	//=============================
	
	public void setSampleRate(int sampleRate) {
		if(opened || closed)
			throw new UnsupportedOperationException("Sample rate must be set before the device is opened.");
		this.sampleRate=sampleRate;
	}
	
	public int getSampleRate() {
		return sampleRate;
	}
	
	public void setVolume(double vol) {
		this.volume=vol;
	}
	
	public double getVolume() {
		return volume;
	}
	
	public void setMode(int mode) {
		if(opened || closed)
			throw new UnsupportedOperationException("Mode must be set before the device is opened.");
		this.mode=mode;
	}
	
	public int getMode() {
		return mode;
	}
	
	public void setPolyChannel(int channel) {
		if(opened || closed)
			throw new UnsupportedOperationException("Polyphonic channel must be set before the device is opened.");
		if(channel<0 || channel>15)
			throw new IllegalArgumentException("Polyphonic channel must be in range 0 to 15");
		this.polyChannel=channel;
	}
	
	public int getPolyChannel() {
		return polyChannel;
	}
	
	public void setBufferSize(int bufferSize) {
		if(opened || closed)
			throw new UnsupportedOperationException("Buffer size must be set before the device is opened.");
		this.bufferSize=bufferSize*2;	//Multiply by 2 to convert from samples to bytes
	}
	
	public int getBufferSize() {
		return bufferSize/2;
	}
	
	public void setTuning(double tuning) {
		if(tuning<=0)
			throw new IllegalArgumentException("Tuning must be positive.");
		this.tuning=tuning;
	}
	
	public double getTuning() {
		return tuning;
	}
	
	public void setPitchBendRange(int range) {
		if(range<0)
			throw new IllegalArgumentException("Pitch bend range must be positive.");
		this.pitchBendRange=range;
	}
	
	public int getPitchBendRange() {
		return pitchBendRange;
	}
	
	//=============================
	//==== MidiDevice Methods =====
	//=============================

	@Override
	public Info getDeviceInfo() {
		return DEVICE_INFO;
	}

	@Override
	public void open() throws MidiUnavailableException {
		if(isOpen() && closed)
			throw new MidiUnavailableException("Device was already closed.");
				
		cyclesPerSample=(double) Operator.OPERATOR_CLOCK/(double) sampleRate;
		
		//Initialize mode
		switch(mode) {
		case STANDARD:
			midiMode=new StandardMode(this,15);
			break;
		case STRICT:
			midiMode=new StandardMode(this,5);
			break;
		case POLYPHONIC:
			midiMode=new PolyphonicMode(this,15,polyChannel);
			break;
		case POLYPHONIC_STRICT:
			midiMode=new PolyphonicMode(this,5,polyChannel);
			break;
		}
		
		//Initialize buffer
		buffer=new byte[bufferSize];
		
		synthThread=new Thread(()->{
			try {
				AudioFormat format=new AudioFormat((float) sampleRate,16,1,true,true);
				outLine=AudioSystem.getSourceDataLine(format);
				outLine.open(format,bufferSize*2);
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
			
			int resampleOrder=3;
			resampler=new Resampler(resampleOrder,Operator.OPERATOR_CLOCK,sampleRate);
						
			while(!Thread.currentThread().isInterrupted()) {
				//Check for midi messages
				while(pendingMessages.size()>0 && pendingMessages.peek().timeStamp<=clock) {
					midiMode.postMidiMessage(pendingMessages.poll().message);
				}
				
				//Fill buffer with as many samples as needed
				while(vrc7Clock<cyclesPerSample) {
					vrc7Hybrid.addSample(fetchSample());
					famicomFilter.addSample(vrc7Hybrid.fetchSample());
					resampler.addSample(famicomFilter.fetchSample());
					vrc7Clock+=1.0;
				}
				vrc7Clock-=cyclesPerSample;
				
				clock+=(int) 1000000000.0/sampleRate;
				
				int sample=(short) (resampler.fetchSample()*volume/2.0);
				buffer[bufferPointer++]=(byte) (sample>>8);
				buffer[bufferPointer++]=(byte) (sample & 0xff);
				
				if(bufferPointer>=bufferSize) {
					int written=0;
					while(written<bufferSize)
						written+=outLine.write(buffer, written, bufferSize);
					if(!outLine.isRunning())
						outLine.start();
					bufferPointer=0;
					
					//Re-sync clock
					clock=System.nanoTime()-startClock;
				}
			}
			outLine.close();
		});
		synthThread.setName("VRC7-J Synthesizer");
		synthThread.setDaemon(true);
		startClock=System.nanoTime();
		synthThread.start();
		opened=true;
	}

	@Override
	public void close() {
		if(opened && !closed) {
			closed=true;
			for(VRC7Receiver rec:receivers) {
				rec.close();
			}
		}
	}

	@Override
	public boolean isOpen() {
		return opened && !closed;
	}

	@Override
	public long getMicrosecondPosition() {
		//Pretent to be 1 ms in the future so that the order of midi events depends only on their timestamps. Basically
		//reduces the likelihood to receive events that are in the past.
		return (long) (clock/1000+1000);
	}

	@Override
	public int getMaxReceivers() {
		return -1;	//unlimited
	}

	@Override
	public int getMaxTransmitters() {
		return 0;
	}

	@Override
	public Receiver getReceiver() throws MidiUnavailableException {
		VRC7Receiver rec=new VRC7Receiver(this);
		receivers.add(rec);
		return rec;
	}

	@Override
	public List<Receiver> getReceivers() {
		return new ArrayList<Receiver>(receivers);
	}

	@Override
	public Transmitter getTransmitter() throws MidiUnavailableException {
		throw new MidiUnavailableException("Synthesizer does not support Transmitters");
	}

	@Override
	public List<Transmitter> getTransmitters() {
		return new ArrayList<Transmitter>();
	}
	
	//==============================
	//==== Synthesizer Methods =====
	//==============================

	@Override
	public int getMaxPolyphony() {
		return 16;	//one note per channel
	}

	@Override
	public long getLatency() {
		return (long) (bufferSize/sampleRate*1000000);
	}

	@Override
	public MidiChannel[] getChannels() {
		return channels;
	}

	@Override
	public VoiceStatus[] getVoiceStatus() {
		VoiceStatus[] stats=new VoiceStatus[channels.length];
		for(int i=0;i<channels.length;i++) {
			stats[i]=channels[i].getVoiceStatus();
		}
		return stats;
	}

	@Override
	public boolean isSoundbankSupported(Soundbank soundbank) {
		return soundbank instanceof VRC7Soundbank;
	}

	@Override
	public boolean loadInstrument(Instrument instrument) {
		if(!isSoundbankSupported(instrument.getSoundbank()))
			throw new IllegalArgumentException("Instrument is not a valid VRC7-J instrument.");
		
		loadedInstruments.put(new PatchWrapper(instrument.getPatch()),instrument);
		return true;
	}

	@Override
	public void unloadInstrument(Instrument instrument) {
		if(!isSoundbankSupported(instrument.getSoundbank()))
			throw new IllegalArgumentException("Instrument is not a valid VRC7-J instrument.");
		
		loadedInstruments.remove(new PatchWrapper(instrument.getPatch()));
	}

	@Override
	public boolean remapInstrument(Instrument from, Instrument to) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Soundbank getDefaultSoundbank() {
		if(defaultSoundbank==null) {
			try {
				InputStream stream=getClass().getResourceAsStream("vrc7.vsb");
				defaultSoundbank=(VRC7Soundbank) MidiSystem.getSoundbank(stream);
			} catch (InvalidMidiDataException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return defaultSoundbank;
	}

	@Override
	public Instrument[] getAvailableInstruments() {
		return getDefaultSoundbank().getInstruments();
	}

	@Override
	public Instrument[] getLoadedInstruments() {
		Instrument[] ins=new Instrument[loadedInstruments.size()];
		loadedInstruments.values().toArray(ins);
		return ins;
	}

	@Override
	public boolean loadAllInstruments(Soundbank soundbank) {
		if(!isSoundbankSupported(soundbank))
			throw new IllegalArgumentException("Soundbank is not a valid VRC7-J soundbank.");
		
		for(Instrument i:soundbank.getInstruments()) {
			loadInstrument(i);
		}
		return true;
	}

	@Override
	public void unloadAllInstruments(Soundbank soundbank) {
		if(!isSoundbankSupported(soundbank))
			throw new IllegalArgumentException("Soundbank is not a valid VRC7-J soundbank.");
		
		for(Instrument i:soundbank.getInstruments()) {
			unloadInstrument(i);
		}
	}

	@Override
	public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
		if(!isSoundbankSupported(soundbank))
			throw new IllegalArgumentException("Soundbank is not a valid VRC7-J soundbank.");
		
		for(Patch p:patchList) {
			Instrument i=soundbank.getInstrument(p);
			if(i!=null) {
				loadInstrument(i);
			}
		}
		return true;
	}

	@Override
	public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
		if(!isSoundbankSupported(soundbank))
			throw new IllegalArgumentException("Soundbank is not a valid VRC7-J soundbank.");
		
		for(Patch p:patchList) {
			Instrument i=loadedInstruments.get(new PatchWrapper(p));
			if(p!=null && i.getSoundbank()==soundbank) {
				unloadInstrument(i);
			}
		}
	}
	
	private static class VRC7Info extends MidiDevice.Info {

		protected VRC7Info() {
			super("VRC7-J","Delphi1024", "Konami VRCVII synthsizer", "1.0");
		}
		
	}
}
