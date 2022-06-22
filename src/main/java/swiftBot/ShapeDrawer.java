package swiftBot;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import swiftbot.SwiftBotAPI;
import swiftbot.Button;

// Main application class for drawing shapes with SwiftBot Enhanced with colourful UI and visual elements
public class ShapeDrawer {
	private static SwiftBotAPI swiftBot;
	private static final AtomicBoolean running = new AtomicBoolean(true);
	private static final AtomicBoolean paused = new AtomicBoolean(false);
	private static final double CM_TO_MS = 60;
	private static final List<Shape> drawnShapes = new ArrayList<>();
	private static final List<Long> drawTimes = new ArrayList<>();
	private static final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
	private static final String AUTHOR = "JAWAD";

	// Flags for controlling program behaviour
	private static boolean cleanupExecuted = false;
	private static final AtomicBoolean currentlyDrawing = new AtomicBoolean(false);

	private static String logFileName = "/data/home/pi/logFile.txt";

	// Setup keyboard listener for pause/resume/exit commands
	// Implemented this as a non-blocking approach to handle user input without interrupting the main program flow
	private static void setupKeyboardListener() {
		new Thread(() -> {
			Scanner controlScanner = new Scanner(System.in);
			try {
				while(running.get()) {
					try {
						// Read line if available, non-blocking approach
						if (controlScanner.hasNextLine()) {
							String input = controlScanner.nextLine().trim().toUpperCase();
							// Add to queue for main thread to process
							inputQueue.offer(input);

							// Also handle immediate commands
							if(input.equals("P")) {
								paused.set(true);
								System.out.println("Pausing operation...");
							} else if(input.equals("R")) {
								if (paused.get()) {
									System.out.println("Resuming operation...");
									paused.set(false);
								}
							} else if(input.equals("X")) {
								System.out.println("X button pressed. Terminating program...");
								// Set running to false to trigger natural program termination
								running.set(false);
							}
						} else {
							// Sleep a bit to prevent CPU hogging
							Thread.sleep(100);
						}
					} catch (Exception e) {
						// More gentle error handling - just log and continue
						if (!(e instanceof java.util.NoSuchElementException)) {
							System.err.println("Input handling: " + e.getMessage());
						}
						// Brief pause to prevent excessive error logging
						Thread.sleep(500);
					}
				}
			} catch (Exception e) {
				System.err.println("Keyboard listener terminated: " + e.getMessage());
			} finally {
				controlScanner.close();
			}
		}).start();

		// Added SwiftBot hardware X button listener
		try {
			swiftBot.disableButton(Button.X);
			swiftBot.enableButton(Button.X, () -> {
				System.out.println("Hardware X button pressed. Terminating program...");
				running.set(false);
				// Add this line to make sure the robot stops immediately
				try {
					swiftBot.move(0, 0, 500);
				} catch (Exception ex) {
					// Ignore any errors when stopping
				}
				// Make sure the under lights are turned off
				try {
					swiftBot.disableUnderlights();
				} catch (Exception ex) {
					// Ignore any errors when disabling lights
				}
			});
		} catch (Exception e) {
			System.err.println("Warning: Could not enable hardware X button: " + e.getMessage());
		}
	}

