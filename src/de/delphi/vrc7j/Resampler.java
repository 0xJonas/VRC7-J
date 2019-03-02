package de.delphi.vrc7j;

/**
 * Converts sample rates using Lanczos resampling.
 * @author Delphi
 *
 */
/*package*/ class Resampler {
	
	private int order;
	
	private int bufferLength=3;
	
	private int[] prevSamples;
	
	private int samplePointer=0;
	
	private double x,step;
	
	public Resampler(int order,double sourceRate,double targetRate) {
		this.order=order;
		this.bufferLength=order*2+1;
		prevSamples=new int[bufferLength];
		step=sourceRate/targetRate;
		x=0.0;
	}
	
	public void addSample(int sample) {
		prevSamples[samplePointer]=sample;
		samplePointer=(samplePointer+1) % bufferLength;
		x-=1.0;
	}
	
	/**
	 * Same as addSample, but without ensures that x is 0.
	 * @param sample
	 */
	public void prefill(int sample) {
		addSample(sample);
		x=0.0;
	}
	
	private double sinc(double x) {
		if(x==0.0)
			return 1.0;
		return Math.sin(Math.PI*x)/(Math.PI*x);
	}
	
	public int fetchSample() {
		double val=0.0;
		for(int i=((int) x)-order+1;i<=((int) x)+order;i++) {
			int index=(samplePointer-order-1+i) % bufferLength;
			if(index<0)
				index+=bufferLength;
			int sample=prevSamples[index];
			val+=sample*sinc(x-i)*sinc((x-i)/order);
		}
		x+=step;
		return (int) val;
	}
}
