package de.delphi.vrc7j;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.spi.MidiDeviceProvider;

public class VRC7DeviceProvider extends MidiDeviceProvider {

	@Override
	public Info[] getDeviceInfo() {
		return new MidiDevice.Info[]{VRC7J.DEVICE_INFO};
	}

	@Override
	public MidiDevice getDevice(Info info) {
		if(!info.equals(VRC7J.DEVICE_INFO))
			throw new IllegalArgumentException("Device is not supported");
		return new VRC7J();
	}

}
