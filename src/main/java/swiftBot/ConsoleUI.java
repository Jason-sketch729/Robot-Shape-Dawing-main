package swiftBot;

// Utility class for creating enhanced console UI elements like coloured text, progress bars, and styled boxes
public class ConsoleUI {
	// ANSI colour codes
	public static final String RESET = "\u001B[0m";
	public static final String BLACK = "\u001B[30m";
	public static final String RED = "\u001B[31m";
	public static final String GREEN = "\u001B[32m";
	public static final String YELLOW = "\u001B[33m";
	public static final String BLUE = "\u001B[34m";
	public static final String PURPLE = "\u001B[35m";
	public static final String CYAN = "\u001B[36m";
	public static final String WHITE = "\u001B[37m";

	public static final String ORANGE = "\u001B[38;5;208m";          // Closest terminal colour to #FFA500
	public static final String BRIGHT_ORANGE = "\u001B[38;5;214m";   // Brighter orange
	public static final String BG_ORANGE = "\u001B[48;5;208m";       // Background orange

	// Background colours
	public static final String BG_BLACK = "\u001B[40m";
	public static final String BG_RED = "\u001B[41m";
	public static final String BG_GREEN = "\u001B[42m";
	public static final String BG_YELLOW = "\u001B[43m";
	public static final String BG_BLUE = "\u001B[44m";
	public static final String BG_PURPLE = "\u001B[45m";
	public static final String BG_CYAN = "\u001B[46m";
	public static final String BG_WHITE = "\u001B[47m";

	// Bright colours
	public static final String BRIGHT_RED = "\u001B[91m";
	public static final String BRIGHT_GREEN = "\u001B[92m";
	public static final String BRIGHT_YELLOW = "\u001B[93m";
	public static final String BRIGHT_BLUE = "\u001B[94m";
	public static final String BRIGHT_PURPLE = "\u001B[95m";
	public static final String BRIGHT_CYAN = "\u001B[96m";
	public static final String BRIGHT_WHITE = "\u001B[97m";

	// Bright backgrounds
	public static final String BG_BRIGHT_RED = "\u001B[101m";
	public static final String BG_BRIGHT_GREEN = "\u001B[102m";
	public static final String BG_BRIGHT_YELLOW = "\u001B[103m";
	public static final String BG_BRIGHT_BLUE = "\u001B[104m";
	public static final String BG_BRIGHT_PURPLE = "\u001B[105m";
	public static final String BG_BRIGHT_CYAN = "\u001B[106m";
	public static final String BG_BRIGHT_WHITE = "\u001B[107m";

	// Text styles
	public static final String BOLD = "\u001B[1m";
	public static final String UNDERLINE = "\u001B[4m";
	public static final String BLINK = "\u001B[5m";

	// ASCII characters for better compatibility across all terminals
	private static final String TOP_LEFT = "+";
	private static final String TOP_RIGHT = "+";
	private static final String BOTTOM_LEFT = "+";
	private static final String BOTTOM_RIGHT = "+";
	private static final String HORIZONTAL = "-";
	private static final String VERTICAL = "|";
	private static final String BLOCK = "#";

	// Unicode characters (only use if you're sure your terminal supports them)
	private static final String UNI_TOP_LEFT = "┌";
	private static final String UNI_TOP_RIGHT = "┐";
	private static final String UNI_BOTTOM_LEFT = "└";
	private static final String UNI_BOTTOM_RIGHT = "┘";
	private static final String UNI_HORIZONTAL = "─";
	private static final String UNI_VERTICAL = "│";
	private static final String UNI_BLOCK = "█";

	private static final int CONSOLE_WIDTH = 90;
	private static final boolean USE_UNICODE = false; // Set to false for maximum compatibility


	// Clears the console (best effort)
	public static void clearConsole() {
		try {
			final String os = System.getProperty("os.name");
			if (os.contains("Windows")) {
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
			} else {
				// ANSI escape code to clear screen
				System.out.print("\033[H\033[2J");
				System.out.flush();
			}
		} catch (Exception e) {
			// Fallback if clearing fails: print blank lines
			for (int i = 0; i < 50; i++) {
				System.out.println();
			}
		}
	}

