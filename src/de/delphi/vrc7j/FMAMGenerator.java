package de.delphi.vrc7j;

/*package*/ class FMAMGenerator {

	private static final int TREMOLO_RATE=78;
	
	private int vibratoCounter=0;
	
	private int tremoloCounter=0;
	
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
	
	public int getTremolo() {	//TODO very wrong, temporary
		int tremValue=Operator.dbToLinear(Operator.logSin(tremoloCounter>>2));
		if(tremoloCounter>=(1<<11))
			tremValue=-tremValue;
		tremValue+=1<<11;
		tremValue=tremValue*13>>7;
		return tremValue;
	}

	public void update() {
		tremoloCounter=(tremoloCounter+TREMOLO_RATE) & 0xfffff;
		vibratoCounter=(vibratoCounter+1) & 0x1fff;
	}
}
