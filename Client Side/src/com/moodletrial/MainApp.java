package com.moodletrial;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

public class MainApp {
	//Implementation of most of the functions written so far
	public static void main(String[] args) throws ProtocolException, IOException, ParserConfigurationException, SAXException, JDOMException, MoodleException 
	{	
		Course[] courses = MoodleTestJSON.getCourses();		
		
		for(int i = 1; i < courses.length; i++)
		{
			System.out.println(courses[i] + "\n\n");
			
			courses[i].setCourseActivities(MoodleTestJSON.getCourseActivities(courses[i].getCourseId()));
			courses[i].setEnrolledStudents(MoodleTestJSON.getEnrolledUsers(courses[i].getCourseId(), courses));
			MoodleTestJSON.getCourseGrades(courses[i]);
			
			for(Activity activity : courses[i].getCourseActivities())
			{
				System.out.println(activity);
				System.out.println("----------------------------------\n");
				
				if(activity instanceof GradedItem)
				{
					System.out.println("GRADES:\n");
					Map<String, Float> marks = ((GradedItem) activity).getMarksMap();
					for(String email : marks.keySet())
					{
						System.out.println(Student.getStudentByEmail(courses[i].getEnrolledStudents(), email).getFullname() + " : " + marks.get(email));
					}
			
					if(activity instanceof Assignment)
					{
						MoodleTestJSON.downloadAssignmentSubmissions("B:", (Assignment)activity, courses[i].getEnrolledStudents());
					}				
			
					
					System.out.println("----------------------------------\n\n");
				}
				else if(activity.getActivityType() == ActivityType.SURVEY)
				{
					System.out.println("RESULTS:\n");
					SurveyResult result = MoodleTestJSON.getSurveyResults(activity.getId());
					System.out.println(result);
					System.out.println("----------------------------------\n\n");
				}
				
			}
		
			for(Student stud : courses[i].getEnrolledStudents())
			{
				System.out.println(stud + "\n");
			}
			
			for(Activity r : courses[i].getResourceList())
			{
				MoodleTestXML.downloadResource("B:\\Acad", r);
			}
			
			System.out.println("----------------------------------\n----------------------------------\n\n");
		}				
	}
}
