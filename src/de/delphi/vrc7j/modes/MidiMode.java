package de.delphi.vrc7j.modes;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import de.delphi.vrc7j.VRC7J;

public abstract class MidiMode {
	
	/*package*/ static final int MIDI_DATA_MASK=0x7f;
	
	protected VRC7J synth;
	
	public MidiMode(VRC7J synth) {
		this.synth=synth;
	}
	
	public void postMidiMessage(MidiMessage message) {
		MidiChannel[] channels=synth.getChannels();
		if(!(message instanceof ShortMessage))
			return;
		ShortMessage smsg=(ShortMessage) message;
		int status=smsg.getCommand();
		int channel=smsg.getChannel();
		byte[] data=message.getMessage();
		switch(status) {
		case ShortMessage.NOTE_ON:{
			channels[channel].noteOn(data[1], data[2]);
			break;
		}case ShortMessage.NOTE_OFF:{
			channels[channel].noteOff(data[1], data[2]);
			break;
		}case ShortMessage.PITCH_BEND:{
			channels[channel].setPitchBend(data[1]+(data[2]<<7));
			break;
		}case ShortMessage.POLY_PRESSURE:{
			channels[channel].setPolyPressure(data[1], data[2]);
			break;
		}case ShortMessage.CHANNEL_PRESSURE:{
			channels[channel].setChannelPressure(data[1]);
			break;
		}case ShortMessage.PROGRAM_CHANGE:{
			channels[channel].programChange(data[1]);
			break;
		}case ShortMessage.CONTROL_CHANGE:{
			channels[channel].controlChange(data[1], data[2]);
			break;
		}case ShortMessage.SYSTEM_RESET:{
			
			break;
		}
		}
	}
}
