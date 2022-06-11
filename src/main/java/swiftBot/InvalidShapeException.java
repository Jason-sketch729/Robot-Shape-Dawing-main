package swiftBot;

// Custom exception for handling invalid shape parameters
// This exception is thrown when shape dimensions or properties do not meet the required constraints
public class InvalidShapeException extends Exception {
	// Added serialVersionUID to prevent compiler warnings
	private static final long serialVersionUID = 1L;

	// Creates a new exception with the specified error message to provide clear feedback about why a shape is invalid
	public InvalidShapeException(String message) {
		super(message);
	}

	// Creates a new exception with the specified message and cause
	// I've added this to support exception chaining for complex validation scenarios
	public InvalidShapeException(String message, Throwable cause) {
		super(message, cause);
	}
}