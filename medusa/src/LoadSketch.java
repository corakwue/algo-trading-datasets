/**
 * Adapted from on Kepler Visualization code by visual artist Jer Thorp @ blprnt@blprnt.com (awesome inspiration)
 * http://blog.blprnt.com/blog/blprnt/data-in-an-alien-context-kepler-visualization-source-code
 * 
 * 
 * Controls:
 * Mouse hover or type quote to show info.
 * Double-click to scale render size by half.
 * Arrow UP / DOWN to zoom.
 * Toggle between view modes with the keys below:
 * ` - Undo sort / restore flatness
 * 1 - Sort by Fund Ranking (color)
 * 2 - Sort by Fund Ranking (color)
 * 3 - Sort by Number of Parents i.e. top competitive companies
 * 4 - Restore tilt
 * 5 - Restore zoom
 * 6 - Change view mode / toggle flatness
 * 
 * Using Leap Motion Library for Processing
 * LeapMotion-Library v1.1.3 - LeapMotion-SDK v1.0.9 
 * Copyright 2013, Darius Morawiec
 *  
 *  Hand Orientation - control view/plane
 *  Swipe Gesture - Change view mode
 *  Screen tap - Restore tilt / zoom / flatness and undo sort
 *  Twirl Finger - Zoom in / out
 *  Point & Hold Index Finger - To select ( for 2s)
 *  While zoomed out (i.e. hover off), hold out n finger for 5s to - Sort by:
 *  	1 - Sort by Fund Ranking (color)
 *  	2 - Sort by Fund Ranking (color)
 *  	3 - Sort by Number of Parents i.e. top competitive companies
 *      Pinky finger - f**k off!
 */

import processing.core.*;

import java.awt.Frame;
import java.io.File;

import processing.data.*;
import processing.event.MouseEvent;

import java.lang.String;
import java.text.*;
import java.util.*;
import java.lang.Float;
import de.voidplus.leapmotion.*;