	// Draws a header with the specified text
	public static void drawHeader(String text) {
		drawColorfulBorder();
		System.out.println(centered(text));
		drawColorfulBorder();
	}

	// Draws a QR scanning header with white background and black text
	public static void drawQRScanningHeader() {
		drawColorfulBorder();
		System.out.println(centered(BG_BRIGHT_WHITE + BLACK + BOLD + " QR CODE SCANNING " + RESET));
		drawColorfulBorder();
	}

	// Draws a simple horizontal line
	public static void drawLine() {
		System.out.println(CYAN + HORIZONTAL.repeat(60) + RESET);
	}

	// Draws a rainbow horizontal line
	public static void drawRainbowLine() {
		// Replace with a single colour (for example, cyan)
		System.out.println(CYAN + "-".repeat(60) + RESET);
	}

	// Draws a colourful border using alternating colours
	public static void drawColorfulBorder() {
		// Replace with a single colour border (for example, cyan)
		StringBuilder border = new StringBuilder();
		border.append(CYAN);

		for (int i = 0; i < CONSOLE_WIDTH; i++) {
			border.append(HORIZONTAL);
		}

		border.append(RESET);
		System.out.println(border.toString());
	}

	// Draws a progress bar
	public static void drawProgressBar(int progress, int width) {
		int completed = (int)((width * progress) / 100.0);
		StringBuilder bar = new StringBuilder();

		// Draw the progress bar with colour gradients
		for (int i = 0; i < completed; i++) {
			// Gradient from green to yellow to red
			if (i < width * 0.33) {
				bar.append(GREEN);
			} else if (i < width * 0.66) {
				bar.append(YELLOW);
			} else {
				bar.append(RED);
			}

			bar.append(USE_UNICODE ? UNI_BLOCK : BLOCK);
		}

		bar.append(RESET);
		for (int i = completed; i < width; i++) {
			bar.append(" ");
		}

		System.out.println(" " + bar.toString() + " " + BRIGHT_WHITE + progress + "%" + RESET);
	}

	// Draws a fancy progress bar with borders and coloured fill
	public static void drawFancyProgressBar(int progress, String label) {
		int barWidth = 40;
		int completed = (int)((barWidth * progress) / 100.0);

		// Top border
		System.out.println(CYAN + "+" + "-".repeat(barWidth + 4) + "+" + RESET);

		// Label row
		System.out.println(CYAN + "| " + 
				BRIGHT_CYAN + label + 
				" ".repeat(Math.max(0, barWidth + 2 - label.length())) + 
				CYAN + "|" + RESET);

		// Progress bar row
		System.out.print(CYAN + "| " + RESET);

		// First print the coloured filled portion
		for (int i = 0; i < completed; i++) {
			if (i < barWidth * 0.2) {
				System.out.print(BRIGHT_BLUE + "|");
			} else if (i < barWidth * 0.4) {
				System.out.print(BRIGHT_CYAN + "|");
			} else if (i < barWidth * 0.6) {
				System.out.print(BRIGHT_GREEN + "|");
			} else if (i < barWidth * 0.8) {
				System.out.print(BRIGHT_YELLOW + "|");
			} else {
				System.out.print(BRIGHT_RED + "|");
			}
		}

		// Add a ">" at the current progress point if not complete
		if (completed < barWidth) {
			System.out.print(BRIGHT_WHITE + ">");
		}

		// Then print the empty portion with dashes
		// FIXED: Use spaces instead of referring to BRIGHT_BLACK to avoid null
		for (int i = completed + (completed < barWidth ? 1 : 0); i < barWidth; i++) {
			System.out.print("-");
		}

		// End the line with percentage
		System.out.println(" " + BRIGHT_WHITE + progress + "%" + RESET + " " + CYAN + "|" + RESET);

		// Bottom border
		System.out.println(CYAN + "+" + "-".repeat(barWidth + 4) + "+" + RESET);
	}

	// Centred the text in the console
	public static String centered(String text) {
		// Remove ANSI codes for length calculation
		String plainText = text.replaceAll("\u001B\\[[;\\d]*m", "");
		int padding = Math.max(0, (CONSOLE_WIDTH - plainText.length()) / 2);
		return " ".repeat(padding) + text;
	}

