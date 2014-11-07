import java.util.*;

public class Technical {
	String 	algorithm;
	int 	windowNumber,
			numTrades, 
			avgTradeDuration_mos;
	
	List<Consensus> consensus = new ArrayList<Consensus>(); // list of consensus
	
	Boolean targetMet;
	float currentPrice, 
		  targetPrice,
		  avgHPR_Long,
		  avgHPR_Short,
		  stdDev_Long,
		  stdDev_Short,
		  windowAccuracy_Long,
		  windowAccuracy_Short; 
	
	Date targetExDate, inceptionDate = new Date ();

}
