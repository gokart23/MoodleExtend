package com.moodletrial;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class representing the basic properties and methods required in a Course
 * 
 * @author Karthik Meher
 *
 */
class Course{
	private int courseId, categoryId;
	private String summary, summaryFormat, courseFormat, shortName, fullName;
	private Activity[] courseActivities;
	private Student[] enrolledStudents;
	private ArrayList<Integer> gradedIndices;
	private ArrayList<Integer> resourceIndices;
	private String gradeKey = null;
	
	//Note: Preferable to use an enum to represent the different formats?(site/weeks/topics/social, etc.)
	public String getCourseFormat() {
		return courseFormat;
	}
	
	public Course(int courseId, int categoryId, String summary, String summaryFormat, String courseFormat, String shortName, String fullName)
	{
		this.courseId = courseId;
		this.categoryId = categoryId;
		this.summary = summary;
		this.summaryFormat = summaryFormat;
		this.courseFormat = courseFormat;
		this.shortName = shortName;
		this.fullName = fullName;
		
		gradedIndices = new ArrayList<Integer>();
		resourceIndices = new ArrayList<Integer>();
	}
	
	@Override
	public String toString()
	{
		StringBuffer course = new StringBuffer();
		
		course.append(this.fullName + " (" + this.shortName + ")");
		course.append("\n" + "Course ID:" + this.courseId + "\t" + "Category ID:" + this.categoryId + "\tCourse Format: " + this.courseFormat);
		course.append("\n" + this.summary);
		
		return course.toString();
	}
	
	public void setGradeKey(String key)
	{
		this.gradeKey = key;
	}
	
	public String getGradeKey()
	{
		return gradeKey;
	}
	
	public int getCourseId()
	{
		return courseId;
	}

	public Activity[] getCourseActivities() {
		return courseActivities;
	}

	public void setCourseActivities(Activity[] courseActivities) {
		this.courseActivities = courseActivities;
		gradedIndices.clear();
		resourceIndices.clear();
		
		for(int i = 0; i < courseActivities.length; i++)
		{
			if(courseActivities[i] instanceof GradedItem)
			{
				gradedIndices.add(i);
			}
			else if(courseActivities[i].getActivityType() == ActivityType.RESOURCE)
			{
				resourceIndices.add(i);
			}
		}
	}
	
	public Activity[] getResourceList()
	{
		Activity[] a = new Activity[resourceIndices.size()];
				
		for(int i=0;i < a.length;i++)
		{
			a[i] = courseActivities[resourceIndices.get(i)];
		}
		
		return a;
	}
	
	public GradedItem[] getGradeItems()
	{
		GradedItem[] a = new GradedItem[gradedIndices.size()];
		Iterator<Integer> it = gradedIndices.iterator();
		
		for(int i=0; it.hasNext(); i++)
		{
			a[i] = (GradedItem) courseActivities[it.next()];
		}
		
		return a;
	}
	
	public static Course getCourseById(Course[] courses, int id)
	{
		for(Course course : courses)
		{
			if(course.getCourseId() == id)
			{
				return course;
			}						
		}
		
		return null;
	}
	
	public String getFullname()
	{
		return fullName;
	}
	
	public Student[] getEnrolledStudents() {
		return enrolledStudents;
	}

	public void setEnrolledStudents(Student[] enrolledStudents) {
		this.enrolledStudents = enrolledStudents;
	}
	
	public Student getStudentById(int studid)
	{
		if(this.enrolledStudents.length == 0)
		{
			return null;
		}
		else
		{
			for(Student s : this.enrolledStudents)
			{
				if(s.getId() == studid)
				{
					return s;
				}
			}
		}
		
		return null;
	}
}
