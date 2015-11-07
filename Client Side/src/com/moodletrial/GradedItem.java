package com.moodletrial;
import java.util.Map;

/**
 * Interface implemented by all Activities which have a grade option.
 * As of now, only Assignment and Quiz implement this
 * 
 * @author Karthik Meher
 *
 */
public interface GradedItem {
	/**
	 * Returns key-value pairs of the student's email id and the grade obtained
	 * 
	 * @return Returns Map<String, Float>
	 */
	public Map<String, Float> getMarksMap();
	/**
	 * Sets the key-value pairs of the student's email id and the grade obtained for the particular item
	 * 
	 * @param mapList Map<String, Float> representing key-value pairs of the student's email id and the grade obtained
	 * 
	 */
	public void setMarksMap(Map<String, Float> mapList);	
}
