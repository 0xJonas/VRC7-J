package de.delphi.vrc7j;

/**
 * The ADSR Envelope used by the VRC7's operators.
 * @author Delphi1024
 *
 */
/*package*/ class Envelope {
	
	public static  final int ATTACK=0,
							DECAY=1,
							SUSTAIN=2,
							RELEASE=3,
							IDLE=4;
							//SETTLE=5;  //Does this exist on the ym2413 ?
	
	//Rates and increment values for the different stages.
	private int  attackRate,attackInc,
				decayRate,decayInc,
				sustainLevel,
				releaseRate,releaseInc;
	
	private int percussiveInc=0,sustainInc=0;
	
	private boolean sustained,keyScale;
	
	private boolean sustainNote;
	
	private int counterLow=0,counterHigh=0;
	
	private int state=IDLE;
	
	public Envelope() {
		
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
		//Compute finetuning part of the rates
		int fine=octave*2+(fNum>>8);
		
		//Apply key rate scaling
		if(!keyScale)
			fine>>=2;
			
		attackInc=Math.min(attackRate*4+fine,0x3f);
		attackInc=12*((attackInc & 0b11)+4)<<(attackInc>>2);
		
		decayInc=Math.min(decayRate*4+fine,0x3f);
		decayInc=((decayInc & 0b11)+4)<<(decayInc>>2);

		releaseInc=Math.min(releaseRate*4+fine,0x3f);
		releaseInc=((releaseInc & 0b11)+4)<<(releaseInc>>2);
		
		percussiveInc=Math.min(7*4+fine,0x3f);
		percussiveInc=((percussiveInc & 0b11)+4)<<(percussiveInc>>2);
		
		sustainInc=Math.min(5*4+fine,0x3f);
		sustainInc=((sustainInc & 0b11)+4)<<(sustainInc>>2);
	}
	
	public void start() {
		//Reset envelope parameters
		state=IDLE;
		nextState();
		counterLow=0;
		counterHigh=0;
	}
	
	/**
	 * Advances the envelope to the next stage.
	 */
	private void nextState() {
		switch(state) {
		case IDLE:{
			state=ATTACK;
			if(attackRate==0)
				nextState();
			break;
		}case ATTACK: {
			state=DECAY;
			break;
		}case DECAY: {
			state=SUSTAIN;
			break;
		}case SUSTAIN: {
			state=RELEASE;
			break;
		}case RELEASE: {
			state=IDLE;
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
		if(state==ATTACK) {
			int expIndex=(0x7f^counterHigh)*2;
			counterHigh=Operator.expTable[expIndex]>>3;
		}
		state=RELEASE;
	}
	
	public int getState() {
		return state;
	}
	
	/**
	 * Fetches a single value from the envelope. This method will update the envelopes internal state so that successive calls will each yield
	 * the next sample.
	 * @return
	 */
	public int fetchEnvelope() {
		//Increment envelope counter based on the current state.
		switch(state) {
		case ATTACK:{
			counterLow+=attackInc;
			break;
		}case DECAY:{
			counterLow+=decayInc;
			break;
		}case SUSTAIN:{
			if(!sustained)
				counterLow+=releaseInc;
			break;
		}case RELEASE:{
			if(sustainNote)
				counterLow+=sustainInc;
			else if(sustained)
				counterLow+=releaseInc;
			else
				counterLow+=percussiveInc;
			break;
		}default:{
			counterLow=0;
			counterHigh=0x7f;
			break;
		}
		}
		
		//Increment envelope output when counter overflows
		if(counterLow>0xffff) {
			counterHigh+=counterLow>>>16;
			counterLow&=0xffff;
		}
		
		//Enter next stage when output overflows (should only happen for attack>decay and sustain/release>idle 
		if(counterHigh>0x7f) {
			if(state==ATTACK)
				counterHigh=0;
			nextState();
		}
		
		//Enter next stage when the envelope is in the decay phase and the sustain level is reached
		if(state==DECAY && counterHigh>=sustainLevel) {
			counterHigh=sustainLevel;
			nextState();
		}
		
		if(state==ATTACK) {	//Use logarithmic scale during attack phase.
			int expIndex=(0x7f^counterHigh)*2;
			return Operator.expTable[expIndex]>>3;
		}else if(state==IDLE){
			return 0x7f;
		}else {
			return counterHigh;
		}
	}
}