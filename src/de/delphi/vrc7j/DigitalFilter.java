package de.delphi.vrc7j;

/*package*/ class DigitalFilter {
	
	private double[] firCoeff,iirCoeff;
	
	private double[] inputBuffer,outputBuffer;
	
	private int inputPointer,outputPointer;
	
	public DigitalFilter(double[] firCoeff,double[] iirCoeff) {
		this.firCoeff=firCoeff;
		this.iirCoeff=iirCoeff;
		inputBuffer=new double[firCoeff.length];
		outputBuffer=new double[iirCoeff.length];
	}
	
	public void addSample(int sample) {
		inputBuffer[inputPointer]=sample;
		inputPointer=(inputPointer+1) % inputBuffer.length;
	}
	
	public int fetchSample() {
		double val=0;
		for(int i=0;i<firCoeff.length;i++) {
			val+=firCoeff[i]*inputBuffer[(inputPointer-i+inputBuffer.length) % inputBuffer.length];
		}
		for(int i=0;i<iirCoeff.length;i++) {
			val+=iirCoeff[i]*outputBuffer[(outputPointer-i+outputBuffer.length) % outputBuffer.length];
		}
		outputBuffer[outputPointer]=val;
		outputPointer=(outputPointer+1) % outputBuffer.length;
		return (int) Math.round(val);
	}
}
