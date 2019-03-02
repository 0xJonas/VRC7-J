package de.delphi.vrc7j;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiMessage;

/*package*/ class VRC7Receiver implements MidiDeviceReceiver {
	
	private boolean closed=false;
	
	private VRC7J synth;
	
	/*package*/ VRC7Receiver(VRC7J synth){
		this.synth=synth;
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		if(closed)
			throw new IllegalStateException("Receiver is closed.");
		if(!synth.isOpen())
			throw new IllegalStateException("Underlying synthesizer was closed");
		synth.handleMessage(message, timeStamp);
	}

	@Override
	public void close() {
		closed=true;
	}
	
	/*package*/ boolean isClosed() {
		return closed;
	}

	@Override
	public MidiDevice getMidiDevice() {
		return synth;
	}

}
