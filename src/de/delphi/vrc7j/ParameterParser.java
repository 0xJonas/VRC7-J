package de.delphi.vrc7j;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;

/*package*/ class ParameterParser {
	
	private static int sampleRate=44100,
						mode=VRC7J.STANDARD,
						loop=0,
						bufferSize=1024,
						pitchBend=200,
						polyChannel=0;
	
	private static double tuning=440.0,
						volume=1.0;
	
	private static volatile boolean trackEndReceived=false;
	
	private static MidiDevice midiIn;
	
	private static VRC7Soundbank soundbank;
	
	private static VRC7J synth;
	
	public static void printVersion() {
		String version=VRC7J.DEVICE_INFO.getVersion();
		String str= "VRC7-J --- A Java synthesizer for the Konami VRCVII. Written in 2019 by Delphi1024.\n"
				+ "Version "+version;
		System.out.println(str);
	}
	
	public static void printUsage() {
		String usage= "Usage: java -jar VRC7J.jar [options] filename\n"
					+ "\n"
					+ "===Available options===\n"
					+ "\t -s, --soundbank file\t Sets the soundbank that will be used by the synthesizer. Default is vrc7.vsb\n"
					+ "\t -r, --samplerate int\t Sets the sample rate. Default is 44100\n"
					+ "\t -v, --volume float%\t Sets the playback volume of the synthesizer. The highest volume without clipping\n"
					+ "\t\t\t\t is 200%. Default is 100%.\n"
					+ "\t -l, --loop int\t\t Sets how many times the file should be played. Use 0 for infinite looping.\n"
					+ "\t\t\t\t Default is 1.\n"
					+ "\t -m, --mode mode\t Sets the mode of the synthesizer. Available modes are listed below.\n"
					+ "\t\t\t\t Default is standard.\n"
					+ "\t -X <parameter>=<value>  Sets a special parameter. Available parameters and defaults are listed below.\n"
					+ "\n"
					+ "===Modes===\n"
					+ "\t standard\t\t   A slightly relaxed version of the actual working of the VRC7. Midi channels 0-15\n"
					+ "\t\t\t\t without channel 9 are all available with one note per channel. The resetting phase\n "
					+ "\t\t\t\t of the envelope is also removed.\n"
					+ "\t strict\t\t\t   The mode closest to the actual VRC7. Midi channels 0-5 are listening for\n"
					+ "\t\t\t\t events and events for other channels are ignored. One note is played per channel.\n"
					+ "\t\t\t\t The envelope also has a reset phase where it is set to it's lowest volume over time.\n"
					+ "\t\t\t\t This may be audible when a new note is played too shortly after the previous note was\n"
					+ "\t\t\t\t released.\n"
					+ "\t polyphonic\t\t   A variation of standard mode where all channels use the same instrument.\n"
					+ "\t\t\t\t Incoming events are, if possible, routed to a channel that is currently not\n"
					+ "\t\t\t\t playing a note. This channel may be different from the channel specified\n"
					+ "\t\t\t\t in the Midi event.\n"
					+ "\t polyphonic-strict\t Similar to polyphonic mode, but with the rules of strict mode instead of standard.\n"
					+ "\n"
					+ "===Special parameters===\n"
					+ "\t buffersize=<int>\t Sets the size of the synthesizers internal buffer in samples. Default is 1024.\n"
					+ "\t pitchbend=<int>\t Sets the range of pitch bend events in cents. Default is 200 cents (2 semitones).\n"
					+ "\t tuning=<float>\t\t Sets the frequency of note a4, which is used as the base for tuning. Default is 440.0\n"
					+ "\t polychannel=<int>\t Sets the channel that the synthesizer listens on in polyphonic mode. Default is 0.\n"
					+ "\t sequencer=<int>\t Selects a Sequencer from the available devices. Default is Real Time Sequencer.\n"
					+ "\n";
		System.out.println(usage);
		listDevices();
	}
	
	private static MidiDevice.Info[] getSortedDevices(){
		MidiDevice.Info[] infos=MidiSystem.getMidiDeviceInfo();
		Arrays.sort(infos,(a,b) -> 
			{
				return a.getName().compareTo(b.getName());
			});
		
		return infos;
	}
	
	private static void listDevices() {
		MidiDevice.Info[] infos=getSortedDevices();
		System.out.println("===Available Midi Devices===");
		for(int i=1;i<infos.length+1;i++) {
			System.out.print(i);
			System.out.print('\t');
			System.out.println(infos[i-1].toString());
			System.out.print("\t  ");
			System.out.println(infos[i-1].getDescription());
		}
	}
	
	private static void handleSoundbankParam(String param) {
		File file=new File(param);
		if(!file.exists()) {
			System.out.println("Soundbank file not found: "+param);
			throw new IllegalArgumentException();
		}
		try {
			Soundbank sb=MidiSystem.getSoundbank(file);
			if(!(sb instanceof VRC7Soundbank))
				throw new InvalidMidiDataException();
			soundbank=(VRC7Soundbank) sb;
		}catch (InvalidMidiDataException e) {
			System.out.println("File is not a valid VRC7-J soundbank: "+param);
			throw new IllegalArgumentException();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void handleSampleRateParam(String param) {
		try {
			sampleRate=Integer.parseInt(param);
		}catch(NumberFormatException e) {
			System.out.println("Bad value for --samplerate: "+param);
			throw new IllegalArgumentException();
		}
	}
	
	private static void handleVolumeParam(String param) {
		String val=param;
		if(param.charAt(param.length()-1)=='%')
			val=param.substring(0, param.length()-1);
		try {
			volume=Double.parseDouble(val)/100.0;
		}catch(NumberFormatException e) {
			System.out.println("Bad value for --samplerate: "+param);
			throw new IllegalArgumentException();
		}
	}
	
	private static void handleLoopParam(String param) {
		try {
			loop=Integer.parseInt(param)-1;
			if(loop==-1)
				loop=Sequencer.LOOP_CONTINUOUSLY;
		}catch(NumberFormatException e) {
			System.out.println("Bad value for --loop: "+param);
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Handles the --mode option.
	 * @param param value for the --mode option
	 */
	private static void handleModeParam(String param) {
		switch(param.toLowerCase()) {
		case "standard":{
			mode=VRC7J.STANDARD;
			break;
		}case "strict":{
			mode=VRC7J.STRICT;
			break;
		}case "polyphonic":{
			mode=VRC7J.POLYPHONIC;
			break;
		}case "polyphonic-strict":{
			mode=VRC7J.POLYPHONIC_STRICT;
			break;
		}default:{
			System.out.println("Bad value for --mode: "+param);
			throw new IllegalArgumentException();
		}
		}
	}
	
	private static void handleMidiIn(String param) {
		MidiDevice.Info[] devices=getSortedDevices();
		int device=Integer.parseInt(param)-1;
		
		//Check if value is in the correct range
		if(device>=0 && device<devices.length) {
			try {
				midiIn=MidiSystem.getMidiDevice(devices[device]);

				if(midiIn.getMaxTransmitters()==0) {
					System.out.println("Selected Midi device does not send midi messages: "+devices[device].getName());
					throw new IllegalArgumentException();
				}
			} catch (MidiUnavailableException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Handles special parameters of the form param=value.
	 * Used for non-standard options.
	 * @param param param=value string
	 */
	private static void handleOptionParam(String param) {
		//Check if parameter is formatted correctly 
		int split=param.indexOf('=');
		if(split<0) {
			System.out.println("Bad value for -X: "+param);
			throw new IllegalArgumentException();
		}
		
		//Get name and value
		String paramName=param.substring(0, split);
		String value=param.substring(split+1);
		
		try {
			//Handle parameter
			switch(paramName) {
			case "buffersize":{
				bufferSize=Integer.parseInt(value);
				break;
			}case "pitchbend":{
				pitchBend=Integer.parseInt(value);
				break;
			}case "tuning":{
				tuning=Float.parseFloat(value);
				break;
			}case "polychannel":{
				polyChannel=Integer.parseInt(value);
				if(polyChannel<0 || polyChannel>15) {
					System.out.println("Polyphonic channel is not a valid midi channel number: "+value);
					throw new IllegalArgumentException();
				}
				break;
			}default:{
				System.out.println("Unrecognized parameter: "+paramName);
				throw new IllegalArgumentException();
			}
			}
		}catch(NumberFormatException e) {
			System.out.println("Bad value for "+paramName+": "+value);
			throw new IllegalArgumentException();
		}
	}
	
	private static void handleMetaEvent(MetaMessage msg) {
		switch(msg.getType()) {
		case 0:{	//Sequence Number
			
			break;
		}case 0x01:{	//Text event
			System.out.println("META: "+new String(msg.getData()));
			break;
		}case 0x02:{ 	//Copyright notice
			System.out.println("COPYRIGHT NOTICE: "+new String(msg.getData()));
			break;
		}case 0x03:{	//Track name
			System.out.println("TRACK NAME: "+new String(msg.getData()));
			break;
		}case 0x04:{	//Instrument name
			System.out.println("INSTRUMENT NAME: "+new String(msg.getData()));
			break;
		}case 0x05:{	//Lyric
			System.out.print(new String(msg.getData()));
			System.out.print(" ");
			break;
		}case 0x06:{	//Marker
			System.out.println("MARKER: "+new String(msg.getData()));
			break;
		}case 0x07:{	//Cue point
			System.out.println("CUE POINT: "+new String(msg.getData()));
			break;
		}case 0x2f:{	//Track end
			trackEndReceived=true;
			break;
		}
		}
	}
	
	private static void playSequence(Sequencer sequencer) {
		while(!Thread.currentThread().isInterrupted()) {
			try {
				if(trackEndReceived) {
					Thread.sleep(1000);		//Wait for any notes to ring out
					if(!sequencer.isRunning())
						break;
					trackEndReceived=false;
				}else {
					Thread.sleep(100);
				}
			}catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * MAIN
	 */
	public static void main(String[] args) {
		if(args.length==0) {
			printVersion();
			System.out.println();
			printUsage();
			return;
		}
		
		Sequence seq=null;
		try {
			int index=0;
			while(index<args.length) {
				
				if(args[index].startsWith("-")) {	//Handle options
					//Check if the option actually has a value
					if(index+1>=args.length) {
						System.out.println("No argument supplied for "+args[index]);
						throw new IllegalArgumentException();
					}
					
					String command=args[index];
					index++;
					
					//Handle option
					switch(command) {
					case "-s": case "--soundbank":{
						handleSoundbankParam(args[index]);
						break;
					}case "-r": case "--samplerate":{
						handleSampleRateParam(args[index]);
						break;
					}case "-v": case "--volume":{
						handleVolumeParam(args[index]);
						break;
					}case "-l": case "--loop":{
						handleLoopParam(args[index]);
						break;
					}case "-m": case "--mode":{
						handleModeParam(args[index]);
						break;
					}case "-i": case "--midiin":{
						handleMidiIn(args[index]);
						break;
					}case "-X":{
						handleOptionParam(args[index]);
						break;
					}default:{
						System.out.println("Unrecognized option: "+args[index]);
						throw new IllegalArgumentException();
					}
					}
					
					index++;
				}else {		//Handle file
					File file=new File(args[index]);
					if(!file.exists())
						throw new IllegalArgumentException("File not found: "+args[index]);
					
					try {
						seq=MidiSystem.getSequence(file);
					}catch(IOException e) {
						e.printStackTrace();
					} catch (InvalidMidiDataException e) {
						System.out.println("File is not a valid Midi file: "+args[index]);
					}
					
					index++;
				}
			}
		}catch(IllegalArgumentException e) {	//Any wrong inputs will end here
			printUsage();
			return;
		}
		
		try {
			synth=(VRC7J) MidiSystem.getMidiDevice(VRC7J.DEVICE_INFO);
			
			//Set standard parameters
			if(soundbank!=null)
				synth.loadAllInstruments(soundbank);
			synth.setSampleRate(sampleRate);
			synth.setVolume(volume);
			synth.setMode(mode);
			
			//Set special parameters
			synth.setBufferSize(bufferSize);
			synth.setPitchBendRange(pitchBend);
			synth.setTuning(tuning);
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		}finally {
			synth.close();
		}
		
		try {
			if(midiIn==null) {
				midiIn=MidiSystem.getSequencer(false);
			}
			
			//Connect components
			synth.open();
			midiIn.getTransmitter().setReceiver(synth.getReceiver());
			if(midiIn instanceof Sequencer) {
				Sequencer sequencer=(Sequencer) midiIn;
				sequencer.setLoopCount(loop);
				if(seq==null) {
					System.out.println("Midi device is a sequencer but no input file was provided.");
					printUsage();
					return;
				}
				sequencer.open();
				sequencer.setSequence(seq);
				sequencer.addMetaEventListener(ParameterParser::handleMetaEvent);
				sequencer.start();
			}else {
				midiIn.open();
			}
		} catch (MidiUnavailableException | InvalidMidiDataException e) {
			e.printStackTrace();
		}
		
		if(midiIn instanceof Sequencer) {
			playSequence((Sequencer) midiIn);
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(()-> {
			midiIn.close();
			synth.close();
		}));
	}
}
