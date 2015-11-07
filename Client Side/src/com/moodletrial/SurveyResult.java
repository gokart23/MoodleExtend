package com.moodletrial;
import java.util.*;

/**
 * Class for holding the data associated with a Survey. All data is stored in the form of records
 * 
 * @author Karthik Meher
 *
 */
public class SurveyResult {
	/**
	 * The header record, containing the column names
	 */
	private ArrayList<String> header = null;
	/**
	 * List of records, each of which is itself a List of Strings
	 */
	private ArrayList<ArrayList<String>> records;
	private int numRecords, numFields, surveyId;
	
	public SurveyResult(int surveyId, ArrayList<String> header, ArrayList<ArrayList<String>> records)
	{
		this.surveyId = surveyId;
		this.header = header;
		this.records = records;
		numFields = header.size();
		numRecords = records.size();
	}
	
	@Override
	public String toString()
	{
		StringBuffer response = new StringBuffer();
		
		for(String s : header)
		{
			response.append(s + "\t");
		}
		
		for(ArrayList<String> r : records)
		{
			response.append("\n");
			
			for(String s : r)
			{
				response.append(s + "\t");
			}
		}
		
		return response.toString();
	}
	
	public ArrayList<String> getHeader()
	{
		return header;
	}
	
	public ArrayList<String> getRecord(int index)
	{
		return records.get(index);
	}
	
	public ArrayList<ArrayList<String>> getAllRecords()
	{
		return records;
	}
	
	public int getNumFields()
	{
		return numFields;
	}
	
	public int getNumRecords()
	{
		return numRecords;
	}
	
	public int getSurveyId()
	{
		return surveyId;
	}	
}
