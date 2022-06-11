package swiftBot;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import swiftbot.SwiftBotAPI;
import swiftbot.Button;
import swiftbot.ImageSize;

// Static class for QR code processing Enhanced with UI feedback and proper image handling
public class QRProcessor {
	// Time in milliseconds to wait between scan attempts (5 seconds)
	private static final int SCAN_DELAY = 5000;
	// Last scan time stamp to enforce delay
	private static long lastScanTime = 0;

	// Scan for QR code with proper UI feedback and timeout
	// Provides visual feedback during scanning and handle timeouts gracefully
	public static String scanQRCode(SwiftBotAPI swiftBot) {
		try {
			ConsoleUI.clearConsole();
			ConsoleUI.drawQRScanningHeader();
			System.out.println("\nPositioning camera... Please show QR code");

			// Safely enable X button for quick termination
			try {
				swiftBot.disableButton(Button.X);
				swiftBot.enableButton(Button.X, () -> {
					System.out.println("X button pressed");
					ShapeDrawer.setRunning(false);
				});
			} catch (Exception e) {
				System.out.println("Note: Could not set X button handler: " + e.getMessage());
			}

			int attempt = 1;
			int animFrame = 0;

			while (ShapeDrawer.isRunning()) {
				// Enforce delay between scan attempts
				long currentTime = System.currentTimeMillis();
				if (currentTime - lastScanTime < SCAN_DELAY) {
					ConsoleUI.drawScanningAnimation(animFrame++);
					Thread.sleep(100);
					continue;
				}
				lastScanTime = System.currentTimeMillis();

				// Clear any animation
				System.out.print("\r                                  \r");

				// For each scan attempt, show a complete progress animation from 0 to 100%
				for (int scanProgress = 0; scanProgress <= 100 && ShapeDrawer.isRunning(); scanProgress += 25) {
					ConsoleUI.clearConsole();
					ConsoleUI.drawQRScanningHeader();
					System.out.println("\nPositioning camera... Please show QR code");

					// Draw the progress bar for the current progress
					ConsoleUI.drawFancyProgressBar(scanProgress, "Processing QR code...");

					System.out.println("Scan attempt " + attempt);
					System.out.println("Press [X] to cancel scanning");

					// Wait a moment for visual effect unless we're at 100%
					if (scanProgress < 100) {
						Thread.sleep(500);
					}
				}

				// After showing 100% progress, try to detect QR code
				String decodedData = "";  // Initialise with empty string instead of null

				// Use the existing getQRImage method first
				BufferedImage qrImage = swiftBot.getQRImage();
				if (qrImage != null) {
					decodedData = swiftBot.decodeQRImage(qrImage);
					if (decodedData == null || decodedData.isEmpty()) {
						// Try with alternative image sizes if available
						try {
							BufferedImage qrImage480 = swiftBot.takeStill(ImageSize.SQUARE_480x480);
							if (qrImage480 != null) {
								decodedData = swiftBot.decodeQRImage(qrImage480);
							}

							if (decodedData == null || decodedData.isEmpty()) {
								BufferedImage qrImage720 = swiftBot.takeStill(ImageSize.SQUARE_720x720);
								if (qrImage720 != null) {
									decodedData = swiftBot.decodeQRImage(qrImage720);
								}
							}
						} catch (Exception ex) {
							System.out.println("Error with alternative image sizes: " + ex.getMessage());
						}
					}
				}

				// Initialise decodedData as empty string if null
				if (decodedData == null) {
					decodedData = "";
				}

				// Check if QR code was detected
				if (!decodedData.isEmpty()) {
					// Disable X button to prevent accidental termination
					swiftBot.disableButton(Button.X);

					ConsoleUI.clearConsole();
					ConsoleUI.drawSuccess("QR CODE DETECTED!");
					System.out.println("\nDecoded data: " + ConsoleUI.BRIGHT_YELLOW + decodedData + ConsoleUI.RESET);

					return decodedData;
				}

				attempt++;

				// Check if running has been set to false (X button pressed)
				if (!ShapeDrawer.isRunning()) {
					break; // Exit the loop if X button was pressed
				}
			}

			// Disable X button if we exit the loop
			swiftBot.disableButton(Button.X);
			return null;
		} catch (Exception e) {
			try {
				swiftBot.disableButton(Button.X);
			} catch (Exception ex) {
				// Silently ignore button disable errors during cleanup
			}
			ConsoleUI.drawErrorBox("QR scanning failed: " + e.getMessage());
			return null;
		}
	}

