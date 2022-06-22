package swiftBot;

import swiftbot.SwiftBotAPI;

// Triangle shape with three sides
public class Triangle extends Shape {
	private static final int DRAWING_SPEED = 40;
	private double[] angles = new double[3];

	// Creates a triangle with the given side lengths
	public Triangle(int a, int b, int c) {
		this.type = "Triangle";
		this.dimensions = new int[]{a, b, c};
	}

	// Validates the triangle dimensions and ensures it satisfies the triangle inequality theorem
	// throws InvalidShapeException if the triangle is invalid
	public boolean validate() throws InvalidShapeException {
		if(!validateDimensions(15, 85)) {
			return false; // validateDimensions throws exception if invalid
		}

		int a = dimensions[0], b = dimensions[1], c = dimensions[2];

		// Check triangle inequality theorem
		if (!((a + b > c) && (a + c > b) && (b + c > a))) {
			throw new InvalidShapeException(
					"Invalid Triangle: T-" + a + "-" + b + "-" + c + "\n" +
							"Sides violate triangle inequality theorem!"
					);
		}

		return true;
	}

	// Calculates the internal angles of the triangle using the law of cosines to work out each angle precisely
	public void calculateAngles() {
		double a = dimensions[0], b = dimensions[1], c = dimensions[2];

		try {
			angles[0] = Math.toDegrees(Math.acos((b*b + c*c - a*a)/(2*b*c)));
			angles[1] = Math.toDegrees(Math.acos((a*a + c*c - b*b)/(2*a*c)));
			angles[2] = 180 - angles[0] - angles[1];
		} catch (Exception e) {
			// If there's a maths error, provide fallback values (this shouldn't happen if validate() is called first)
			System.out.println("Warning: Error calculating triangle angles: " + e.getMessage());
			angles[0] = 60;
			angles[1] = 60;
			angles[2] = 60;
		}
	}

	// Returns the internal angles of the triangle
	public double[] getAngles() {
		if (angles[0] == 0) { // If angles haven't been calculated yet
			calculateAngles();
		}
		return angles;
	}

	// Draws the triangle using the SwiftBot draw each side in sequence, turning by the appropriate exterior angle
	// throws InterruptedException if interrupted during drawing
	public void draw(SwiftBotAPI swiftBot, double cmToMs) throws InterruptedException {

		// Making sure angles are calculated
		calculateAngles();
		int[] sides = getDimensions();

		for(int i=0; i<3; i++) {
			move(swiftBot, DRAWING_SPEED, (int)(sides[i] * cmToMs));
			turn(swiftBot, DRAWING_SPEED, 180 - angles[i]);
		}
	}

	// Returns a string representation of the triangle
	public String toString() {
		return "Triangle with sides " + dimensions[0] + ", " + 
				dimensions[1] + ", " + dimensions[2] + " cm";
	}
}