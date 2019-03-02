package de.delphi.vrc7j.modes;

import java.util.ArrayList;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import de.delphi.vrc7j.VRC7Channel;
import de.delphi.vrc7j.VRC7J;

public class PolyphonicMode extends MidiMode{
	
	private ArrayList<Pair> notes=new ArrayList<>();
	
	private int polyChannel=0,maxChannel=15;

	public PolyphonicMode(VRC7J synth,int maxChannel,int polyChannel) {
		super(synth);
		this.polyChannel=polyChannel;
		if(maxChannel<0 || maxChannel>=16)
			throw new IllegalArgumentException("maxChannel be a valid midi channel number.");
		this.maxChannel=maxChannel;
	}
	
	private void handleNoteOn(int note,int velocity) {
		VRC7Channel[] channels=(VRC7Channel[]) synth.getChannels();
		int channelNum=-1;
		for(int i=0;i<maxChannel;i++) {
			if(!channels[i].isAudible()) {
				channelNum=i;
				break;
			}
		}
		if(channelNum<0) {
			if(!notes.isEmpty()) {
				Pair pair=notes.get(0);
				channels[pair.channel].noteOff(pair.note);
				notes.remove(0);
				channelNum=pair.channel;
			}else {	//no notes are technically active but all envelopes are in their release phase (audible)
				channelNum=0;
			}
		}
		channels[channelNum].noteOn(note, velocity);
		notes.add(new Pair(channelNum,note));
	}
	
	private void handleNoteOff(int note,int velocity) {
		VRC7Channel[] channels=(VRC7Channel[]) synth.getChannels();
		for(int i=notes.size()-1;i>=0;i--) {
			Pair pair=notes.get(i);
			if(pair.note==note) {
				channels[pair.channel].noteOff(note,velocity);
				notes.remove(i);
			}
		}
	}
	
	private void handlePolyPressure(int note,int pressure) {
		VRC7Channel[] channels=(VRC7Channel[]) synth.getChannels();
		for(int i=0;i<notes.size();i++) {
			Pair pair=notes.get(i);
			if(pair.note==note) {
				channels[pair.channel].setPolyPressure(note,pressure);
				break;
			}
		}
	}
	
	@Override
	public void postMidiMessage(MidiMessage message) {
		if(!(message instanceof ShortMessage))
			return;

		VRC7Channel[] channels=(VRC7Channel[]) synth.getChannels();
		ShortMessage smsg=(ShortMessage) message;
		int data1=smsg.getData1();
		int data2=smsg.getData2();
		
		if(smsg.getChannel()!=polyChannel)
			return;
		
		switch(smsg.getCommand()) {
		case ShortMessage.NOTE_ON:{
			if(smsg.getData2()==0)
				handleNoteOff(data1,data2);
			else
				handleNoteOn(data1,data2);
			break;
		}case ShortMessage.NOTE_OFF:{
			handleNoteOff(data1,data2);
			break;
		}case ShortMessage.POLY_PRESSURE:{
			handlePolyPressure(data1,data2);
			break;
		}case ShortMessage.CHANNEL_PRESSURE:{
			for(VRC7Channel channel:channels) {
				channel.setChannelPressure(data1);
			}
			break;
		}case ShortMessage.PROGRAM_CHANGE:{
			for(VRC7Channel channel:channels) {
				channel.programChange(data1);
			}
			break;
		}case ShortMessage.PITCH_BEND:{
			for(VRC7Channel channel:channels) {
				channel.setPitchBend(data1+(data2<<7));
			}
			break;
		}case ShortMessage.CONTROL_CHANGE:{
			for(VRC7Channel channel:channels) {
				channel.controlChange(data1,data2);
			}
			break;
		}
		}
	}
	
	private class Pair {
		
		public int channel;
		
		public int note;
		
		public Pair(int channel,int note) {
			this.channel=channel;
			this.note=note;
		}
	}
}
