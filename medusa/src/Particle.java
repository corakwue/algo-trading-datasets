import processing.core.*;

public class Particle  implements Comparable<Particle> {

	PApplet parent; // The parent PApplet that we will render ourselves onto
	private static final long serialVersionUID = 4032155538755611788L;

	PVector location = new PVector(0, 0, 0);
	float radius, wt = 0;
	int defaultColor = 255, opacity = 175;
	int nbrChildren = 0;
	float axis = 0, tz = 0;
	float theta, thetaSpeed, apixelAxis;
	boolean featured = false;
	boolean asParent = false;
	int nbrParents = 0;
	Asset asset = new Asset();

	Particle(int col, float radius, Asset asset, PApplet parent, boolean asParent) {
		this.parent = parent;
		this.defaultColor = col;
		this.nbrChildren = 0;
		this.radius = radius;
		this.asset = asset;
		this.asParent = asParent;
		// Random particle initial angle, placement and speed!
		this.theta = parent.random(2 * PConstants.PI);
		this.thetaSpeed = (2 * PConstants.PI) / parent.random(5000, 10000);
		this.axis = parent.random((float)0.05, LoadSketch.nbrRings / 2 );
	}

	void update() {
		theta += thetaSpeed;
		location.z += (tz - location.z) * 0.1;
	}
	
	void updateRadius(){
		if (LoadSketch.shrinkAU == 2) {
			radius /= 2; 
		}
		else if (LoadSketch.shrinkAU == 1) {
			radius *= 2;
		}
	}
	
	boolean equals(Particle pi){
		return pi.asset.quote.equalsIgnoreCase(asset.quote);
	}
	
	@Override
	public int compareTo(Particle o) {
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;
	    
	    if ( o.equals(this)) return EQUAL;
	    if ( this.asset.fundRanking > o.asset.fundRanking) return BEFORE;
	    if ( this.asset.fundRanking < o.asset.fundRanking) return AFTER;
		
	    assert this.equals(o) : "compareTo inconsistent with equals";
	    return EQUAL;
	}

	void updateAlpha() {
		//for alpha glow effect
		if (opacity <= 0 ) opacity += 50;
		if (opacity >= 255 ) opacity -= 50;
	}
	
	void updateLocation(){
		apixelAxis = LoadSketch.AU * this.axis;
		
		//if (axis > 1.06 && featured) {
		//	apixelAxis = (float) (((1.06 + ((axis - 1.06) * (1 - LoadSketch.flatness))) * LoadSketch.AU) + axis * 10);
		//}
		location.x = PApplet.sin(theta * (1 - LoadSketch.flatness)) * apixelAxis;
		location.y = PApplet.cos(theta * (1 - LoadSketch.flatness)) * apixelAxis;
	}
	
	boolean isOver(PVector point) {
		// Below code works well in Processing 2.0 but not in Eclipse. UPDATE: Fixed. Use parent.screenX/Y/Z.
		updateLocation();
		PVector location2D = new PVector(parent.screenX(location.x, location.y, location.z),parent.screenY(location.x, location.y, location.z),0 );
		return location2D.dist(point) < (radius)/4; // temporary cheat..hehe it works!
	}

	public void render() {
		updateLocation();
		parent.pushMatrix();
		parent.translate(location.x, location.y, location.z);
		// Billboard
		parent.rotateZ(-LoadSketch.rot.z);
		parent.rotateX(-LoadSketch.rot.x);
		parent.noStroke();
		if (featured) {
			updateAlpha();
			parent.translate(0, 0, 1);
			parent.stroke(255, 255);
			parent.strokeWeight(2);
			parent.noFill();
			parent.ellipse(0, 0, radius + 10, radius + 10);
			parent.strokeWeight(1);
			parent.pushMatrix();
			parent.rotate((1 - LoadSketch.flatness) * PConstants.PI / 2);
			parent.stroke(255, 100);
			float r = PApplet.max(50, 100 + ((1 - axis) * 200));
			r *= PApplet.sqrt(1 / LoadSketch.zoom);
			if (LoadSketch.zoom > 0.2) {
				parent.line(0, 0, 0, -r);
				parent.translate(0, -r - 5);
				parent.rotate(-PConstants.PI / 2);
				parent.scale(1 / LoadSketch.zoom);
				parent.fill(255, 200);
				parent.text(asset.quote, 0, 4);
			}
			parent.popMatrix();
			parent.fill(255, 255, 255, opacity);
		}
		else parent.fill(defaultColor);
		parent.noStroke();
		parent.ellipse(0, 0, radius, radius);
		parent.popMatrix();
	}
}