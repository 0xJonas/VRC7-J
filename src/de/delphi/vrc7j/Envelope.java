package de.delphi.vrc7j;

public class Envelope {
	
	public static final int DAMPING=3,
							ATTACK=0,
							DECAY=1,
							RELEASE=2;
	
	private static final int PERCUSSIVE_RATE=7,
							SUSTAINED_RATE=5,
							DAMPING_RATE=12;
	
	private static final boolean[][] ENV_TABLE= {
									{false,false,false,false},
									{true, false,false,false},
									{true, false,true, false},
									{true, true, true, false}
								};
	
	private int  attackRate,decayRate,sustainLevel,releaseRate;

	private boolean sustained,keyScale;
	
	private int currentHigh,rateLow;
	
	private int attackHigh,
				decayHigh,
				releaseHigh,
				percussiveHigh,
				sustainedHigh,
				dampingHigh;
	
	private int cycleCounter=0,miniCounter=0;
	
	private int output=127;

	private boolean sustainNote;

	private int state=RELEASE;
	
	private boolean envelopeEnabled=true;
	
	public Envelope() {
		
	}
	
	public void setRates(int attackRate,int decayRate,int sustainLevel,int releaseRate,boolean sustained,boolean keyScale) {
		this.attackRate=attackRate;
		this.decayRate=decayRate;
		this.releaseRate=releaseRate;
		this.sustainLevel=sustainLevel;
		this.sustained=sustained;
		this.keyScale=keyScale;
	}
	
	public void setFNumber(int fNum,int octave) {
		int ksr=(octave<<1)+(fNum>>8);
		if(!keyScale)
			ksr>>=2;
		
		rateLow=ksr & 3;
		
		attackHigh=Math.min(attackRate+(ksr>>2),0xf);
		decayHigh=Math.min(decayRate+(ksr>>2),0xf);
		releaseHigh=Math.min(releaseRate+(ksr>>2),0xf);
		percussiveHigh=Math.min(PERCUSSIVE_RATE+(ksr>>2),0xf);
		sustainedHigh=Math.min(SUSTAINED_RATE+(ksr>>2),0xf);
		dampingHigh=Math.min(DAMPING_RATE+(ksr>>2),0xf);
	}
	
	private void setState(int state) {
		this.state=state;
		switch(state) {
		case DAMPING:{
			currentHigh=dampingHigh;
			break;
		}case ATTACK:{
			currentHigh=attackHigh;
			break;
		}case DECAY:{
			currentHigh=decayHigh;
			break;
		}case RELEASE:{
			currentHigh=releaseHigh;
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
		if(state==RELEASE) {
			setState(RELEASE);	//Reset percussive/sustained rates
		}
	}
	
	public boolean getSustainNote() {
		return sustainNote;
	}
	
	public void start() {
		setState(DAMPING);
		envelopeEnabled=true;
	}
	
	/**
	 * Enters the release phase of the envelope.
	 */
	public void release() {
		setState(RELEASE);
		//TODO a bit hacky, change maybe? 
		if(!sustained)
			currentHigh=percussiveHigh;
		if(sustainNote)
			currentHigh=sustainedHigh;
		envelopeEnabled=true;
	}
	
	public int getState() {
		return state;
	}
	
	public int fetchEnvelope() {
		//Count leading zeros of cycle counter
		int zeroCount=1;
		for(int i=1;i<=13;i++) {
			if((cycleCounter & 1)==1)
				break;
			if(((cycleCounter>>i) & 1)!=0 
					&& ((cycleCounter>>(i-1)) & 1)==0) {
				zeroCount=i+1;
				break;
			}
		}
		if(zeroCount>13)
			zeroCount=0;
		
		//Check whether to update the envelope based on zero count
		boolean clockEnvelope=false;
		if((currentHigh+zeroCount) % 0xf==12)
			clockEnvelope=true;
		if((currentHigh+zeroCount) % 0xf==13 && (rateLow & 2)!=0)
			clockEnvelope=true;
		if((currentHigh+zeroCount) % 0xf==14 && (rateLow & 1)!=0)
			clockEnvelope=true;
		if(currentHigh>=0xc)
			clockEnvelope=false;
		if(currentHigh==0)
			clockEnvelope=false;
		
		//Get various flags
		boolean envTable=ENV_TABLE[rateLow][cycleCounter & 3];
		
		boolean miniZero=miniCounter==0;
		boolean miniOdd=(miniCounter & 1)==1;
		
		//Setup increment values
		int inc1=state==ATTACK ? output>>1 : 0x7f;
		int inc2=state==ATTACK ? output>>2 : 0x7f;
		if(envelopeEnabled) {
			inc1&=~2;
			inc2&=~1;
		}
		
		int inc3=state==ATTACK ? output>>3 : 0x7f;
		int inc4=state==ATTACK ? output>>4 : 0x7f;
		
		//Apply increments based on previous stuff
		int envInc=0x7f;
		
		if(clockEnvelope || !envTable && currentHigh==12)
			envInc&=inc4;
		
		if(!envTable && currentHigh==13 || envTable && currentHigh==12)
			envInc&=inc3;
		
		if(currentHigh==14 && !envTable
				|| currentHigh==13 && envTable
				|| currentHigh==13 && !envTable && miniOdd && envelopeEnabled
				|| currentHigh==12 && !envTable && miniZero && envelopeEnabled
				|| currentHigh==12 && envTable && miniOdd && envelopeEnabled
				|| clockEnvelope && miniZero && envelopeEnabled)
			envInc&=inc2;
		
		if(currentHigh==15 || currentHigh==14 && envTable)
			envInc&=inc1;
		
		//Update output
		output=~((0x7f^output)+envInc+1) & 0x7f;
		
		//Update envelope stage
		if(state==ATTACK && currentHigh==15) {	//Skip attack
			output=0;
			setState(DECAY);
		}
		
		if(state==DAMPING && output>=0x7c)
			setState(ATTACK);
		
		if(state==ATTACK && output==0)
			setState(DECAY);
		
		if(state==DECAY && output>>3==sustainLevel) {
			if(sustained)
				envelopeEnabled=false;
			else
				setState(RELEASE);
		}
		
		if(state==RELEASE && output>=0x7c)
			envelopeEnabled=false;
		
		//Update counters
		if(miniZero)
			cycleCounter++;
		miniCounter=(miniCounter+1) & 3;
		
		return output;
	}
}