	// Draws a box with a message
	public static void drawBox(String message, String color) {
		String[] lines = message.split("\n");
		int maxLength = 0;

		for (String line : lines) {
			maxLength = Math.max(maxLength, line.length());
		}

		String topLeft = USE_UNICODE ? UNI_TOP_LEFT : TOP_LEFT;
		String topRight = USE_UNICODE ? UNI_TOP_RIGHT : TOP_RIGHT;
		String bottomLeft = USE_UNICODE ? UNI_BOTTOM_LEFT : BOTTOM_LEFT;
		String bottomRight = USE_UNICODE ? UNI_BOTTOM_RIGHT : BOTTOM_RIGHT;
		String horizontal = USE_UNICODE ? UNI_HORIZONTAL : HORIZONTAL;
		String vertical = USE_UNICODE ? UNI_VERTICAL : VERTICAL;

		// Top border with rounded corners
		System.out.print(color + topLeft);
		System.out.print(horizontal.repeat(maxLength + 2));
		System.out.println(topRight + RESET);

		// Content
		for (String line : lines) {
			System.out.print(color + vertical + " ");
			System.out.print(RESET + BRIGHT_WHITE + line);
			System.out.print(" ".repeat(maxLength - line.length()));
			System.out.println(" " + color + vertical + RESET);
		}

		// Bottom border with rounded corners
		System.out.print(color + bottomLeft);
		System.out.print(horizontal.repeat(maxLength + 2));
		System.out.println(bottomRight + RESET);
	}

	// Draws an error box with a colourful border
	public static void drawErrorBox(String message) {
		drawHeader(BG_RED + WHITE + " !!! ERROR !!! " + RESET);

		// Just show the message once with a red border
		drawBox(message, RED);
	}


	// Draws a success message with animation
	public static void drawSuccess(String message) {
		drawHeader(BG_GREEN + BLACK + " " + message + " " + RESET);
	}

	// Displays completion message with animation
	public static void drawCompletionMessage(String shapeType, int drawTime, boolean hasMoreShapes) {
		String color;
		String symbol;

		if (shapeType.equals("Square")) {
			color = ORANGE; // Changed from GREEN to ORANGE to match your colour scheme
			symbol = "[]";
		} else if (shapeType.equals("Rectangle")) {
			color = PURPLE;
			symbol = "[ ]";
		} else if (shapeType.equals("Triangle")) {
			color = BLUE;
			symbol = "/ \\";
		} else {
			color = YELLOW;
			symbol = "*";
		}

		// Remove animation loop - just show the message once
		clearConsole();
		drawHeader(BG_GREEN + BLACK + " " + shapeType.toUpperCase() + " COMPLETED ! " + RESET);

		// Fixed-width box with better alignment
		int boxWidth = 30; // Fixed box width

		// Top border
		String borderLine = color + "+" + "-".repeat(boxWidth - 2) + "+" + RESET;
		System.out.println("\n" + centered(borderLine));

		// First line - Shape drawn
		String shapeText = symbol + " " + shapeType + " DRAWN!";
		String line1 = color + "|" + RESET + BG_BRIGHT_WHITE + BLACK + " " + shapeText +
				" ".repeat(boxWidth - 4 - shapeText.length()) + RESET + color + "|" + RESET;
		System.out.println(centered(line1));

		// Second line - Time
		String timeText = "Time: " + drawTime + "s";
		String line2 = color + "|" + RESET + BG_BRIGHT_WHITE + BLACK + " " + timeText +
				" ".repeat(boxWidth - 4 - timeText.length()) + RESET + color + "|" + RESET;
		System.out.println(centered(line2));

		// Third line - SwiftBot status
		String statusText = "SwiftBot: Blinking " + GREEN + "... " + RESET;
		// Calculate visual length excluding ANSI colour codes
		int statusVisualLength = "SwiftBot: Blinking ... ".length();
		String line3 = color + "|" + RESET + BG_BRIGHT_WHITE + BLACK + " " + statusText +
				" ".repeat(boxWidth - 4 - statusVisualLength) + RESET + color + "|" + RESET;
		System.out.println(centered(line3));

		// Bottom border
		System.out.println(centered(borderLine) + "\n");

		// Only show repositioning message if there are more shapes
		if (hasMoreShapes) {
			System.out.println(YELLOW + "Repositioning 15 cm for next shape..." + RESET);
		}
	}