	// Parse QR code data into shapes with proper UI feedback
	// To handle multiple shape commands in the QR data and provide visual feedback during the parsing process
	// throws InvalidShapeException if a shape is invalid
	public static List<Shape> parseQR(String qrData) {
		List<Shape> shapes = new ArrayList<>();
		if(qrData == null) return shapes;

		ConsoleUI.clearConsole();
		ConsoleUI.drawHeader(ConsoleUI.BG_BRIGHT_GREEN + ConsoleUI.BLACK + " PARSING QR DATA " + ConsoleUI.RESET);
		System.out.println("Processing: " + ConsoleUI.BRIGHT_YELLOW + qrData + ConsoleUI.RESET + "\n");

		String[] commands = qrData.split("&");
		ArrayList<String> detectedShapeDescriptions = new ArrayList<>();

		// Add a counter to track total shapes (valid or not)
		final int MAX_SHAPES = 5;

		// Check the total number of commands first
		if(commands.length > MAX_SHAPES) {
			ConsoleUI.drawErrorBox("More than " + MAX_SHAPES + " shapes detected (" + commands.length + ").\n" +
					"Please use " + MAX_SHAPES + " or fewer shapes in your QR code.");
			// Return an empty list to indicate no shapes should be processed
			return shapes;
		}

		int totalShapesProcessed = 0;

		for(int i = 0; i < commands.length; i++) {
			String cmd = commands[i].trim();

			// Check remains as a safeguard, but we've already validated the max count
			if(shapes.size() >= MAX_SHAPES || totalShapesProcessed >= MAX_SHAPES) {
				System.out.println(ConsoleUI.YELLOW + "Maximum of " + MAX_SHAPES + " shapes reached. Ignoring remaining commands." + ConsoleUI.RESET);
				break; // Limit to 5 shapes
			}

			// Skip empty commands
			if(cmd.isEmpty()) {
				System.out.println(ConsoleUI.YELLOW + "Empty shape command found - skipping" + ConsoleUI.RESET);
				continue;
			}

			// Check for invalid format with missing shape type
			if(!cmd.toLowerCase().matches("[srt]-\\d+.*")) {
				System.out.println(ConsoleUI.RED + "Invalid shape format (missing shape type): " + cmd + ConsoleUI.RESET);
				continue;
			}

			// Increment the counter for each command we attempt to process
			totalShapesProcessed++;

			int progress = ((i + 1) * 100) / commands.length;
			ConsoleUI.drawFancyProgressBar(progress, "Processing QR code...");
			System.out.println("Processing command " + (i+1) + " of " + commands.length + ": " + ConsoleUI.BRIGHT_WHITE + cmd + ConsoleUI.RESET);

			String[] parts = cmd.split("-");
			if(parts.length < 2) {
				System.out.println(ConsoleUI.RED + "Invalid command format: " + cmd + ConsoleUI.RESET);
				continue;
			}

			try {
				Shape shape = createShape(parts);
				if(shape != null) {
					// Validate after creation but before adding to list
					if(shape.validate()) {
						shapes.add(shape);
						System.out.println(ConsoleUI.ORANGE + "-> Added " + shape.getType() + ConsoleUI.RESET);

						// Add to detected shapes for displaying in QR scanning UI
						if(shape.getType().equals("Square")) {
							detectedShapeDescriptions.add("SQUARE: " + ((Square)shape).getSide() + " cm");
						} else if(shape.getType().equals("Rectangle")) {
							Rectangle r = (Rectangle)shape;
							// Changed × to x for better compatibility
							detectedShapeDescriptions.add("RECTANGLE: " + r.getLength() + "x" + r.getWidth() + " cm");
						} else if(shape.getType().equals("Triangle")) {
							int[] sides = shape.getDimensions();
							detectedShapeDescriptions.add("TRIANGLE: " + sides[0] + "-" + sides[1] + "-" + sides[2] + " cm");
						}
					}
				} else {
					System.out.println(ConsoleUI.RED + "Could not create shape from: " + cmd + ConsoleUI.RESET);
				}
				Thread.sleep(300); // Short delay to show progress
			} catch(InvalidShapeException e) {
				// Log the error but continue processing other shapes
				System.out.println(ConsoleUI.RED + "Invalid shape: " + e.getMessage() + ConsoleUI.RESET);
			} catch(NumberFormatException e) {
				System.out.println(ConsoleUI.RED + "Invalid number in: " + cmd + " - " + e.getMessage() + ConsoleUI.RESET);
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		// Show detected shapes in a stylish format
		if (!shapes.isEmpty()) {
			ConsoleUI.clearConsole();

			// Use a simple header
			ConsoleUI.drawHeader(ConsoleUI.BG_BRIGHT_CYAN + ConsoleUI.BLACK + " SHAPES DETECTED " + ConsoleUI.RESET);

			System.out.println(ConsoleUI.BRIGHT_WHITE + "Detected:" + ConsoleUI.RESET);
			for (int i = 0; i < detectedShapeDescriptions.size(); i++) {
				String shape = detectedShapeDescriptions.get(i);
				if (shape.startsWith("SQUARE")) {
					System.out.println(ConsoleUI.ORANGE + "[] " + ConsoleUI.RESET + shape);
				} else if (shape.startsWith("TRIANGLE")) {
					System.out.println(ConsoleUI.BLUE + "/\\ " + ConsoleUI.RESET + shape);
				} else if (shape.startsWith("RECTANGLE")) {
					System.out.println(ConsoleUI.PURPLE + "[ ] " + ConsoleUI.RESET + shape);
				}
			}

			System.out.println("\n" + ConsoleUI.CYAN + "Validating parameters..." + ConsoleUI.RESET);

			// Pause to show the detection screen
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		// Show final results
		ConsoleUI.clearConsole();
		System.out.println();
		System.out.println(ConsoleUI.GREEN + "Successfully parsed " + shapes.size() + " shapes" + ConsoleUI.RESET);
		for(Shape shape : shapes) {
			if(shape.getType().equals("Square")) {
				System.out.println(ConsoleUI.GREEN + "[] Square: " + ((Square)shape).getSide() + " cm" + ConsoleUI.RESET);
			} else if(shape.getType().equals("Rectangle")) {
				Rectangle r = (Rectangle)shape;
				// Changed × to x for better compatibility
				System.out.println(ConsoleUI.PURPLE + "[ ] Rectangle: " + r.getLength() + "x" + r.getWidth() + " cm" + ConsoleUI.RESET);
			} else if(shape.getType().equals("Triangle")) {
				int[] sides = shape.getDimensions();
				System.out.println(ConsoleUI.BLUE + "/\\ Triangle: " + sides[0] + "-" + sides[1] + "-" + sides[2] + " cm" + ConsoleUI.RESET);
			}
		}

		return shapes;
	}

	// Create a shape object based on parsed command parts
	// To handle the three different shape types (S, R, T) and validate their parameters before returning the shape object
	// throws NumberFormatException if number parsing fails
	private static Shape createShape(String[] parts) throws NumberFormatException {
		Shape shape = null;

		// Trim each part to handle potential whitespace
		for (int i = 0; i < parts.length; i++) {
			parts[i] = parts[i].trim();
		}

		switch(parts[0].toUpperCase()) {
		case "S":
			if(parts.length == 2) {
				try {
					int side = Integer.parseInt(parts[1]);
					shape = new Square(side);
				} catch (NumberFormatException e) {
					throw new NumberFormatException("Square requires a numeric side length");
				}
			} else {
				System.out.println(ConsoleUI.YELLOW + "Square must have exactly 1 dimension parameter" + ConsoleUI.RESET);
			}
			break;
		case "T":
			if(parts.length == 4) {
				try {
					int side1 = Integer.parseInt(parts[1]);
					int side2 = Integer.parseInt(parts[2]);
					int side3 = Integer.parseInt(parts[3]);

					// Check triangle inequality theorem before creating the triangle
					if ((side1 + side2 > side3) && (side1 + side3 > side2) && (side2 + side3 > side1)) {
						shape = new Triangle(side1, side2, side3);
					} else {
						System.out.println(ConsoleUI.RED + "Invalid Triangle: Sides violate triangle inequality theorem!" + ConsoleUI.RESET);
					}
				} catch (NumberFormatException e) {
					throw new NumberFormatException("Triangle requires 3 numeric parameters");
				}
			} else {
				System.out.println(ConsoleUI.YELLOW + "Triangle must have exactly 3 dimension parameters" + ConsoleUI.RESET);
			}
			break;
		case "R":
			if(parts.length == 3) {
				try {
					int length = Integer.parseInt(parts[1]);
					int width = Integer.parseInt(parts[2]);
					shape = new Rectangle(length, width);
				} catch (NumberFormatException e) {
					throw new NumberFormatException("Rectangle requires 2 numeric parameters");
				}
			} else {
				System.out.println(ConsoleUI.YELLOW + "Rectangle must have exactly 2 dimension parameters" + ConsoleUI.RESET);
			}
			break;
		default:
			System.out.println(ConsoleUI.RED + "Unknown shape type: " + parts[0] + ConsoleUI.RESET);
		}

		return shape;
	}
}