public class LoadSketch extends PApplet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7000768013596452778L;

	PFont defaultFont = createFont("Gill Sans MT", 24);

	XML xml;

	SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy");
	DecimalFormat floatFormat 	= new DecimalFormat("#.##");

	int backgroundColor 	= color(0); // dark background color
	int defaultFontColor 	= color(255); // Default font color (white)
	int foundFontColor 		= color(23, 184, 203); // Green for found user entry
	int childFontColor 		= color(192, 192, 192); // Gray for children
	int highlightColor 		= color(255); // white
	int loadingBarColor 	= color(255);
	int	notificationBoxColor = color(251, 72, 52); // light orange

	// For typing and selection
	String typedString = "";
	char typedChars[] = new char[5];
	int typedCount, foundCount;

	float messageX, messageY, selectedX, selectedY, notificationX,
			notificationY, notificationDX;
	String message = "Enter quote to search: ";
	String selected = "", lastNotification = "";
	Particle highlighted;

	// Size of rings
	static int nbrRings = 10;
	static float AU = 1200;
	// Enables shrinks scale of all rendered by half. on double-click.
	// 0 - Default, 1 - Restore to default, 2 - Shrink by 2x
	static int shrinkAU = 0, lastShrinkAU = 0;

	// This will keep track of whether the loading thread is finished
	boolean finished = false, textDisplayed = false;
	boolean nbrAddedDisplayed = false, nbrRenderedDisplayed = false;
	// And how far along is loading thread
	float percent = 0;
	// And what are we loading?
	String currentlyLoading = "...";

	// timers
	int timer, timerSinceLoadComplete;
	int lastNotificationTime, nextNotificationTime;
	int notificationInterval = 10000, notificationDuration = 5000;

	// hover effect
	PVector hoverLocation = new PVector(0, 0, 0);
	boolean hoverActive = false;
	double zoomToActivateHover = 0.2;
	
	// file selection
	String currentFileName = "watchlist_2013.xml";

	boolean DEBUG = false;
	int nbrDebug = 200; // load this number of equities if debug is enabled.

	Map<String, Stock> stocks = new HashMap<String, Stock>();
	Map<String, Particle> particles = new HashMap<String, Particle>();
	Map<String, Spring> springs = new HashMap<String, Spring>();
	Iterator<String> keys = null;

	int nbrStocks, nbrAdded = 0;

	float maxFundRanking, minFundRanking, maxMarketRanking, minMarketRanking,
			maxNumberParent = 1;

	// Axis & labels
	float yMax = 5, yMin = -1;
	String xLabel = "Semi-major Axis";
	String yLabel = ""; // TBD later

	// Rotation Vectors - control the main 3D space
	static PVector rot = new PVector(0, 0, 0);
	static PVector trot = new PVector(0, 0, 0);

	// Master zoom
	static float zoom = 0, tzoom = (float) 0.3;

	// Controls whether particles are flat on the plane (0) or not (1)
	static float flatness = 0, tflatness = 0;
	
	// Leap Motion support
	LeapMotion leap;
	float lastSwipeTimestamp, lastHandVisibleTimestamp, lastCircleGestureProgress = 0;
	float zoomDirectionCircleGesture = 1;
	float lastHandPitch, lastHandRoll, lastHandYaw = 0;
	int lastSwipeState, lastHandFingerCount;
	PVector lastHandOrientation = new PVector(0, 0, 0);
	PVector diffHandOrientation = new PVector(0, 0, 0);
	boolean holdZoomGesture = false, lasthoverState = false, enableSortGesture = true;
	float hoverToSelectDuration = 2; //in seconds
	float timeBetweenSwipes = 3000; // in milliseconds
	float minFingerVisibleToSort = 5; //in seconds
	float lastSortTimestamp;
	float minTimeBetweenGestureSorts = 5000; //in milliseconds
	
	Spring lastSpring;

	public void setup() {
		size(displayWidth, displayHeight, OPENGL); // P3D 
		background(backgroundColor);
		smooth();
		leap = new LeapMotion(this).withGestures("circle, swipe, screen_tap");
		// Enter Quote to search (top right)
		messageX = width - 180;
		messageY = 40;
		// Shows selected (top left)
		selectedX = 50;
		selectedY = 20;
		// Notifications (bottom right)
		notificationDX = width;
		notificationX = width - 40;
		notificationY = height - 40;
		thread("loadData"); // fire off the loading thread
	}

	public void draw() {

		// If we're not finished loading, draw a "loading bar"
		if (!finished) {
			background(backgroundColor); // draw background color
			stroke(defaultFontColor);
			noFill();
			rect(width / 2 - 150, height / 2, 300, 10);
			fill(loadingBarColor);
			// The size of the rectangle is mapped to the percentage completed
			float w = map(percent, 0, 1, 0, 300);
			rect(width / 2 - 150, height / 2, w, 10);
			textSize(16);
			textAlign(CENTER);
			fill(defaultFontColor);
			text("Please wait. Loading ", width / 2, height / 2 + 30);
			text(currentlyLoading, width / 2 + 100, height / 2 + 30);
		} else {
			// Load complete. Start rendering
			if (!textDisplayed) {
				background(backgroundColor); // draw background color
				textAlign(CENTER);
				textSize(24);
				fill(defaultFontColor);
				text("Finished loading. Rendering.", width / 2, height / 2);
				println("Finished loading. Rendering.");
				textDisplayed = true;
				try { // pause it here..then get iterator
					Thread.sleep((long) 1500);
					keys = stocks.keySet().iterator(); // set of keys for the
														// map
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				timerSinceLoadComplete = millis();
			} 
			else 
			{ // Render main sketch
				
				background(backgroundColor);

				// Add what to render
				while (keys.hasNext())
					addNext();

				if (!keys.hasNext() && !nbrAddedDisplayed) {
					println(nbrAdded + " nodes generated from XML");
					nbrAddedDisplayed = true;
					tiltPlane();
				}

				// Ease rotation vectors, zoom
				//if (!holdZoomGesture) {
					zoom += (tzoom - zoom) * 0.01;
					if (zoom < 0) {
						zoom = 0;
					} else if (zoom > 3.0) {
						zoom = (float) 3.0;
					}
				//}
				
				rot.x += (trot.x - rot.x) * 0.1;
				rot.y += (trot.y - rot.y) * 0.1;
				rot.z += (trot.z - rot.z) * 0.1;

				// Ease the flatness weight
				flatness += (tflatness - flatness) * 0.1;

				// MousePress - Controls Handling
				if (mousePressed) {
					//hoverActive = false; // Temporarily disables hover on mouse press.
					// MousePress - Rotation Adjustment
					trot.x += (pmouseY - mouseY) * 0.01;
					trot.z += (pmouseX - mouseX) * 0.01;
				}
				// For leap motion (assumes 1 hand for controls)
				 for(Hand hand : leap.getHands()){
					 if (hand.getTimeVisible() > 0.2){ // hand visible for more than 500ms
					 		
						 	holdZoomGesture = false;
					 						 		
						 	// hand.draw(); ;
						 	// TODO: Use roll (x), yaw (z) and pitch (y)
						    PVector.sub(hand.getDynamics(), lastHandOrientation, diffHandOrientation);
						    
						    // If user is NOT kind-a pointing. Also dampen sensitivity
						    if (hand.countFingers() >= 4 && diffHandOrientation.mag() >= 0.1 ){
						    	trot.x -= diffHandOrientation.x * 0.03;
						    	trot.z -= diffHandOrientation.z * 0.03;
						    	//trot.y += diffHandOrientation.y * 0.03; //(does absolutely nothing - hahaha)  
						    }
						     					    	
						    if (hand.hasFingers()) { 
						    	
						    	for(Finger finger : hand.getFingers())
						        	finger.draw(); // Draw fingers
						    	
						    	if (hoverActive && (hand.countFingers() == 2 || hand.countFingers() == 1) && hand.getFrontFinger().getTimeVisible() >= hoverToSelectDuration ){ // Only one finger? Time how longer its been hovered.
							    	hoverLocation.set(hand.getFrontFinger().getPosition()); hoverLocation.z = 0;
							    	//if (DEBUG) println("Using front finger to hover!");
							    }else if (!hoverActive && enableSortGesture){ // Hover is off.		
							    	boolean yesSort = millis() - lastSortTimestamp >= minTimeBetweenGestureSorts ? true : false;
							    	switch (hand.countFingers()){
								    	case 1:
								    		if (yesSort && hand.getFrontFinger().getTimeVisible() >= minFingerVisibleToSort) {
							    				sortByTemp();
							    				lastSortTimestamp = millis();
								    		}
								    		break;
								    	case 2:
								    		for(Finger finger : hand.getFingers()){
							    				if (finger.getTimeVisible() < minFingerVisibleToSort) yesSort &= false;
							    			}
							    			if (yesSort) {
							    				sortBySize();
							    				lastSortTimestamp = millis();
							    			}
							    			break;
								    	case 3:
								    		for(Finger finger : hand.getFingers()){
							    				if (finger.getTimeVisible() < minFingerVisibleToSort) yesSort &= false;
							    			}
							    			if (yesSort) {
							    				sortByNbrParent();	
							    				lastSortTimestamp = millis();
							    			}
							    			break;
							    	}
							    }
						    	
						    }
						    else // When I close my hand. pause zoom
						    	holdZoomGesture = true;
						    	
						    // Continuously monitor for smooth motion!
						    lastHandOrientation.set(hand.getDynamics());
						    
						    //lastHandFingerCount = hand.countFingers();	
						    break; // use one of the hands.
					}    
				 }
				 
				// Display text
				textFont(defaultFont);
				fill(defaultFontColor);
				textAlign(LEFT);
				textSize(18);

				// Display selected text
				if (!selected.isEmpty())
					text(selected, selectedX, selectedY);

				// Display user entry or default message
				if (typedCount == 0) {
					text(message, messageX, messageY); // show default message
				} else { // we typed something! tell user by changing font color
							// and highlighting the particle (later)
					if (foundCount > 0)
						fill(foundFontColor);
					text(typedString.toUpperCase(), messageX, messageY);
				}
				
				// For first 10s, display # of nodes rendered
				if (millis() - timerSinceLoadComplete < 10000) { 
					notificationText("Rendering " + nbrAdded + " nodes.",
							notificationX, notificationY, true);
				} else if (zoom > 0.2) // Post 10s, indicate hover active if enabled.
				{
					if (hoverActive)
						notificationText("Zoom : " + floatFormat.format(zoom)
								+ "x. Hover enabled. ", notificationX,
								notificationY, false); 
				} else { // Post 10s, when too zoomed out, disable hover.
					notificationText("Zoom : " + floatFormat.format(zoom)
							+ "x. Hover disabled. ", notificationX,
							notificationY, false); // shows for 5s
					hoverActive = false;
				}
				// Show peer info when possible
				if (highlighted != null) {
					Spring spr = springs.get(highlighted.asset.quote);
					// if (DEBUG) println("Rendering springs for " +
					// spr.parent.asset.quote + " and its " +
					// spr.children.size() + " children");
					if (spr != null && spr.children.size() > 0) {
						stroke(defaultFontColor);
						line(selectedX, selectedY + 70, selectedX
								+ textWidth(selected), selectedY + 70);
						fill(childFontColor);
						for (int i = 0; i < spr.children.size(); i++) {
							Particle child = spr.children.get(i);
							text(particleInfo(child), selectedX, selectedY
									+ ((i + 1) * 100));
						}
					}
				}

				// We want the center to be in the middle and slightly down when
				// flat, and to the left and down when raised
				translate((float) (width / 2 - (width * flatness * 0.4)),
						height / 2 + (160 * rot.x));
				rotateX(rot.x);
				rotateZ(rot.z);
				scale(zoom);

				smooth();

				// Draw the CENTER
				fill(255 - (255 * flatness));
				noStroke();
				ellipse(0, 0, 20, 20);

				// Draw Rings
				strokeWeight(2);
				noFill();
				for (int i = 0; i < nbrRings; i++) {
					int j = i + 1;
					stroke(255, 50 - (j * 40 * flatness));
					ellipse(0, 0, AU * j, AU * j);
				}

				// Draw the Y Axis
				stroke(255, 100);
				pushMatrix();
				rotateY(-PI / 2);
				line(0, 0, 1500 * flatness, 0);

				// Draw Y Axis max/min
				pushMatrix();
				fill(255, 100 * flatness, 255);
				rotateZ(PI / 2);
				textFont(defaultFont);
				textSize(18);
				text(round(yMin), -textWidth(str(yMin)), 0);
				text(round(yMax), -textWidth(str(yMax)), -1500);
				popMatrix();

				// Draw Y Axis Label
				textSize(18);
				fill(255, flatness * 255, 255);
				text(yLabel, 250 * flatness, -10);

				popMatrix();

				/*
				 * // Draw the X Axis if we are not flat pushMatrix();
				 * rotateZ(PI/2); line(0, 0, 1500 * flatness, 0);
				 * 
				 * if (flatness > 0.5) { pushMatrix(); rotateX(PI/2);
				 * line((float) (AU * 1.06), (float)-10.0, (float)(AU * 1.064),
				 * (float)10); line((float) (AU * 1.064), (float)-10.0,
				 * (float)(AU * 1.068), (float)10); popMatrix(); }
				 * 
				 * // Draw X Axis Label fill(255, flatness * 255);
				 * rotateX(-PI/2); text(xLabel, 50 * flatness, 17);
				 * 
				 * // Draw X Axis min/max fill(255, 100 * flatness); text(1, AU,
				 * 17); text("0.5", AU/2, 17);
				 * 
				 * popMatrix();
				 */
				
				if (springs != null && highlighted != null && springs.containsKey(highlighted.asset.quote)) {
					if (lastSpring != null)
						lastSpring.clear();
					Spring spr = springs.get(highlighted.asset.quote);
					// if (DEBUG) println("Rendering springs for " +
					// spr.parent.asset.quote + " and its " +
					// spr.children.size() + " children");
					if (spr.children.size() > 0)
						spr.render(); // if it has children...render it
					lastSpring = spr;
				}
				
				// Update mouse position & enable hover if not too zoomed in/out.
				// Placed here for performance reasons!
				if (zoom > zoomToActivateHover && zoom <= zoomToActivateHover + 1) { 
					hoverActive = true;
					// update hover location with mouse only if mouse was moved
					if (pmouseX != mouseX && pmouseY != mouseY) {
						hoverLocation.set(mouseX, mouseY);  
						//if (DEBUG) println("Moused moved. Using mouse to hover!");
					}
				}
				
				// Render particles we added.
				for (String k1 : particles.keySet()) {
					Particle pi = particles.get(k1);
					if (pi == null)
						continue;
					if (hoverActive && pi.isOver(hoverLocation)) {
						if (highlighted != null)
							highlighted.featured = false; // clear last
															// highlight
						pi.featured = true; 
						highlighted = pi; // highlight at mouse over!
						// println("Got : " + pi.asset.quote +
						// " with distance of " +
						// pi.location.dist(hoverLocation)+ "px and radius of "
						// + pi.radius + "px");
						selected = particleInfo(pi);
					}
					pi.update();
					pi.render();
				}
			}
		}

	}

	public class Spring {
		Particle parent; // parent
		List<Particle> children = new ArrayList<Particle>(); // children
																// (typically 3)
		PVector ran;
		PVector ran2;

		Spring(Particle parent) {
			this.parent = parent;
		}

		void render() {

			for (int i = 0; i < children.size(); i++) {
				stroke(129);
				noFill();
				strokeWeight(1);
				Particle child = children.get(i);
				child.featured = true; // enable highlight of its children
				this.ran = new PVector(1, 30, random(parent.location.z - 1500,
						child.location.z - 3000));
				this.ran2 = new PVector(10, 20, random(
						parent.location.z - 1500, child.location.z - 3000));
				bezier(parent.location.x, parent.location.y, parent.location.z,
						ran.x, ran.y, ran.z, ran2.x, ran2.y, ran2.z,
						child.location.x, child.location.y, child.location.z);
			}
		}

		void clear() {
			parent.featured = false; // clear particle's highlight.
			for (int i = 0; i < children.size(); i++) {
				Particle child = children.get(i);
				child.featured = false; // clear highlight of its children
			}
		}

	}
    
	/* 
	 * Draws notification text with box around it.
	 * Notification lasts for "notificationDuration" in milliseconds
	 * with "notificationInterval" between notifications.
	 * timerOverride: forces to display notification always, irrespective of above.
	 */
	public void notificationText(String t, float x, float y,
			boolean timerOverride) { 
		// TODO: Support for multiple notifications!
		
		// If last timer hasn't expired, display last!
		if (!timerOverride && !lastNotification.isEmpty()
				&& (millis() - lastNotificationTime) < notificationDuration) {
			t = lastNotification;
		} 
		
		// Location adjustment based on text
		float textH = textDescent() - textAscent();
		float textW = textWidth(t);
		float adjustedX = textW - (width - x);

		if (adjustedX < 0)
			adjustedX = 0;

		 // initialize lastNotificationTime to now if not set.
		if (!timerOverride && lastNotificationTime == 0)
			lastNotificationTime = millis();

		if (!lastNotification.equals(t)) { // different text entered.
			if (!timerOverride) {
				if (nextNotificationTime > 0 && millis() < nextNotificationTime) {
					return; // if not yet time between notifications!
				}
				// We got this far, means we will render you.
				lastNotificationTime = millis(); // Set when no longer in
													// motion!
				nextNotificationTime = lastNotificationTime
						+ notificationInterval;
				notificationDX = width;
			} else {
				lastNotificationTime = millis(); // Set when no longer in
													// motion!
				nextNotificationTime = lastNotificationTime
						+ notificationInterval;
				notificationDX = width;
			} // reset motion ...we must render you.
		} else { // same text as before!
			// if ( millis() < nextNotificationTime) print("elapsed: " +
			// (millis() - lastNotificationTime) + "\n");
			if (!timerOverride
					&& (millis() - lastNotificationTime) > notificationDuration) {
				return;
			} // you have expired, its been more than 5s.
		}
		// must be after return statement above
		if (notificationDX > x)
			notificationDX -= 5; // Notifications are RIGHT to LEFT
		else { // done with motion.
			notificationDX = x;
		}

		fill(notificationBoxColor);
		noStroke(); // draw our box
		rect(notificationDX - adjustedX - 10, y - (textH / 2),
				(float) (textW * 1.25), 2 * textH);
		fill(defaultFontColor); // draw our text
		text(t, notificationDX - adjustedX, y);

		lastNotification = t; // keep last notification.
	}

	public void fileSelected(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
		} else {
			currentFileName = selection.getAbsolutePath();
			println("User selected " + currentFileName);
		}
	}

	/*
	 * Loads XML data
	 */
	public void loadData() throws InterruptedException {
		// The thread is not completed
		finished = false;

		// if (!DEBUG) selectInput("Select a file to process:", "fileSelected");

		if (DEBUG)
			println("Loading stocks from XML");

		// Load XML file
		xml = loadXML(currentFileName);
		// Get all the child nodes named "row"
		XML[] children = xml.getChildren("row");

		// The size of the array of particle objects is determined by the total
		// XML elements
		if (DEBUG)
			nbrStocks = nbrDebug; // load few if we're debugging.
		else
			nbrStocks = children.length;

		println(nbrStocks + " stocks in XML ");

		for (int i = 0; i < nbrStocks; i++) {

			// Initialize stock .. will append this!
			Stock stock = new Stock();

			String quote = children[i].getChild("Quote").getContent();
			currentlyLoading = quote; // tell us where you stand!
			String companyName = children[i].getChild("CompanyName")
					.getContent();
			companyName = companyName.replace("\\X26", "&");
			String cusip = children[i].getChild("CUSIP").getContent();
			float fundRanking = children[i].getChild("FundRanking")
					.getFloatContent();
			float marketRanking = children[i].getChild("MktRanking")
					.getFloatContent();

			stock.asset = new Asset(companyName, cusip, quote, fundRanking,
					marketRanking, 0);

			// Get peer
			XML[] peerElements = children[i].getChildren("peer");
			for (int count = 0; count < peerElements.length; count++) {
				String peerQuote = peerElements[count].getChild("Quote")
						.getContent();
				if (peerQuote.isEmpty())
					continue; // its ought to be filled at least to be correct!
				String peerCompanyName = peerElements[count].getChild(
						"CompanyName").getContent();
				peerCompanyName = peerCompanyName.replace("\\X26", "&");
				float peerFundRanking = peerElements[count].getChild(
						"FundRanking").getFloatContent();
				float peerMarketRanking = peerElements[count].getChild(
						"MktRanking").getFloatContent();
				// append this peer
				stock.peers.add(new Asset(peerCompanyName, "", peerQuote,
						peerFundRanking, peerMarketRanking, 0));
			}

			// Get technical.
			XML[] technicalElements = children[i].getChildren("technical");
			for (int count = 0; count < technicalElements.length; count++) {
				Technical technical = new Technical();
				// Which algorithm
				technical.algorithm = technicalElements[count].getChild(
						"Algorithm").getContent();

				// Get windowInfo element
				XML windowInfoElement = technicalElements[count]
						.getChild("windowInfo");

				technical.avgTradeDuration_mos = windowInfoElement
						.getInt("AvgTradeDurationmos");
				try {
					technical.inceptionDate = dateFormat
							.parse(windowInfoElement.getString("InceptionDate"));
				} catch (ParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				technical.numTrades = windowInfoElement
						.getInt("NumberofTrades");
				technical.windowNumber = windowInfoElement
						.getInt("windowNumber");

				// Get windowAccuracy Element
				XML windowAccuracy = technicalElements[count]
						.getChild("windowAccuracy");
				String avgHPR_Long = windowAccuracy.getString("AvgHPRLong");
				String avgHPR_Short = windowAccuracy.getString("AvgHPRLong");
				String stdDev_Long = windowAccuracy.getString("StdDevLong");
				String stdDev_Short = windowAccuracy.getString("StdDevShort");
				String windowAccuracyLong = windowAccuracy
						.getString("WindowAccuracyLong");
				String windowAccuracyShort = windowAccuracy
						.getString("WindowAccuracyShort");

				technical.avgHPR_Long = Float.parseFloat(avgHPR_Long.substring(
						0, avgHPR_Long.length() - 1));
				technical.avgHPR_Short = Float.parseFloat(avgHPR_Short
						.substring(0, avgHPR_Short.length() - 1));
				technical.stdDev_Long = Float.parseFloat(stdDev_Long.substring(
						0, stdDev_Long.length() - 2));
				technical.stdDev_Short = Float.parseFloat(stdDev_Short
						.substring(0, stdDev_Short.length() - 2));
				technical.windowAccuracy_Long = Float
						.parseFloat(windowAccuracyLong.substring(0,
								windowAccuracyLong.length() - 1));
				technical.windowAccuracy_Short = Float
						.parseFloat(windowAccuracyShort.substring(0,
								windowAccuracyShort.length() - 1));

				// Get current consensus
				XML currentConsensus = technicalElements[count]
						.getChild("currentConsensus");

				Consensus consensus = new Consensus();

				consensus.consensus = currentConsensus.getString("Consensus");
				try {
					consensus.consensusDate = dateFormat.parse(currentConsensus
							.getString("ConsensusDate"));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String consensusPrice = currentConsensus
						.getString("ConsensusPrice");
				consensus.consensusPrice = Float
						.parseFloat(consensusPrice.substring(1,
								consensusPrice.length()).replace(",", ""));
				consensus.index = 0; // yes, this is current
				technical.consensus.add(consensus);

				// Get previous consensus // TODO: Change XML to include all
				// consensus as in version 2.0
				XML previousConsensus = technicalElements[count]
						.getChild("previousConsensus");

				// NOTE: Re-use of consensus

				consensus.consensus = previousConsensus
						.getString("PreviousConsensus");
				try {
					consensus.consensusDate = dateFormat
							.parse(previousConsensus
									.getString("PreviousConsensusDate"));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String previous_consensusPrice = previousConsensus
						.getString("PreviousConsensusPrice");
				consensus.consensusPrice = Float
						.parseFloat(previous_consensusPrice.substring(1,
								previous_consensusPrice.length()).replace(",",
								""));
				consensus.index = 1; // yes, this is previous
				technical.consensus.add(consensus);

				// Get price target
				XML priceTarget = technicalElements[count]
						.getChild("priceTarget");

				String currentPrice = priceTarget.getString("CurrentPrice");
				technical.currentPrice = Float.parseFloat(currentPrice
						.substring(1, currentPrice.length()).replace(",", ""));
				String targetPrice = priceTarget.getString("TargetPrice");
				technical.targetPrice = Float.parseFloat(targetPrice.substring(
						1, targetPrice.length()).replace(",", ""));

				try {
					technical.targetExDate = dateFormat.parse(priceTarget
							.getString("TargetEx-Date"));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				technical.targetMet = priceTarget.getString("TargetMet")
						.equals("Y");

				stock.technicals.add(technical);

			}
			// Add this stock to hash
			stocks.put(quote, stock);

			// Dummy delay to "slow down" data load
			/* if (!DEBUG) */Thread.sleep((long) 10);

			// How far along!
			if (i % 10 == 0) {
				percent = (float) i / nbrStocks;
			}
		}

		// The thread is completed!
		finished = true;

	}

	public static void main(String[] args) {
//		  Frame frame = new Frame("testing");
//		  frame.setUndecorated(true);
//		  PApplet applet = new LoadSketch();
//		  frame.add(applet);
//		  applet.init();
//		  frame.setBounds(0, 0, 1920, 1080); 
//		  frame.setVisible(true);
		  PApplet.main(new String[] { /*"--present",*/ "LoadSketch" });
	}
	
	/*
	 * Added nodes to render from loaded XML data
	 */
	public void addNext() {
		// IMPORTANT: Each particle is added as an asset rather than a stock.
		// A stock is an entity which was analyzed. For instance, if you ran the
		// program on "XYZ" then "XYZ" here is the stock.
		// An asset is a peer entity which may/may not have been analyzed.
		// For instance, if "ABC" was a peer of "XYZ" and hence was "analyzed"
		// during "XYZ" ...but not completely entered, then "ABC" is an asset to
		// stock "XYZ"
		// The major difference between a stock and an asset is that all
		// technical info of "XYZ" is returned. Plus more!
		// Common between an asset and a stock is that both should have rankings
		// from Fund Analyzer!

		// Each rendered particle here is at least, an asset!
		if (!keys.hasNext()) {
			return;
		}

		// Treat each stock node as a parent, unless its rendered.
		Particle parent = null, child = null;
		String key = keys.next();
		Stock parentNode = stocks.get(key);

		maxFundRanking = max(maxFundRanking, parentNode.asset.fundRanking);
		minFundRanking = min(minFundRanking, parentNode.asset.fundRanking);
		maxMarketRanking = max(maxMarketRanking, parentNode.asset.marketRanking);
		minMarketRanking = min(minMarketRanking, parentNode.asset.marketRanking);

		if (particles.containsKey(key)) { // node already exists
			// was it added as a child (i.e. asset) ..then proceed so we add its
			// children...else return
			parent = particles.get(key);
			if (parent.asParent)
				return; // its parent. then return. we already added you...as
						// parent (i.e. stock).
			else { // re-update to use latest #s [ not just relative to some
					// parent ]
					// why? because relative to some parent might be based on
					// limited historical info for that parent.
				parent.asset = parentNode.asset;
				parent.defaultColor = colorMapping(parent.asset);
				parent.radius = sizeMapping(parent.asset);
			}
		} else { // new parent node
			parent = new Particle(colorMapping(parentNode.asset),
					sizeMapping(parentNode.asset), parentNode.asset, this, true);
			particles.put(key, parent);
			springs.put(key, new Spring(parent));
			++nbrAdded;
			//if (DEBUG) println("Added new parent node - " + parentNode.asset.quote);
		}

		if (parentNode != null) {
			for (int i = 0; i < parentNode.peers.size(); i++) {
				Asset childNode_pri = parentNode.peers.get(i);

				if (childNode_pri != null) {
					// Ensure we don't add itself as child [although it can be,
					// in top peers]
					if (childNode_pri.quote.equals(key))
						continue;

					// Add child if it doesn't already exist.
					if (!particles.containsKey(childNode_pri.quote)) {
						child = new Particle(colorMapping(childNode_pri),
								sizeMapping(childNode_pri), childNode_pri,
								this, false);
						child.nbrParents = 1;
						particles.put(childNode_pri.quote, child);
						++nbrAdded;
					} else { // already there? update # of children.
						child = particles.get(childNode_pri.quote);
						child.nbrParents++;
						maxNumberParent = max(maxNumberParent,
								particles.get(childNode_pri.quote).nbrParents);
					}
					parent.nbrChildren++;

					//if (DEBUG) println("Added child node to " + key + " - " +
					// childNode_pri.quote);

					if (springs.containsKey(key)) {// parent exist.
						springs.get(key).children.add(child);
					}
					// if (DEBUG) println("Added new node - " +
					// childNode.asset.quote);
				}
			}
			parent.asParent = true; // ha, now you're added as parent since your
									// children are handled.
		}

	}
	
	/*
	 * (non-Javadoc)
	 * @see processing.core.PApplet#mouseClicked(java.awt.event.MouseEvent)
	 * Scale render size on mouse double click
	 */
	public void mouseClicked(MouseEvent e) {
		if (e.getCount() == 2) {		
			changeScale();
		}
	}
	
	public void keyPressed() {
		if ((key == BACKSPACE) || (key == DELETE)) {
			typedCount--;
			if (typedCount < 0){
				typedCount = 0;
			}
				
			updateTyped();
		} else if ((key >= 'A' && key <= 'Z') || (key >= 'a' && key <= 'z')) {
			if (typedCount != 5) { // only 5 entries
				typedChars[typedCount++] = key;
			}
			updateTyped();
		} else if (key == TAB) {
			// TODO
		}
		else if (keyCode == UP) { // Zoom in
			tzoom += 0.08;
		} else if (keyCode == DOWN) { // Zoom out
			tzoom -= 0.08;
		}
		/*
		 * else if (keyCode == F1) { showHelp(); }
		 */
		else if (key == '2') { // sort by market ranking
			sortBySize();
			// toggleFlatness(1);
			yMax = 5;
			yMin = -1;
			yLabel = "Market Ranking (Higher is Better)";

		} else if (key == '3') { // sort by market ranking
			sortByNbrParent();
			// toggleFlatness(1);
			yMax = maxNumberParent;
			yMin = 0;
			yLabel = "Number of Competitors";

		} else if (key == '1') { // sort by fund ranking
			sortByTemp();
			trot.x = PI / 2;
			yMax = 5;
			yMin = -1;
			yLabel = "Fund Ranking (Higher is Better)";
			// toggleFlatness(1);
		} else if (key == '`') {
			unSort();
			toggleFlatness(0);
			yLabel = "";
		} else if (key == '4') { // tilt plane
			tiltPlane();
		} else if (key == '5') { // Restore zoom
			restoreZoom();
		}

		if (key == '6') { // Reset flatness other view
			tflatness = (tflatness == 1) ? (0) : (1);
			toggleFlatness(tflatness);
		}

	}
	
	void changeScale(){
		// Silly state machine for AU state!
		lastShrinkAU = shrinkAU;
		switch (shrinkAU) {
		case 1: // currently restored
			shrinkAU = 2;
			AU /= 2;
			break;// then shrink.
		case 2: // currently shrunk
			shrinkAU = 1;
			AU *= 2;
			break;// then restore
		default: // Default 0.
			shrinkAU = 2;
			AU /= 2;
			break; // see the shrink!
		}
		// Update radius and what's clicked.
		for (String k1 : particles.keySet()) {
			Particle pi = particles.get(k1);
			if (pi == null)
				continue;
			pi.updateRadius(); // update radius;
		}
		// if (DEBUG) println("mouse double clicked. ShrinkAU: " + shrinkAU
		// + ". LastShrinkAU: " + lastShrinkAU);
	}
	
	void updateTyped() {
		if (typedCount > 0)
			typedString = new String(typedChars, 0, typedCount);
		else
			typedString = "";
		// if (DEBUG && typedCount > 0) println("User typed: " + typedString);
		findUserEntry(); // find what user typed.
	}

	void findUserEntry() {
		foundCount = 0;
		if (!typedString.isEmpty() && particles.containsKey(typedString.toUpperCase())) {
			foundCount++;
			if (highlighted != null)
				highlighted.featured = false; // clear last highlight
			highlighted = particles.get(typedString.toUpperCase());
			highlighted.featured = true;
			selected = particleInfo(highlighted);
		} else {
			if (highlighted != null)
				highlighted.featured = false; // clear last highlight
		}
	}

	/*
	 * Returns string to display for given particle
	 */
	String particleInfo(Particle pi) {
		return pi.asset.name + " (" + pi.asset.quote + ") \nFund Ranking: "
				+ pi.asset.fundRanking + "\nMarket Ranking: "
				+ pi.asset.marketRanking;
	}

	/*
	 * Maps fund ranking to particle's color for loaded XML set
	 */
	int colorMapping(Asset a) {
		// IMPORTANT: For HSB color mode - if other mode, then convert.
		// Color (Hue, Saturation & Brightness) based on fundRanking. Size based
		// on marketRanking. In other words, larger brighter & colorful is
		// better!
		// we know numbers are from 0 to 5 (higher is better)
		colorMode(HSB);
		float h = (a.fundRanking >= 0) ? map(a.fundRanking, minFundRanking,
				maxFundRanking, 200, 0) : 200; // for negative fund ranking!
		int result = color(h, 255, 255);
		colorMode(RGB);
		// if (DEBUG) println("Color is " + result + " & h is" + h);
		return result;
	}
	
	/*
	 * Maps market ranking to particle's radius for loaded XML set
	 */
	int sizeMapping(Asset a) {
		// IMPORTANT: For HSB color mode - if other mode, then convert.
		// Color (Hue & Brightness) based on fundRanking. Size based on
		// marketRanking. In other words, larger brighter & colorful is better!
		// we know numbers are from 0 to 5 (higher is better)
		int result = (a.marketRanking >= 0) ? round(map(a.marketRanking,
				minMarketRanking, maxMarketRanking, 30, 120)) : 30; 
		return result;
	}

	void sortBySize() {
		// Raise the particles off of the plane according to their size / market
		// ranking
		for (String k1 : particles.keySet()) {
			particles.get(k1).tz = map(particles.get(k1).asset.marketRanking,
					yMin, yMax, 0, 1500);
		}
	}

	void sortByNbrParent() {
		// Raise the particles off of the plane according to times it was peer
		// of another
		for (String k1 : particles.keySet()) {
			particles.get(k1).tz = map(particles.get(k1).nbrParents, 0,
					maxNumberParent, 0, 1500);
		}
	}

	void sortByTemp() {
		// Raise the particles off of the plane according to their temp / fund
		// ranking
		for (String k1 : particles.keySet()) {
			particles.get(k1).tz = map(particles.get(k1).asset.fundRanking,
					yMin, yMax, 0, 1500);
		}
	}

	void unSort() {
		// Put all of the particles back onto the plane
		for (String k1 : particles.keySet()) {
			particles.get(k1).tz = 0;
		}
	}
	
	void tiltPlane(){
		trot.x = (float) 1.5;
	}
	
	void restoreZoom(){
		tzoom = 1;
	}
	
	void toggleFlatness(float f) {
		tflatness = f;
		if (tflatness == 1) {
			trot.x = PI / 2;
			trot.z = -PI / 2;
		} else {
			trot.x = 0;
		}
	}
	
	// For Leap Motion
		
	// SWIPE GESTURE
	public void leapOnSwipeGesture(SwipeGesture g, int state){
		//TODO: Swipe gesture sucks at detecting! Should only act on horizontal swipes.
		
		switch(state){
			case 1: // Start
				//println("SwipeGesture: started");
				lastSwipeState = state;
				enableSortGesture = false;
				break;
			case 2: // Update
				break;
			case 3: // Stop
				// Reset flatness other view
				//println("SwipeGesture: ended");
				
				if (state !=lastSwipeState && (millis() - lastSwipeTimestamp) >  timeBetweenSwipes) {
					tflatness = (tflatness == 1) ? (0) : (1);
					toggleFlatness(tflatness);
				}
				lastSwipeState = state;
		        lastSwipeTimestamp =  millis();
		        enableSortGesture = true;
				break;
		}
	}
	
	// CIRCLE GESTURE
	public void leapOnCircleGesture(CircleGesture g, int state){
	    float   progress            = g.getProgress();
	    // TODO: Really detect CW or CCW!
	    switch(state){
	        case 1: // Start
	        	lasthoverState = hoverActive;
	        	hoverActive = false; //disable so we don't hover
	        	enableSortGesture = false;
	            break;
	        case 2: // Update
	        	hoverActive = false; //disable so we don't hover
	        	if (progress - lastCircleGestureProgress < 0)
	        		zoomDirectionCircleGesture = -1* zoomDirectionCircleGesture;
	        	if (!holdZoomGesture)
	        		tzoom += zoomDirectionCircleGesture*0.005;
	        	lastCircleGestureProgress = progress;
	            break;
	        case 3: // Stop
	            //println("CircleGesture: "+id);
	        	hoverActive = lasthoverState; //re-enable hover
	        	enableSortGesture = true;
	            break;
	    }
	}

	// SCREEN TAP GESTURE
	public void leapOnScreenTapGesture(ScreenTapGesture g){
		// Reset zoom, title plane, un-sort, toggle flatness & change AU scale on screen tap
		// TODO: Better detect this gesture
	    changeScale();
	    restoreZoom();
		unSort();
		toggleFlatness(0);
		tiltPlane();
	}

//	// KEY TAP GESTURE
//	public void leapOnKeyTapGesture(KeyTapGesture g){
//			
//		// sort based on finger ID
//		switch(g.getFinger().getId()){
//			case 1:
//				break;
//			case 2:
//				break;
//			case 3:
//				break;
//			}
//	}

}
