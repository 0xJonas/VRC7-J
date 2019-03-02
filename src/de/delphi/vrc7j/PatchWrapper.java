package de.delphi.vrc7j;

import javax.sound.midi.Patch;

/*package*/ class PatchWrapper {
	private Patch patch;
	
	public PatchWrapper(Patch patch){
		this.patch=patch;
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof PatchWrapper))
			return false;
		PatchWrapper other=(PatchWrapper) o;
		return patch.getBank()==other.patch.getBank() && patch.getProgram()==other.patch.getProgram();
	}
	
	@Override
	public int hashCode(){
		return patch.getBank()*128+patch.getProgram();
	}
}
