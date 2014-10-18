package edu.snu.cms.reef.tutorial;

public class SGD_Linear {
	
	public SGD_Linear(){}
	
	
	public double hypothesis(double [] x, double [] hypo, int i, int y, double lRate){
		if( i == 0)
			hypo[i] = hypo[i] - lRate*(x[i]*hypo[i]-x[y]);
		else
			hypo[i] = hypo[i] - lRate*(x[i]*hypo[i]-x[y])*x[i];
		return hypo[i];
	}
}
