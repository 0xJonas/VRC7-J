package de.delphi.vrc7j;

/*package*/ class Envelope {
	
	public static  final int ATTACK=0,
							DECAY=1,
							SUSTAIN=2,
							RELEASE=3,
							IDLE=4;
							//SETTLE=5;  //Does this exist on the ym2413 ?
	
	private static final int CYCLE_MASK=(1<<23)-1;
	
	private static final int RATE_14=4,
							RATE_15=8,
							RATE_DEFAULT=9;
	
	private static final int[][] INCREMENTS={{0,1,0,1,0,1,0,1},	//Rates 1-13 
											 {0,1,0,1,1,1,0,1},
	                                         {0,1,1,1,0,1,1,1},
	                                         {0,1,1,1,1,1,1,1},
	                                         {1,1,1,1,1,1,1,1},	//Rate 14 0
	                                         {1,1,1,2,1,1,1,2},	//Rate 14 1
	                                         {1,2,1,2,1,2,1,2},	//Rate 14 2
	                                         {1,2,2,2,1,2,2,2},	//Rate 14 3
	                                         {2,2,2,2,2,2,2,2},	//Rate 15
	                                         {0,0,0,0,0,0,0,0}};//Used for non-percussive sustain/idle
	
	private int  attackRate,decayRate,sustainLevel,releaseRate;
	
	private int attackShift,decayShift,releaseShift,sustainShift,percussiveShift;
	
	private int[] attackInc,decayInc,releaseInc,sustainInc,percussiveInc;
	
	private int currentShift=20;
	
	private int[] currentInc=INCREMENTS[5];
	
	private int cycleCounter=0;
	
	private int output=0;
	
	private boolean sustained,keyScale;
	
	private boolean sustainNote;
	
	private boolean skipAttack=false;
	
	private int state=IDLE;
	
	public Envelope() {
		//Initialize variables
		setRates(0,0,0,0,false,false);
		setFNumber(0,0);
	}
	
	public void setRates(int attackRate,int decayRate,int sustainLevel,int releaseRate,boolean sustained,boolean keyScale) {
		this.attackRate=attackRate;
		this.decayRate=decayRate;
		this.releaseRate=releaseRate;
		this.sustainLevel=sustainLevel<<3;
		this.sustained=sustained;
		this.keyScale=keyScale;
	}
	
	public void setFNumber(int fNum,int octave) {
		int ksr=(octave<<1)+(fNum>>8);
		if(!keyScale)
			ksr>>=2;
		
		int rate=Math.min(attackRate*4+ksr,63);
		if(rate>=0x3c) {
			skipAttack=true;
		}else if(rate>=0x38) {
			skipAttack=false;
			attackInc=INCREMENTS[RATE_14+(rate & 3)];
			attackShift=0;
		}else if(attackRate==0){
			skipAttack=false;
			attackInc=INCREMENTS[RATE_DEFAULT];
			attackShift=20;
		}else {
			skipAttack=false;
			attackInc=INCREMENTS[rate & 3];
			attackShift=((0x3f ^ rate)>>2)-2;
		}
				
		rate=Math.min(decayRate*4+ksr,63);
		if(rate>=0x3c) {
			decayInc=INCREMENTS[RATE_15];
			decayShift=0;
		}else if(rate>=0x38){
			decayInc=INCREMENTS[RATE_14+(rate & 3)];
			decayShift=0;
		}else if(decayRate==0){
			decayInc=INCREMENTS[RATE_DEFAULT];
			decayShift=20;
		}else {
			decayInc=INCREMENTS[rate & 3];
			decayShift=((0x3f ^ rate)>>2)-2;
		}
		
		rate=Math.min(releaseRate*4+ksr,63);
		if(rate>=0x3c) {
			releaseInc=INCREMENTS[RATE_15];
			releaseShift=0;
		}else if(rate>=0x38){
			releaseInc=INCREMENTS[RATE_14+(rate & 3)];
			releaseShift=0;
		}else if(releaseRate==0){
			releaseInc=INCREMENTS[RATE_DEFAULT];
			releaseShift=20;
		}else {
			releaseInc=INCREMENTS[rate & 3];
			releaseShift=((0x3f ^ rate)>>2)-2;
		}
		
		rate=Math.min(7*4+ksr,63);
		percussiveInc=INCREMENTS[rate & 3];
		percussiveShift=((0x3f ^ rate)>>2)-2;

		rate=Math.min(5*4+ksr,63);
		sustainInc=INCREMENTS[rate & 3];
		sustainShift=((0x3f ^ rate)>>2)-2;
	}
	
	public void start() {
		//Reset envelope parameters
		if(!skipAttack) 
			setState(ATTACK);
		else
			setState(DECAY);
		output=0;
	}
	
	private void setState(int state) {
		this.state=state;
		switch(state) {
		case ATTACK:{
			currentShift=attackShift;
			currentInc=attackInc;
			break;
		}case DECAY: {
			currentShift=decayShift;
			currentInc=decayInc;
			break;
		}case SUSTAIN: {
			if(sustained) {
				currentShift=20;
				currentInc=INCREMENTS[5];
			}else {
				currentShift=releaseShift;
				currentInc=releaseInc;
			}
			break;
		}case RELEASE: {
			if(sustainNote) {
				currentShift=sustainShift;
				currentInc=sustainInc;
			}else if(sustained){
				currentShift=releaseShift;
				currentInc=releaseInc;
			}else {
				currentShift=percussiveShift;
				currentInc=percussiveInc;
			}
			break;
		}case IDLE: {
			currentShift=20;
			currentInc=INCREMENTS[5];
			break;
		}
		}
	}
	
	/**
	 * Sets wether the note should be sustained. Not to be confused with the sustained parameter which is definded by the instrument.
	 * @param sustain
	 */
	public void setSustainNote(boolean sustain) {
		this.sustainNote=sustain;
	}
	
	public boolean getSustainNote() {
		return sustainNote;
	}
	
	/**
	 * Enters the release phase of the envelope.
	 */
	public void release() {
		setState(RELEASE);
	}
	
	public int getState() {
		return state;
	}
	
	public int fetchEnvelope() {
		if((cycleCounter & (1<<currentShift)-1)==0) {
			int inc=currentInc[(cycleCounter>>currentShift) & 0b111];
			if(state==ATTACK) {
				output+=inc*((128-output)/4+1);
			}else if(state!=IDLE){
				output+=inc;
			}
		}
		
		cycleCounter=(cycleCounter+1) & CYCLE_MASK;
		
		if(state==ATTACK && output>=127) {
			setState(DECAY);
			output=0;
		}
		if(state==DECAY && output>=sustainLevel) {
			setState(SUSTAIN);
			output=sustainLevel;
		}
		if((state==SUSTAIN || state==RELEASE) && output>=127) {
			setState(IDLE);
			output=127;
		}
		
		if(state==ATTACK) 
			return 0x7f^output;
		else if(state!=IDLE)
			return output;
		else
			return 0x7f;
	}
}
