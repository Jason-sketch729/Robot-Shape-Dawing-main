package swiftBot;

import swiftbot.SwiftBotAPI;

// Rectangle shape with length and width dimensions
public class Rectangle extends Shape {
	private static final int DRAWING_SPEED = 40;

	// Creates a rectangle with the specified dimensions
	public Rectangle(int length, int width) {
		this.type = "Rectangle";
		this.dimensions = new int[]{length, width};
	}

	// Gets the length of this rectangle
	public int getLength() { 
		return dimensions[0]; 
	}

	// Gets the width of this rectangle
	public int getWidth() { 
		return dimensions[1]; 
	}

	// Validates that the rectangle's dimensions are within acceptable range and throws InvalidShapeException if either dimension is invalid
	public boolean validate() throws InvalidShapeException {
		return validateDimensions(15, 85);
	}

	// Draws the rectangle using the SwiftBot alternating between length and width dimensions
	// throws InterruptedException if interrupted during drawing
	public void draw(SwiftBotAPI swiftBot, double cmToMs) throws InterruptedException {

		int lengthTime = (int)(getLength() * cmToMs);
		int widthTime = (int)(getWidth() * cmToMs);

		for(int i=0; i<2; i++) {
			move(swiftBot, DRAWING_SPEED, lengthTime);
			turn(swiftBot, DRAWING_SPEED, 90);
			move(swiftBot, DRAWING_SPEED, widthTime);
			turn(swiftBot, DRAWING_SPEED, 90);
		}
	}

	// Returns a string representation of this rectangle
	public String toString() {
		return "Rectangle with length " + getLength() + 
				" cm and width " + getWidth() + " cm";
	}
}