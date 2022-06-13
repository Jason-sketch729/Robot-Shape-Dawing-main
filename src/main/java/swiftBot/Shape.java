package swiftBot;

import swiftbot.SwiftBotAPI;

//Abstract base class for all shapes that can be drawn by the SwiftBot
public abstract class Shape {
	protected String type;
	protected int[] dimensions;

	// Validates the shape dimensions to ensure they're within acceptable ranges
	// returns true if shape is valid and throws InvalidShapeException if shape has invalid dimensions
	public abstract boolean validate() throws InvalidShapeException;

	// Draws the shape using the SwiftBot and throws InterruptedException if interrupted during drawing
	public abstract void draw(SwiftBotAPI swiftBot, double cmToMs) throws InterruptedException;

	// Gets the type of this shape and return the shape type as a string
	public String getType() { 
		return type; 
	}

	// Gets the dimensions of this shape and returns array of dimensions in centimeter
	public int[] getDimensions() { 
		return dimensions; 
	}

	// Validates that all dimensions are within specified min and max bounds
	// throws InvalidShapeException if any dimension is out of range
	protected boolean validateDimensions(int min, int max) throws InvalidShapeException {
		for(int dim : dimensions) {
			if(dim < min || dim > max) {
				throw new InvalidShapeException(
						"Invalid " + type + " dimensions. All sides must be between " + 
								min + " and " + max + " cm."
						);
			}
		}
		return true;
	}

	// Moves the SwiftBot forward with pause-aware functionalities handle long movements by breaking them into smaller chunks
	// throws InterruptedException if interrupted during movement
	protected void move(SwiftBotAPI swiftBot, int speed, int duration) throws InterruptedException {
		// Check if we should terminate first
		if (!ShapeDrawer.isRunning()) {
			// Making sure robot stops when terminated
			swiftBot.move(0, 0, 100);
			return;
		}

		// For small movements, just do them directly
		if (duration < 500) {
			swiftBot.move(speed, speed, duration);
			// Check again after movement
			if (!ShapeDrawer.isRunning()) {
				swiftBot.move(0, 0, 100);
			}
			return;
		}

		// For longer movements, splitting them up to be able to pause
		int remaining = duration;
		int segmentSize = 100; // Smaller segments to check more frequently

		while (remaining > 0 && ShapeDrawer.isRunning()) {
			// Check if paused
			while (ShapeDrawer.isPaused() && ShapeDrawer.isRunning()) {
				swiftBot.move(0, 0, 100); // Keep robot stationary
				Thread.sleep(100);
			}

			// If programme is no longer running then exit
			if (!ShapeDrawer.isRunning()) {
				swiftBot.move(0, 0, 100);
				return;
			}

			// Move for a segment or the remaining duration, whichever is smaller
			int moveTime = Math.min(segmentSize, remaining);
			swiftBot.move(speed, speed, moveTime);
			remaining -= moveTime;

			// Check if we need to stop after each segment
			if (!ShapeDrawer.isRunning()) {
				swiftBot.move(0, 0, 100);
				return;
			}
		}
	}

	// Turns the SwiftBot with pause-aware functionality so the robot can be paused mid-operation.
	// throws InterruptedException if interrupted during turning
	protected void turn(SwiftBotAPI swiftBot, int speed, double degrees) throws InterruptedException {
		// Check if we should terminate first
		if (!ShapeDrawer.isRunning()) {
			// Make sure robot stops when terminated
			swiftBot.move(0, 0, 100);
			return;
		}

		int duration = (int)(degrees * 6.65);

		// For small turns, just do them directly
		if (duration < 500) {
			swiftBot.move(speed, -speed, duration);
			// Check again after movement
			if (!ShapeDrawer.isRunning()) {
				swiftBot.move(0, 0, 100);
			}
			return;
		}

		// For longer turns, split them up to be able to pause
		int remaining = duration;
		int segmentSize = 100; // Smaller segments to check more frequently

		while (remaining > 0 && ShapeDrawer.isRunning()) {
			// Check if paused
			while (ShapeDrawer.isPaused() && ShapeDrawer.isRunning()) {
				swiftBot.move(0, 0, 100); // Keep robot stationary
				Thread.sleep(100);
			}

			// If programme is no longer running, exit
			if (!ShapeDrawer.isRunning()) {
				swiftBot.move(0, 0, 100);
				return;
			}

			// Turn for a segment or the remaining duration, whichever is smaller
			int turnTime = Math.min(segmentSize, remaining);
			swiftBot.move(speed, -speed, turnTime);
			remaining -= turnTime;

			// Check if we need to stop after each segment
			if (!ShapeDrawer.isRunning()) {
				swiftBot.move(0, 0, 100);
				return;
			}
		}
	}
}