	// Draws shape preview with enhanced visuals
	public static void drawShapePreview(String shapeType, int[] dimensions) {
		String title = "SHAPE: " + shapeType + " (";
		String color;
		String bgColor;

		if (shapeType.equalsIgnoreCase("square")) {
			title += "S-" + dimensions[0];
			color = ORANGE;
			bgColor = BG_ORANGE;
		} else if (shapeType.equalsIgnoreCase("rectangle")) {
			title += "R-" + dimensions[0] + "-" + dimensions[1];
			color = PURPLE;
			bgColor = BG_PURPLE;
		} else if (shapeType.equalsIgnoreCase("triangle")) {
			title += "T-" + dimensions[0] + "-" + dimensions[1] + "-" + dimensions[2];
			color = BLUE;
			bgColor = BG_BLUE;
		} else {
			title += "Unknown";
			color = YELLOW;
			bgColor = BG_YELLOW;
		}

		title += ")";

		// Draw a more colourful header for the shape
		System.out.println();
		System.out.println(centered(BOLD + color + title + RESET));
		System.out.println();

		if (shapeType.equalsIgnoreCase("square")) {
			drawEnhancedSquarePreview(dimensions[0], color, bgColor);
		} else if (shapeType.equalsIgnoreCase("rectangle")) {
			drawEnhancedRectanglePreview(dimensions[0], dimensions[1], color, bgColor);
		} else if (shapeType.equalsIgnoreCase("triangle")) {
			drawEnhancedTrianglePreview(dimensions, color, bgColor);
		}
	}

	private static void drawEnhancedSquarePreview(int side, String color, String bgColor) {
		// Draw a square with colourful border and filled centre
		int width = Math.min(30, side / 2);

		System.out.println();

		// Top border
		System.out.print(centered(color + "+" + HORIZONTAL.repeat(width) + "+" + RESET));
		System.out.println();

		// Middle with gradient fill
		for (int i = 0; i < width/2; i++) {
			if (i == width/4) {
				// Show dimension in middle of shape
				String dimText = "SIDE: " + side + " cm";
				int padding = (width - dimText.length()) / 2;
				System.out.print(centered(
						color + VERTICAL + RESET + 
						" ".repeat(Math.max(0, padding)) + 
						BRIGHT_WHITE + BOLD + dimText + RESET + 
						" ".repeat(Math.max(0, width - padding - dimText.length())) + 
						color + VERTICAL + RESET
						));
			} else {
				// Colourful fill patterns
				System.out.print(centered(
						color + VERTICAL + RESET + 
						bgColor + BLACK + " ".repeat(width) + RESET + 
						color + VERTICAL + RESET
						));
			}
			System.out.println();
		}

		// Bottom border
		System.out.print(centered(color + "+" + HORIZONTAL.repeat(width) + "+" + RESET));
		System.out.println("\n");
	}

	private static void drawEnhancedRectanglePreview(int length, int width, String color, String bgColor) {
		// Draw a rectangle with colourful patterns
		int displayWidth = Math.min(35, length / 2);
		int displayHeight = Math.min(5, width / 2);

		System.out.println();

		// Top border
		System.out.print(centered(color + "+" + HORIZONTAL.repeat(displayWidth) + "+" + RESET));
		System.out.println();

		// Middle with fancy pattern
		for (int i = 0; i < displayHeight; i++) {
			if (i == displayHeight/2) {
				// Show dimension in middle of shape
				String dimText = length + "X" + width + " cm";
				int padding = (displayWidth - dimText.length()) / 2;
				System.out.print(centered(
						color + VERTICAL + RESET + 
						" ".repeat(Math.max(0, padding)) + 
						BRIGHT_WHITE + BOLD + dimText + RESET + 
						" ".repeat(Math.max(0, displayWidth - padding - dimText.length())) + 
						color + VERTICAL + RESET
						));
			} else {
				// Create a checkerboard pattern for visual interest
				StringBuilder pattern = new StringBuilder();
				for (int j = 0; j < displayWidth; j++) {
					if ((i + j) % 2 == 0) {
						pattern.append(bgColor + " " + RESET);
					} else {
						pattern.append(BG_BRIGHT_PURPLE + " " + RESET);
					}
				}
				System.out.print(centered(color + VERTICAL + RESET + pattern + color + VERTICAL + RESET));
			}
			System.out.println();
		}

		// Bottom border
		System.out.print(centered(color + "+" + HORIZONTAL.repeat(displayWidth) + "+" + RESET));
		System.out.println("\n");
	}

