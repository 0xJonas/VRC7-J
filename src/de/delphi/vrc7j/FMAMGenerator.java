package de.delphi.vrc7j;

/*package*/ class FMAMGenerator {
	
	private int vibratoCounter=0;
	
	private int tremoloInc=1,tremoloVal=0;
	
	public FMAMGenerator() {
		
	}
	
	public int getVibrato(int fNum,int octave) {
		int inc=0;
		if((vibratoCounter & (1<<11))!=0) {
			inc=fNum>>6;
		}
		if((vibratoCounter & (1<<10))!=0) {
			inc=fNum>>7;
		}
		
		if(vibratoCounter>=0x1000) {
			inc=-inc;	// actual 2s-complement, not just bit inversion
		}
		return inc<<(octave+1);		//one less then the octave shift from fNum
	}
	
	public int getTremolo() {
		return tremoloVal>>3;
	}

	public void update() {
		vibratoCounter=(vibratoCounter+1) & 0x1fff;
		
		if((vibratoCounter & 0x3f)==0) {
			tremoloVal+=tremoloInc;
			if(tremoloVal>=0x69 || tremoloVal<=0) {
				tremoloInc*=-1;
			}
		}
	}
}
