package com.moodletrial;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

enum ActivityType
{
	CHOICE,
	RESOURCE,
	ASSIGNMENT,
	SURVEY,
	QUIZ,
	UNREGISTERED//Any other activity apart from these will appear as 'Unregistered' --> More activities can be added here
}

/**
 * Class containing all the basic properties needed in an Activity.
 *  
 * @author Karthik Meher
 */

//NOTE: Once all required modules have been finalized, it is recommended to convert this to an abstract class and have all modules extend this instead
class Activity {
	protected int id;
	protected URL urlLocation;
	protected String name, description;
	protected ActivityType type;
	protected URL fileURL = null;
	
	public Activity(int id, URL loc, String name, String desc, ActivityType type, URL fileURL)
	{
		this.id = id;
		this.urlLocation = loc;
		this.name = name;
		this.description = desc;
		this.type = type;
		this.fileURL = fileURL;
	}
	
	@Override
	public String toString()
	{
		StringBuffer response = new StringBuffer();
		
		response.append(this.name + "\t" + "ID: " + this.id +"\t" + "Type:" + this.type);
		response.append("\n" + this.description);
		if(fileURL == null)
			response.append(this.urlLocation);
		else
			response.append(this.fileURL);
		
		return response.toString();
	}
	
	public String getFullname()
	{
		return name;
	}
	
	public int getId()
	{
		return this.id;
	}
	
	public static Activity getActivityByName(Activity[] courses, String name)
	{
		for(Activity course : courses)
		{
			if(course.getFullname().equalsIgnoreCase(name))
			{
				return course;
			}						
		}
		
		return null;
	}
	
	public static Activity getActivityById(Activity[] activities, int id)
	{
		for(Activity a : activities)
		{
			if(a.getId() == id)
			{
				return a;
			}
		}
		
		return null;
	}
	
	public ActivityType getActivityType()
	{
		return this.type;
	}
}

/**
 * Class extending {@link Activity} and adding additional properties specific to assignments
 *  
 * @author Karthik Meher
 *
 */
class Assignment extends Activity implements GradedItem
{
	private Map<String, Float> marksList = new HashMap<String, Float>();
	private int assignmentId=0, submissionType=0;//For submission type, 0->unset, 1->file type submission, 2->online text submission 3->both	
	
	public int getAssignmentId() {
		return assignmentId;
	}

	public void setAssignmentId(int assignmentId) {
		this.assignmentId = assignmentId;
	}

	public int getSubmissionType() {
		return submissionType;
	}

	public void setSubmissionType(int submissionType) {
		this.submissionType = submissionType;
	}

	public Assignment(int id, URL loc, String name, String desc, ActivityType type, URL fileURL) 
	{
		super(id, loc, name, desc, type, fileURL);		
	}

	@Override
	public Map<String, Float> getMarksMap() {
		return marksList;
	}

	@Override
	public void setMarksMap(Map<String, Float> mapList) {
		this.marksList = mapList;		
	}
}

/**
 * Class extending {@link Activity} and adding properties specific to quizzes
 * @author Karthik Meher
 *
 */
class Quiz extends Activity implements GradedItem
{
	private Map<String, Float> marksList = new HashMap<String, Float>();
	
	public Quiz(int id, URL loc, String name, String desc, ActivityType type, URL fileURL) {
		super(id, loc, name, desc, type, fileURL);
	}

	@Override
	public Map<String, Float> getMarksMap() {
		return marksList;
	}

	@Override
	public void setMarksMap(Map<String, Float> mapList) {
		this.marksList = mapList;
		
	}	
}