	private static void drawEnhancedTrianglePreview(int[] dimensions, String color, String bgColor) {
		// Make a more colourful triangle
		int height = 12;

		System.out.println();

		// Gradient-colored triangle
		for (int i = 0; i < height; i++) {
			int spaces = (height - i - 1);
			int chars = i * 2 + 1;

			StringBuilder line = new StringBuilder();
			line.append(" ".repeat(spaces));

			// Choose colour based on position (creating a rainbow effect)
			String currentColor;
			if (i < height/3) {
				currentColor = BRIGHT_BLUE;
			} else if (i < 2*height/3) {
				currentColor = BRIGHT_CYAN;
			} else {
				currentColor = BLUE;
			}

			if (i == height/2) {
				// Show dimension in middle of shape
				String dimText = dimensions[0] + "-" + dimensions[1] + "-" + dimensions[2];
				if (dimText.length() <= chars) {
					int remaining = chars - dimText.length();
					int leftPad = remaining / 2;
					line.append(currentColor)
					.append("/".repeat(leftPad))
					.append(BRIGHT_WHITE)
					.append(dimText)
					.append(currentColor)
					.append("\\".repeat(remaining - leftPad))
					.append(RESET);
				} else {
					line.append(currentColor)
					.append("/\\".repeat(chars/2))
					.append(chars % 2 == 1 ? "/" : "")
					.append(RESET);
				}
			} else if (i == height - 1) {
				// Bottom line
				line.append(BRIGHT_BLUE + " " + HORIZONTAL.repeat(chars-2) + " " + RESET);
			} else {
				// Fill with interesting pattern
				line.append(currentColor);
				for (int j = 0; j < chars; j++) {
					if (i == 0) {
						line.append(" ");
					} else {
						line.append(j % 2 == 0 ? "/" : "\\");
					}
				}
				line.append(RESET);
			}

			System.out.println(centered(line.toString()));
		}

		System.out.println();
	}

	// Displays completion message with animation
	public static void drawTerminationSummary(String logFile, int squares, int triangles, 
			int rectangles, double averageTime) {
		clearConsole();
		drawRainbowLine();
		drawHeader(BG_RED + WHITE + BOLD + " PROGRAM TERMINATED " + RESET);

		System.out.println("Log file saved to:");
		System.out.println(GREEN + logFile + RESET + "\n");

		// Box dimensions
		int boxWidth = 50; // Keep the same width

		// Create centered title with proper spacing
		String title = " SESSION SUMMARY ";
		int paddingEachSide = (boxWidth - 2 - title.length()) / 2;
		// Handle odd/even box width by adding extra space to the right if needed
		int extraPad = (boxWidth - 2 - title.length()) % 2;

		String titleBar = "|" + BG_BRIGHT_CYAN + BLACK + 
				" ".repeat(paddingEachSide) + 
				title + 
				" ".repeat(paddingEachSide + extraPad) + 
				RESET + BRIGHT_CYAN + "|";

		// Create all other lines same as before
		String topLine = "+" + "-".repeat(boxWidth - 2) + "+";
		String dividerLine = "|" + "-".repeat(boxWidth - 2) + "|";

		// Format item lines as before
		String squaresLine = createAlignedLine(boxWidth, ORANGE + "[]" + RESET, "Squares:", squares);
		String trianglesLine = createAlignedLine(boxWidth, BLUE + "/\\" + RESET, "Triangles:", triangles);
		String rectanglesLine = createAlignedLine(boxWidth, PURPLE + "[ ]" + RESET, "Rectangles:", rectangles);

		// Format time line
		String timeStr = String.format("%.1fs", averageTime);
		String timeLine = createAlignedTimeLine(boxWidth, YELLOW + "->" + RESET, "Avg. time/shape:", timeStr);

		// Draw the box
		System.out.println(centered(BRIGHT_CYAN + topLine + RESET));
		System.out.println(centered(BRIGHT_CYAN + titleBar + RESET));
		System.out.println(centered(BRIGHT_CYAN + dividerLine + RESET));
		System.out.println(centered(BRIGHT_CYAN + squaresLine + RESET));
		System.out.println(centered(BRIGHT_CYAN + trianglesLine + RESET));
		System.out.println(centered(BRIGHT_CYAN + " " + rectanglesLine + RESET));
		System.out.println(centered(BRIGHT_CYAN + dividerLine + RESET));
		System.out.println(centered(BRIGHT_CYAN + timeLine + RESET));
		System.out.println(centered(BRIGHT_CYAN + topLine + RESET) + "\n");

		// thank you message
		System.out.print(RED + "Thank You for using Shape Drawing Program on SwiftBot!" + RESET);
		System.out.println();
		//Draw colourful border line below
		drawColorfulBorder();
	}

