package de.delphi.vrc7j.modes;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import de.delphi.vrc7j.VRC7J;

public class StandardMode extends MidiMode{
	
	private int[] channelMapping=new int[16];
	
	public StandardMode(VRC7J synth,int maxChannel) {
		super(synth);
		for(int i=0;i<channelMapping.length;i++)
			channelMapping[i]=i%(maxChannel+1);
	}
	
	@Override
	public void postMidiMessage(MidiMessage message) {
		try {
			if(message instanceof ShortMessage) {
				ShortMessage smsg=(ShortMessage) message;
				smsg.setMessage(smsg.getCommand(), channelMapping[smsg.getChannel()], smsg.getData1(), smsg.getData2());
				super.postMidiMessage(smsg);
			}else {
				super.postMidiMessage(message);
			}
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}
	
}
