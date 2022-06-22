package swiftBot;

import swiftbot.SwiftBotAPI;

// Square shape with a single side dimension
public class Square extends Shape {
	private static final int DRAWING_SPEED = 40;

	// Creates a square with the specified side length
	public Square(int side) {
		this.type = "Square";
		this.dimensions = new int[]{side};
	}

	// Gets the side length of this square
	public int getSide() { 
		return dimensions[0]; 
	}

	// Validates that the square's side length is within acceptable range and throws InvalidShapeException if the side length is invalid
	public boolean validate() throws InvalidShapeException {
		return validateDimensions(15, 85);
	}

	// Draws the square using the SwiftBot
	// I've implemented this to draw a perfect square with 4 equal sides and 90-degree angles.
	// throws InterruptedException if interrupted during drawing
	public void draw(SwiftBotAPI swiftBot, double cmToMs) throws InterruptedException {

		int sideTime = (int)(getSide() * cmToMs);

		// Draw square: 4 sides with 90 degree turns
		for(int i=0; i<4; i++) {
			move(swiftBot, DRAWING_SPEED, sideTime);
			turn(swiftBot, DRAWING_SPEED, 90);
		}
	}

	// Returns a string representation of this square
	public String toString() {
		return "Square with side " + getSide() + " cm";
	}
}