	// Creates a perfectly aligned line for items in the summary box
	private static String createAlignedLine(int boxWidth, String symbol, String label, int value) {
		// Calculate the visual length (accounting for ANSI colour codes)
		int symbolVisualLength = 2; // Most symbols like [], /\ visually take 2 spaces
		int valueStrLength = String.valueOf(value).length();

		// Create base string with symbol and label
		String baseText = "| " + symbol + " " + label + " " + value;

		// Calculate how many spaces to add for alignment
		// Total spaces = box width - (visible characters in base text + right margin)
		int visibleLength = 3 + symbolVisualLength + label.length() + 1 + valueStrLength; // 3="| " and " "
		int spacesToAdd = boxWidth - 2 - visibleLength; // -2 for the ending " |"

		// Construct the final string with exact padding
		return baseText + " ".repeat(spacesToAdd) + " |";
	}

	// Creates a perfectly aligned line for the time display
	private static String createAlignedTimeLine(int boxWidth, String symbol, String label, String value) {
		// Calculate the visual length (accounting for ANSI colour codes)
		int symbolVisualLength = 1; // ">" is just 1 character

		// Create the start of the line
		String lineStart = "| " + symbol + " " + label;

		// Calculate exact spaces needed for perfect right-alignment of the value
		int visibleStartLength = 3 + symbolVisualLength + label.length(); // 3="| " and " "
		int spacesToAdd = boxWidth - 2 - visibleStartLength - value.length() - 1; // -2 for "| " and -1 for ending "|"

		// Construct the final string with exact padding
		return lineStart + " ".repeat(spacesToAdd) + value + " |";
	}

