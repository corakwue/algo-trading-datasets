
public class Asset {
	String name, cusip, quote= "";       
	float fundRanking, marketRanking, price = 0;
	
	public Asset(){} // No-argument Constructor
	
	// Explicit constructor
	Asset(String name_, String cusip_, String quote_, float fundRanking_, float marketRanking_, float price_){
	name = name_;
	cusip = cusip_;
	quote = quote_;
	fundRanking = fundRanking_;
	marketRanking = marketRanking_;
	price = price_;
	}
	
}

