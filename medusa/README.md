
PROJECT MEDUSA.
---------------

Data Visualization of analysis results using Processing but in Eclipse IDE.

Getting Started:

* Launch Eclipse and create workspace by pointing to "medusa" folder. 

* It uses `watchlist_2013.xml` in /src folder. Feel free to add more and edit LoadSketch.java

- Its includes Leap Motion support or can also use keyboard/mouse (see Controls below)

Adapted from on Kepler Visualization code by visual artist Jer Thorp @ blprnt@blprnt.com (awesome inspiration)
http://blog.blprnt.com/blog/blprnt/data-in-an-alien-context-kepler-visualization-source-code

Controls:

Mouse hover or type quote to show info.
Double-click to scale render size by half.
ARROW UP / DOWN to zoom in/out.
Toggle between view modes with the keys below:

	` - Undo sort / restore flatness
	1 - Sort by Fund Ranking (color)
	2 - Sort by Fund Ranking (color)
	3 - Sort by Number of Parents i.e. top competitive companies
	4 - Restore tilt
	5 - Restore zoom
	6 - Change view mode / toggle flatness

Using Leap Motion API in Processing
 
 Hand Orientation - control view/plane
 Swipe Gesture - Change view mode
 Screen tap - Restore tilt / zoom / flatness and undo sort
 Twirl (make circles) with one finger - Zoom in / out
	- While zooming in / out i.e. twirling finger, make fist to hold zoom!
	- If too zooming out, point & hold is disabled!
  Point & Hold Index Finger - To select (for 2s)
	- Requires to be zoomed in to hover enabled level!
	
 While zoomed out (i.e. hover disabled), hold out [n] finger for 5s to:
 	[1] - Sort by Fund Ranking (color)
 	[2] - Sort by Fund Ranking (color)
 	[3] - Sort by Number of Parents i.e. top competitive companies
     Pinky finger - does nothing!