	// Draw welcome screen with colourful branding
	public static void drawWelcomeScreen() {
		clearConsole();

		// Draw colourful border line above (same as program terminated screen)
		drawColorfulBorder();


		// Title with colourful background
		System.out.println(centered(BG_GREEN + WHITE + BOLD + " Shape Drawing Program " + RESET));

		// Draw colourful border line above (same as program terminated screen)
		drawColorfulBorder();

		// SwiftBot ASCII art (colourful representation)
		System.out.println(centered(BLUE + "   _____   " + RESET));
		System.out.println(centered(BLUE + " /|_|_|\\ " + RESET));
		System.out.println(centered(BLACK + "[" + RED + "/" + RESET + BLACK + "]" + CYAN + "[CAM]" + BLACK + "[" + RED + "\\" + RESET + BLACK + "]" + RESET));
		System.out.println(centered(PURPLE + "|=======|" + BLACK + RESET));
		System.out.println(centered(BLUE + "/" + BLACK + "=" + PURPLE + "[" + GREEN + "SwiftBot" + PURPLE + "]" + BLACK + "=" + BLUE + "\\" + RESET));
		System.out.println(centered(CYAN + "//" + BLACK + "=" + YELLOW + "[=====]" + BLACK + "=" + CYAN + "\\\\" + RESET));
		System.out.println(centered(BLUE + "(O)" + BLACK + "----" + BRIGHT_RED + "|||" + BLACK + "----" + BLUE + "(O)" + RESET));
		System.out.println();

		// Instructions with colourful highlights in a box
		System.out.println(BRIGHT_CYAN + "=" + "=".repeat(CONSOLE_WIDTH) + "=" + RESET);

		System.out.println(BRIGHT_CYAN + "|" + RESET + " " + BG_BRIGHT_YELLOW + BLACK + " INSTRUCTIONS: " + RESET + " ".repeat(45) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + " ".repeat(58) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + " 1. Scan a QR code with shapes in format:" + " ".repeat(21) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + "    " + BG_ORANGE + BLACK + " S-20 " + RESET + " (Square 20cm)" + " ".repeat(34) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + "    " + BG_BLUE + BLACK + " T-15-20-25 " + RESET + " (Triangle with sides 15,20,25cm)" + " ".repeat(11) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + "    " + BG_PURPLE + BLACK + " R-10-30 " + RESET + " (Rectangle 10×30cm)" + " ".repeat(27) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + " ".repeat(58) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + " 2. Chain multiple shapes with & character:" + " ".repeat(19) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + "    Example: " + BG_ORANGE + BLACK + " S-20 " + RESET + "&" + BG_BLUE + BLACK + " T-15-20-25 " + RESET + "&" + BG_PURPLE + BLACK + " R-10-30 " + RESET + " ".repeat(10) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + " ".repeat(58) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + " 3. Controls:" + " ".repeat(46) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + "    [" + BG_GREEN + BLACK + " P " + RESET + "] Pause drawing  [" + BG_GREEN + BLACK + " R " + RESET + "] Resume  [" + BG_RED + WHITE + " X " + RESET + "] Exit" + " ".repeat(9) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + " ".repeat(58) + BRIGHT_CYAN + RESET);

		// Scan prompt with animated effect
		System.out.println(BRIGHT_CYAN + "|" + RESET + " ".repeat(58) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + " " + BRIGHT_YELLOW + "-> Place a QR code in front of SwifBot's camera to begin ... " + RESET + " ".repeat(15) + BRIGHT_CYAN + RESET);
		System.out.println(BRIGHT_CYAN + "|" + RESET + " ".repeat(58) + BRIGHT_CYAN + RESET);

		// Bottom border
		System.out.println(BRIGHT_CYAN + "=" + "=".repeat(CONSOLE_WIDTH) + "=" + RESET);
		System.out.println();

		// Call to action
		System.out.println(centered(BG_BRIGHT_WHITE + BLACK + " Press [ENTER] to start scanning or [X] to exit " + RESET));

	}

	// Draw a fancy status screen during shape drawing
	public static void drawShapeDrawingStatus(String shapeType, String dimension, 
			int elapsed, int remaining, int progress) {
		String color = getColorForShape(shapeType);
		String bgColor = getBgColorForShape(shapeType);

		clearConsole();
		drawColorfulBorder();
		System.out.println(centered(bgColor + BLACK + " DRAWING [" + shapeType + "] - " + dimension + "" + RESET));
		drawColorfulBorder();

		// Progress bar with gradient and animation
		System.out.println();
		int barWidth = 40;
		int completed = (int)((barWidth * progress) / 100.0);
		System.out.print(" ");

		// First draw the filled portion with colour gradient
		for (int i = 0; i < completed; i++) {
			double ratio = (double)i / barWidth;
			if (ratio < 0.2) {
				System.out.print(BRIGHT_BLUE + "||");
			} else if (ratio < 0.4) {
				System.out.print(BRIGHT_CYAN + "||");
			} else if (ratio < 0.6) {
				System.out.print(BRIGHT_GREEN + "||");
			} else if (ratio < 0.8) {
				System.out.print(BRIGHT_YELLOW + "||");
			} else {
				System.out.print(BRIGHT_RED + "||");
			}
		}

		// Then the "active" part with pattern
		int activeWidth = Math.min(5, barWidth - completed);
		if (activeWidth > 0) {
			System.out.print(BRIGHT_WHITE + "||".repeat(activeWidth));
		}

		// Finally the empty part
		System.out.print(RESET + " ".repeat(Math.max(0, barWidth - completed - activeWidth)));

		System.out.println(" " + BRIGHT_WHITE + progress + "%" + RESET);
		System.out.println();


		// Robot status with more visual elements
		System.out.println(BG_BRIGHT_WHITE + BLACK + " SwiftBot Status: " + RESET);
		System.out.println(GREEN + "-> " + RESET + "SwiftBot Moving (" + BRIGHT_WHITE + "40%" + RESET + " speed)");
		System.out.println(color + "-> " + RESET + "Lights: Solid " + 
				color + getColorName(shapeType) + RESET);

		// Option 2: Robot mood indicator
		System.out.println(BRIGHT_YELLOW + "-> " + RESET + "Robot mood: " + 
				(progress < 30 ? "Started ..." : (progress < 70 ? "Mid Way ..." : "Finishing Off ...")));

	}

