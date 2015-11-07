package com.moodletrial;

/**
 * Defines an Exception class for throwing Exceptions pertaining to Moodle related issues
 * 
 * @author Karthik Meher
 *
 */
public class MoodleException extends Exception {
	
	private String message = null;
	
	public MoodleException() {
        super();
    }
 
    public MoodleException(String message) {
        super(message);
        this.message = message;
    }
 
    public MoodleException(Throwable cause) {
        super(cause);
    }
 
    @Override
    public String toString() {
        return message;
    }
 
    @Override
    public String getMessage() {
        return message;
    }
}
