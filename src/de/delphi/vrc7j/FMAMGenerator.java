package de.delphi.vrc7j;

/*package*/ class FMAMGenerator {

	private static final int TREMOLO_RATE=78,VIBRATO_RATE=128;
	
	private int vibratoCounter=0;
	
	private int tremoloCounter=0;
	
	public FMAMGenerator() {
		
	}
	
	public int getVibrato(int fNum,int octave) {
		int inc=fNum>>(0b111^octave);
		int mult=(vibratoCounter>>13) & 0b11111;
		if((vibratoCounter & (1<<18))!=0) {
			inc*=0b11111^mult;
		}else {
			inc*=mult;
		}
		inc>>>=5;
		if((vibratoCounter & (1<<19))!=0) {
			return -inc;
		}else {
			return inc;
		}
	}
	
	public int getTremolo() {
		int tremValue=Operator.dbToLinear(Operator.logSin(tremoloCounter>>2));
		if(tremoloCounter>=(1<<19))
			tremValue=-tremValue;
		tremValue+=1<<20;
		tremValue=tremValue*13>>17;
		return tremValue;
	}

	public void update() {
		tremoloCounter=(tremoloCounter+TREMOLO_RATE) & 0xfffff;
		vibratoCounter=(vibratoCounter+VIBRATO_RATE) & 0xfffff;
	}
}