	// Get appropriate background colour code for a shape type
	private static String getBgColorForShape(String shapeType) {
		if (shapeType.equals("Square")) {
			return BG_ORANGE; // Changed from BG_GREEN to BG_ORANGE
		} else if (shapeType.equals("Rectangle")) {
			return BG_PURPLE;
		} else if (shapeType.equals("Triangle")) {
			return BG_BLUE;
		}
		return BG_BRIGHT_WHITE;
	}

	// Get appropriate colour code for a shape type
	private static String getColorForShape(String shapeType) {
		if (shapeType.equals("Square")) {
			return ORANGE; // Changed from GREEN to ORANGE
		} else if (shapeType.equals("Rectangle")) {
			return PURPLE;
		} else if (shapeType.equals("Triangle")) {
			return BLUE;
		}
		return BRIGHT_WHITE;
	}

	// Get colour name based on shape type
	public static String getColorName(String shapeType) {
		if (shapeType.equals("Square")) {
			return "orange"; // Changed from "green" to "orange"
		} else if (shapeType.equals("Rectangle")) {
			return "purple";
		} else if (shapeType.equals("Triangle")) {
			return "blue";
		}
		return "white";
	}

	// Draw a pause screen with enhanced visuals
	public static void drawEnhancedPauseScreen(String shapeType, String dimension, int progress) {
		String color = getColorForShape(shapeType);

		clearConsole();
		drawColorfulBorder();
		System.out.println(centered(BG_BRIGHT_YELLOW + BLACK + BOLD + " PAUSED " + RESET));
		drawColorfulBorder();

		// Progress bar with suspended animation
		System.out.println();
		int barWidth = 40;
		int completed = (int)((barWidth * progress) / 100.0);
		System.out.print(" ");

		// Draw the filled portion 
		for (int i = 0; i < completed; i++) {
			// Use the same bar style as in the normal progress screen
			System.out.print(color + "||");
		}

		// Draw the rest empty
		System.out.print(RESET + " ".repeat(Math.max(0, (barWidth - completed) * 2)));

		System.out.println(" " + BRIGHT_WHITE + progress + "%" + RESET);

		// Current shape info in a box
		String[] lines = {
				" Current Task: " + color + shapeType + RESET + " (" + dimension + " cm)",
				" Status: " + YELLOW + "PAUSED" + RESET
		};

		int maxWidth = 40;
		System.out.println();
		System.out.println(BRIGHT_WHITE + "+" + "-".repeat(maxWidth + 2) + "+" + RESET);
		for (String line : lines) {
			System.out.println(BRIGHT_WHITE + "|" + RESET + " " + line + 
					" ".repeat(Math.max(0, maxWidth - line.length() + 1)) + 
					BRIGHT_WHITE + "|" + RESET);
		}
		System.out.println(BRIGHT_WHITE + "+" + "-".repeat(maxWidth + 2) + "+" + RESET);

		System.out.println();
		System.out.println("Press [" + BG_GREEN + BLACK + " R " + RESET + "] to resume or [" + BG_RED + WHITE + " X " + RESET + "] to exit.");
		drawColorfulBorder();
	}

	// Draws a scanning animation to indicate activity
	public static void drawScanningAnimation(int frame) {
		String[] frames = {
				"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"
		};

		String[] colors = {
				BRIGHT_BLUE, BRIGHT_CYAN, BRIGHT_GREEN, BRIGHT_YELLOW, BRIGHT_RED,
				BRIGHT_PURPLE, BRIGHT_BLUE, BRIGHT_CYAN, BRIGHT_GREEN, BRIGHT_YELLOW
		};

		System.out.print("\r" + colors[frame % colors.length] + 
				frames[frame % frames.length] + " Scanning QR code..." + RESET + "     ");
	}
}