	// Main entry point - Structured this to ensure proper initialisation, UI presentation, graceful shutdown in all scenarios
	public static void main(String[] args) {
		initialiseRobot();
		setupEmergencyStopSystem();  // Make sure this is called
		setupKeyboardListener();

		// Display welcome screen with branding
		ConsoleUI.clearConsole();
		ConsoleUI.drawWelcomeScreen();

		// Wait for user input
		boolean startScanning = true;
		while(running.get()) {
			String input = waitForInput(-1);  // -1 means wait indefinitely
			if ("X".equals(input)) {
				running.set(false);
				startScanning = false;
				break;
			} else if (input.isEmpty() || input.equals("\n")) {
				// User pressed ENTER or didn't type anything specific
				break;  // Exit the wait loop and proceed with scanning
			}
			// If we get here, it was another key press, so continue waiting
		}

		if (!startScanning || !running.get()) {
			// Important: Call cleanup directly here instead of just exiting
			cleanup();
			System.exit(0);
		}

		// Register shutdown hook for proper cleanup
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			cleanup();
		}));

		while(running.get()) {
			processQRCommands();

			// Short pause between scan attempts
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		// This is the proper cleanup call that should run for ALL termination paths
		cleanup();

		// Only exit after cleanup has fully completed
		System.exit(0);
	}

	// Wait for user input with timeout to support both indefinite waiting and timeout-based waiting for flexible UI
	private static String waitForInput(long timeoutMillis) {
		try {
			String input;
			if (timeoutMillis < 0) {
				// Wait indefinitely until input is available
				input = inputQueue.take();  // This blocks until an element is available
			} else {
				// Wait with timeout
				input = inputQueue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
			}
			return input != null ? input.trim().toUpperCase() : "";
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return "";
		}
	}

	// Process QR commands by scanning and executing shapes
	// I've structured this to handle QR scanning, parsing, and error handling with appropriate user feedback at each stage
	private static void processQRCommands() {
		try {
			// Scan for QR code - this method has proper UI feedback
			String qrData = QRProcessor.scanQRCode(swiftBot);
			if(qrData == null) return;

			// Parse QR data - this method has proper UI feedback
			List<Shape> shapes = QRProcessor.parseQR(qrData);

			// Special check to distinguish between "no shapes due to too many" vs "no valid shapes"
			String[] commands = qrData.split("&");
			if(shapes.isEmpty() && commands.length > 5) {
				// I already showed the "too many shapes" error in parseQR, so just return without additional errors
				return;
			}

			// Original check for empty shapes (only runs if not too many shapes)
			if(shapes.isEmpty()) {
				ConsoleUI.drawErrorBox("No valid shapes found in QR code");
				return;
			}

			// Execute the shapes
			executeShapeSequence(shapes);

		} catch(Exception e) {
			if (e.getMessage() != null && e.getMessage().contains("triangle inequality")) {
				ConsoleUI.drawErrorBox("Invalid Triangle: Sides violate triangle\ninequality theorem!");
				System.out.println("Action: Rescan QR code.");
				System.out.println("Press [X] to exit.");
			} else {
				ConsoleUI.drawErrorBox("Processing error: " + e.getMessage());
				System.out.println("Action: Rescan QR code.");
				System.out.println("Press [X] to exit.");

				// Wait for user to acknowledge error
				waitForInput(2000);  // Wait up to 2 seconds
			}
		}
	}

	// Execute a sequence of shapes with proper UI feedback
	// I've implemented this to handle multiple shapes in sequence with user feedback, allowing for pausing and cancellation between shapes
	// throws InterruptedException if interrupted during execution
	private static void executeShapeSequence(List<Shape> shapes) throws InterruptedException {
		int shapeIndex = 1;
		int totalShapes = shapes.size();

		for(Shape shape : shapes) {
			// Check if we should pause or exit
			if (!running.get()) break;
			handlePauseState();

			displayShapeWithUI(shape, shapeIndex, totalShapes);

			// Confirm before drawing
			System.out.println("Please Wait for Shape Drawing or [X] to cancel");
			String input = waitForInput(5000);  // Wait up to 5 seconds
			if ("X".equals(input) || !running.get()) {
				break;
			}

			// Draw the shape with timing
			long start = System.currentTimeMillis();
			try {
				drawShapeWithFeedback(shape, start, shapeIndex < totalShapes);

				// Only reposition if there are more shapes to draw and we're still running
				if (shapeIndex < totalShapes && running.get()) {
					repositionForNextShape();
					// Short pause between shapes
					Thread.sleep(1000);
				}
			} catch (Exception e) {
				ConsoleUI.drawErrorBox("Error drawing shape: " + e.getMessage());
				System.out.println("Trying to reset robot position...");

				// Try to reset robot position
				try {
					// Move backward a bit to create space
					swiftBot.move(-30, -30, 1000);
					Thread.sleep(500);
					// Stop all movement
					swiftBot.move(0, 0, 500);
				} catch (Exception ex) {
					// If that fails, just ignore
				}

				// Wait for user acknowledgment
				System.out.println("Press [X] to exit or wait to Continue");
				String errorInput = waitForInput(2000);  // Wait up to 2 second
				if ("X".equals(errorInput) || !running.get()) {
					running.set(false);
					return;
				}
			}

			shapeIndex++;
		}
	}

	// Calculate a fixed draw time for shape visualisation
	// I've used simple fixed values based on shape type for consistent UI feedback.
	private static int calculateEstimatedDrawTime(Shape shape) {
		// Simple fixed values based on shape type
		if (shape instanceof Square) {
			return 15;  // Fixed time for square
		} 
		else if (shape instanceof Rectangle) {
			return 18;  // Fixed time for rectangle
		} 
		else if (shape instanceof Triangle) {
			return 20;  // Fixed time for triangle
		}

		// Default fallback
		return 15;
	}

	// Draw a shape with real-time feedback
	// I've designed this to show an accurate progress display while the robot draws, with appropriate lighting effects and status updates
	// throws InterruptedException if interrupted during drawing
	private static void drawShapeWithFeedback(Shape shape, long startTime, boolean hasMoreShapes) throws InterruptedException {
		// Get a realistic estimate
		int estimatedDrawTime = calculateEstimatedDrawTime(shape);
		String dimensionStr = getShapeDimensionString(shape);

		// We'll use this to signal when the actual drawing is complete
		final boolean[] drawingComplete = new boolean[1];

		// This thread will show a smooth count down regardless of actual completion
		Thread progressThread = new Thread(() -> {
			try {
				// We'll manually count seconds for display
				for (int elapsed = 0; elapsed <= estimatedDrawTime; elapsed++) {
					// Check if program was terminated
					if (!running.get() || Thread.currentThread().isInterrupted()) {
						break;
					}

					// Check for X button input
					String input = waitForInput(10); // Check briefly without blocking too long
					if ("X".equals(input)) {
						running.set(false); // Set running to false to trigger termination
						Thread.currentThread().interrupt(); // Interrupt this thread
						break;
					}

					int remaining = estimatedDrawTime - elapsed;
					boolean isFinished = drawingComplete[0];
					int progress;

					if (isFinished) {
						progress = 100;
					} else {
						progress = (elapsed * 100) / estimatedDrawTime;
					}

					// Check again if program was terminated
					if (!running.get()) {
						break;
					}

					// Don't update if paused
					if (paused.get()) {
						ConsoleUI.drawEnhancedPauseScreen(shape.getType(), dimensionStr, progress);
						Thread.sleep(1000);
						elapsed--;
						continue;
					}

					// Update display
					ConsoleUI.clearConsole();
					ConsoleUI.drawShapeDrawingStatus(
							shape.getType(), 
							dimensionStr, 
							elapsed, 
							remaining, 
							progress
							);

					if (isFinished && remaining == 0) {
						break;
					}

					// Sleep slightly less to accommodate the input check
					// Check running flag more frequently by using shorter sleep intervals
					for (int i = 0; i < 10; i++) {
						if (!running.get()) {
							break;
						}
						Thread.sleep(99);
					}
				}
			} catch (InterruptedException e) {
				// Thread was interrupted
			}
		});

		progressThread.start();

		try {
			// Set drawing flag to true
			currentlyDrawing.set(true);

			// Make sure under lights are off during drawing
			swiftBot.disableUnderlights();

			// Ensure robot is stationary before drawing
			swiftBot.move(0, 0, 500);

			// Set lights based on shape
			if (shape instanceof Square) {
				swiftBot.fillUnderlights(new int[]{255, 165, 0}); // Orange for Square
			} else if (shape instanceof Rectangle) {
				swiftBot.fillUnderlights(new int[]{255, 0, 255}); // Purple for Rectangle
			} else if (shape instanceof Triangle) {
				swiftBot.fillUnderlights(new int[]{0, 0, 255}); // Blue for Triangle
			}

			// Check if terminated before drawing
			if (!running.get()) {
				// Clean up and exit if terminated
				swiftBot.disableUnderlights();
				return;
			}

			// Actually draw the shape
			shape.draw(swiftBot, CM_TO_MS);

			// Check again if program was terminated during drawing
			if (!running.get()) {
				// Clean up and exit if terminated
				swiftBot.disableUnderlights();
				return;
			}

			// Mark drawing as complete only if we got here (complete drawing and still running)
			drawingComplete[0] = true;
			currentlyDrawing.set(false);

			// Wait for the progress thread to reach 100% naturally
			// but with a timeout to prevent waiting too long
			long waitStart = System.currentTimeMillis();
			while (progressThread.isAlive() && System.currentTimeMillis() - waitStart < 5000 && running.get()) {
				Thread.sleep(100);
			}

			// Shape completed - blink green for 1 second ONLY if we're still running
			if (running.get()) {
				swiftBot.fillUnderlights(new int[]{0, 255, 0});
				Thread.sleep(1000);
				swiftBot.disableUnderlights();

				// Now show completion message and record shape data
				long drawTime = System.currentTimeMillis() - startTime;
				// Only record the shape if drawing was actually completed
				recordShapeData(shape, drawTime);
				int drawTimeSeconds = (int)(drawTime / 1000);
				ConsoleUI.drawCompletionMessage(shape.getType(), drawTimeSeconds, hasMoreShapes);
			}
		} finally {
			// Make sure drawing flag is false in any case
			currentlyDrawing.set(false);

			// Make sure lights are off
			try {
				swiftBot.disableUnderlights();
			} catch (Exception e) {
				// Ignore errors when cleaning up
			}

			// Ensure thread is stopped
			progressThread.interrupt();
			try {
				progressThread.join(1000); // Wait up to 1 second for thread to end
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	// Handle pause state - Implemented this to properly pause robot movement and display
	// throws InterruptedException if interrupted
	private static void handlePauseState() throws InterruptedException {
		if (paused.get() && running.get()) {
			// Stop robot movement when entering pause state
			try {
				swiftBot.move(0, 0, 500);
			} catch (Exception e) {
				System.err.println("Error stopping robot during pause: " + e.getMessage());
			}

			// Display pause screen
			Shape lastShape = drawnShapes.isEmpty() ? null : drawnShapes.get(drawnShapes.size()-1);
			String dimensionStr = lastShape != null ? getShapeDimensionString(lastShape) : "Unknown";
			ConsoleUI.drawEnhancedPauseScreen(
					lastShape != null ? lastShape.getType() : "Unknown", 
							dimensionStr, 50);

			System.out.println("PAUSED: Press R to resume or X to exit (X works only after resuming)");

			// Wait in paused state
			while (paused.get() && running.get()) {
				Thread.sleep(200);
			}

			// Add a small delay after resuming to ensure UI updates
			if (running.get()) {
				System.out.println(ConsoleUI.YELLOW + "Resuming operation..." + ConsoleUI.RESET);
				Thread.sleep(1000);
			}
		}
	}

	// Check if program is running
	public static boolean isRunning() {
		return running.get();
	}

	// Set the running state
	public static void setRunning(boolean value) {
		running.set(value);
	}

	// Check if the drawing is currently paused
	public static boolean isPaused() {
		return paused.get();
	}

	// Reposition the robot for the next shape
	// I've created this to ensure the robot has enough space to draw the next shape by moving forward slightly after completing a shape
	// throws InterruptedException if interrupted
	private static void repositionForNextShape() throws InterruptedException {
		try {
			// Stop first
			swiftBot.move(0, 0, 500);

			// Move forward to create space for the next shape
			swiftBot.move(40, 40, (int)(15 * CM_TO_MS));

			// Stop again after repositioning
			swiftBot.move(0, 0, 500);
		} catch (Exception e) {
			System.out.println("Warning: Error during repositioning: " + e.getMessage());
			// Try a simpler movement if the original fails
			try {
				swiftBot.move(30, 30, 1000);
				swiftBot.move(0, 0, 500);
			} catch (Exception ex) {
				// If that also fails, just log it
				System.out.println("Could not reposition robot: " + ex.getMessage());
			}
		}
	}

	// Initialise the robot - Kept this simple and robust to handle initialisation errors properly
	private static void initialiseRobot() {
		try {
			swiftBot = new SwiftBotAPI();

			// Add this section immediately after creating the SwiftBot
			// Set up an immediate emergency stop for the X button
			swiftBot.disableButton(Button.X);
			swiftBot.enableButton(Button.X, () -> {
				System.out.println("\n\nHARDWARE X BUTTON PRESSED - EMERGENCY STOP");
				// Force stop the robot
				try {
					swiftBot.move(0, 0, 100);
					swiftBot.disableUnderlights();
				} catch (Exception ex) {
					// Ignore errors during emergency stop
				}
				// Force terminate the program - more drastic but will work
				System.exit(0);
			});

		} catch(Exception e) {
			System.err.println("CRITICAL ERROR: Robot initialisation failed: " + e.getMessage());
			System.exit(1);
		}
	}

	// Emergency stop system for immediate termination when X button is pressed
	private static void setupEmergencyStopSystem() {
		// Create an atomic flag that will be checked throughout the code
		final AtomicBoolean emergencyStopRequested = new AtomicBoolean(false);

		// Create a dedicated thread to check for emergency stop
		new Thread(() -> {
			try {
				while (true) {
					// Setup the X button handler repeatedly in case it gets overwritten
					try {
						swiftBot.disableButton(Button.X);
						swiftBot.enableButton(Button.X, () -> {
							System.out.println("\n" + ConsoleUI.RED + "EMERGENCY STOP - X BUTTON PRESSED" + ConsoleUI.RESET);
							emergencyStopRequested.set(true);
							running.set(false);

							// Try to stop movement immediately
							try {
								swiftBot.move(0, 0, 100);
								swiftBot.disableUnderlights();
							} catch (Exception ex) {
								// Ignore errors
							}

							// Make sure cleanup is always called before exit
							cleanup();
							System.exit(0);
						});
					} catch (Exception e) {
						// If button setup fails, just continue
					}

					// Check if emergency stop was requested
					if (emergencyStopRequested.get()) {
						System.out.println(ConsoleUI.RED + "Emergency stop executing..." + ConsoleUI.RESET);
						cleanup();
						System.exit(0);
					}

					Thread.sleep(100);
				}
			} catch (Exception e) {
				System.out.println("Emergency system failed: " + e.getMessage());
			}
		}).start();
	}

	// Clean up resources before exit
	// I've made this method robust to ensure it runs only once and properly stops all movement, disables lights, and creates the log file
	private static void cleanup() {
		// Only run cleanup once
		if (cleanupExecuted) {
			return;
		}

		cleanupExecuted = true;

		if (swiftBot != null) {
			try {
				// Stop all movement
				swiftBot.move(0, 0, 500);

				// Make sure under lights are disabled properly
				// Try multiple times with delays to ensure it takes effect
				for (int i = 0; i < 3; i++) {
					swiftBot.disableUnderlights();
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			} catch (Exception e) {
				System.err.println(ConsoleUI.RED + "Error during cleanup: " + e.getMessage() + ConsoleUI.RESET);
			}
		}
		running.set(false);

		// Display summary at end
		int squares = countShapesByType("Square");
		int triangles = countShapesByType("Triangle");
		int rectangles = countShapesByType("Rectangle");
		double averageTime = calculateAverageTime() / 1000.0; // Convert to seconds

		// Use the base filename - createLogFile will generate the unique name
		String baseLogFileName = "/data/home/pi/logFile.txt";

		// Create the log file only once
		createLogFile(baseLogFileName);

		// Display the termination summary with the actual filename (updated by createLogFile)
		ConsoleUI.drawTerminationSummary(
				logFileName,
				squares,
				triangles,
				rectangles,
				averageTime
				);
	}

	// Count the number of shapes by type
	private static int countShapesByType(String type) {
		int count = 0;
		for (Shape shape : drawnShapes) {
			if (shape.getType().equals(type)) {
				count++;
			}
		}
		return count;
	}

	// Display shape with enhanced UI
	// I've designed this to show a visual preview of the shape before drawing for better user understanding
	private static void displayShapeWithUI(Shape shape, int index, int total) {
		ConsoleUI.clearConsole();
		ConsoleUI.drawHeader(ConsoleUI.BG_BRIGHT_YELLOW + ConsoleUI.BLACK + " SHAPE " + 
				index + "/" + total + ": " + 
				shape.getType().toUpperCase() + " (" + getShapeCode(shape) + ") " + 
				ConsoleUI.RESET);

		ConsoleUI.drawShapePreview(shape.getType(), shape.getDimensions());

		System.out.println("Status: Ready");
	}

	// Get shape code for display
	private static String getShapeCode(Shape shape) {
		if (shape instanceof Square) {
			return "S-" + ((Square)shape).getSide();
		} else if (shape instanceof Rectangle) {
			Rectangle rect = (Rectangle)shape;
			return "R-" + rect.getLength() + "-" + rect.getWidth();
		} else if (shape instanceof Triangle) {
			int[] sides = shape.getDimensions();
			return "T-" + sides[0] + "-" + sides[1] + "-" + sides[2];
		}
		return "";
	}

	// Record shape data
	private static void recordShapeData(Shape shape, long duration) {
		drawnShapes.add(shape);
		drawTimes.add(duration);
	}

	// Create log file with drawing session data
	// I've implemented this to generate a uniquely time stamped log file containing detailed information about all shapes drawn in the session
	private static void createLogFile(String baseFilename) {
		try {
			// Create a unique filename with time stamp using the requested format
			// Format: logFile_DD|MM|YYYY_HH|MM|SS.txt
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd':'MM':'yyyy_HH':'mm':'ss");
			String timestamp = dateFormat.format(new Date());

			// Split the baseFilename to insert time stamp before the extension
			String directory = baseFilename.substring(0, baseFilename.lastIndexOf("/") + 1);
			String filename = baseFilename.substring(baseFilename.lastIndexOf("/") + 1);
			String filenameWithoutExt = filename;
			String extension = "";

			if (filename.contains(".")) {
				filenameWithoutExt = filename.substring(0, filename.lastIndexOf("."));
				extension = filename.substring(filename.lastIndexOf("."));
			}

			// Create the new unique filename
			String uniqueFilename = directory + filenameWithoutExt + "_" + timestamp + extension;

			// Update the logFileName to be used in UI display
			logFileName = uniqueFilename;

			try(FileWriter writer = new FileWriter(uniqueFilename)) {
				writer.write("=== Drawing Session Log ===\n");
				writer.write("Generated: " + new Date() + "\n");
				writer.write("Created by: " + AUTHOR + "\n\n");

				// Shapes drawn with detailed information
				writer.write("=== Shapes Drawn ===\n");
				if (!drawnShapes.isEmpty()) {
					for(int i=0; i<drawnShapes.size(); i++) {
						Shape s = drawnShapes.get(i);
						StringBuilder shapeInfo = new StringBuilder();

						if (s.getType().equals("Square")) {
							Square square = (Square)s;
							shapeInfo.append(String.format("Square: %d (time: %.0f seconds)", 
									square.getSide(), drawTimes.get(i)/1000.0));
						} else if (s.getType().equals("Rectangle")) {
							Rectangle rect = (Rectangle)s;
							shapeInfo.append(String.format("Rectangle: %d, X %d (time: %.0f seconds)", 
									rect.getLength(), rect.getWidth(), drawTimes.get(i)/1000.0));
						} else if (s.getType().equals("Triangle")) {
							Triangle tri = (Triangle)s;
							int[] sides = tri.getDimensions();
							double[] angles = tri.getAngles();
							shapeInfo.append(String.format("Triangle: %d, %d, %d (angles: %.2f, %.2f, %.2f; time: %.0f seconds)", 
									sides[0], sides[1], sides[2], 
									angles[0], angles[1], angles[2], 
									drawTimes.get(i)/1000.0));
						}

						writer.write(i+1 + ". " + shapeInfo.toString() + "\n");
					}
				} else {
					writer.write("No shapes were drawn in this session.\n");
				}

				// Statistics
				writer.write("\n=== Statistics ===\n");
				writer.write("Total Shapes: " + drawnShapes.size() + "\n");

				if (!drawnShapes.isEmpty()) {
					// Average drawing time
					double avgTimeSeconds = calculateAverageTime() / 1000.0;
					writer.write(String.format("Average Time: %.0f seconds\n", avgTimeSeconds));

					// Largest shape by area
					String largestShape = findLargestShape();
					writer.write("Largest Shape: " + largestShape + "\n");

					// Most frequently drawn shape
					int squareCount = countShapesByType("Square");
					int triangleCount = countShapesByType("Triangle");
					int rectangleCount = countShapesByType("Rectangle");

					String mostFrequentShape;
					if (squareCount >= triangleCount && squareCount >= rectangleCount) {
						mostFrequentShape = "Square: " + squareCount + " times";
					} else if (triangleCount >= squareCount && triangleCount >= rectangleCount) {
						mostFrequentShape = "Triangle: " + triangleCount + " times";
					} else {
						mostFrequentShape = "Rectangle: " + rectangleCount + " times";
					}
					writer.write("Most Frequently Drawn: " + mostFrequentShape + "\n");
				} else {
					writer.write("No shapes were drawn in this session.\n");
				}
			} catch(IOException e) {
				System.err.println("Failed to create log file: " + e.getMessage());
			}
		} catch(Exception e) {
			System.err.println("Error creating unique filename: " + e.getMessage());
			// Fallback to original filename if there's an error
			try(FileWriter writer = new FileWriter(baseFilename)) {
				// Write log data using the original filename
				writer.write("=== Drawing Session Log ===\n");
				// ... (same content as above)
			} catch(IOException ioEx) {
				System.err.println("Failed to create fallback log file: " + ioEx.getMessage());
			}
		}
	}

	// Calculate average drawing time
	private static long calculateAverageTime() {
		if (drawTimes.isEmpty()) {
			return 0;
		}
		return drawTimes.stream().mapToLong(Long::longValue).sum() / 
				drawTimes.size();
	}

	// Find the largest shape by area
	// I've implemented this to calculate the area of each shape type and return the largest one for the session summary
	private static String findLargestShape() {
		if (drawnShapes.isEmpty()) {
			return "No shapes drawn";
		}

		Shape largest = null;
		double maxArea = -1;

		for (Shape shape : drawnShapes) {
			double area = calculateArea(shape);
			if (area > maxArea) {
				maxArea = area;
				largest = shape;
			}
		}

		if (largest == null) {
			return "Unable to determine";
		}

		if (largest instanceof Square) {
			return String.format("Square: %d", ((Square)largest).getSide());
		} else if (largest instanceof Rectangle) {
			Rectangle rect = (Rectangle)largest;
			return String.format("Rectangle: %d×%d", rect.getLength(), rect.getWidth());
		} else if (largest instanceof Triangle) {
			int[] sides = largest.getDimensions();
			return String.format("Triangle: %d-%d-%d", sides[0], sides[1], sides[2]);
		}

		return largest.getType();
	}

	// Calculate area of a shape
	// I've used appropriate formulas for each shape type: square (side²), rectangle (length × width), and triangle (Heron's formula)
	private static double calculateArea(Shape shape) {
		if (shape instanceof Square) {
			int side = ((Square) shape).getSide();
			return side * side;
		} else if (shape instanceof Rectangle) {
			Rectangle rect = (Rectangle) shape;
			return rect.getLength() * rect.getWidth();
		} else if (shape instanceof Triangle) {
			// Use Heron's formula
			int[] sides = shape.getDimensions();
			double s = (sides[0] + sides[1] + sides[2]) / 2.0;
			return Math.sqrt(s * (s - sides[0]) * (s - sides[1]) * (s - sides[2]));
		}
		return 0;
	}

	// Get formatted dimension string for a shape
	private static String getShapeDimensionString(Shape shape) {
		if (shape instanceof Square) {
			return String.valueOf(((Square)shape).getSide());
		} else if (shape instanceof Rectangle) {
			Rectangle rect = (Rectangle)shape;
			return rect.getLength() + " x " + rect.getWidth();
		} else if (shape instanceof Triangle) {
			int[] sides = shape.getDimensions();
			return sides[0] + " - " + sides[1] + " - " + sides[2];
		}
		return "";
	}
}