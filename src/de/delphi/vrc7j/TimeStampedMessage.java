package de.delphi.vrc7j;

import javax.sound.midi.MidiMessage;

/*package*/ class TimeStampedMessage implements Comparable<TimeStampedMessage>{
	
	public MidiMessage message;
	
	public long timeStamp;
	
	public long index;
	
	public TimeStampedMessage(MidiMessage message, long timeStamp,long index) {
		this.message=message;
		this.timeStamp=timeStamp;
		this.index=index;
	}

	@Override
	public int compareTo(TimeStampedMessage o) {
		if(timeStamp!=o.timeStamp)
			return (int) (timeStamp-o.timeStamp);
		return (int) (index-o.index);
	}

}