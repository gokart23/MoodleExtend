package com.moodletrial;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jdom2.*;
import org.jdom2.input.*;
import org.xml.sax.InputSource;

/**
* Class containing low-level methods for network communication with Moodle Webservice API.
* Contains various utility methods which fetch the data and store in an object. The data interchange format used is XML
*  
* @author Karthik Meher D
*/

public class MoodleTestXML {
	
	
	/**
	 * The domain name of the site where the Moodle application runs 
	 */
	static final String domainName = "http://localhost:16000/moodle";
	static final String token = "11da9b0e58ab136cec1af1793248ca5f";//Variable to hold the token which is used for communication - might have to change
	static final int adminID = 2;//the admin id on my system is 2 - used for registering for a grade publishing key
	static int indent = 0;//helper variable for formatting options when testing
			
	/**
	 * Method for downloading file(s) associated with a particular resource object
	 * 
	 * @param folderPath Path of the folder where all the files need to be saved
	 * @param resource The Activity object of the resource to be downloaded
	 * @throws IOException IOException thrown when file could not be downloaded
	 * @throws MoodleException MoodleException thrown when resource passed has been improperly initialized or lacks fileURL parameter
	 */
	
	public static void downloadResource(String folderPath, Activity resource) throws IOException, MoodleException
	{
		//Check if the resource has been properly initialised, if not, throw MoodleException
		if(resource.fileURL == null)
		{
			throw new MoodleException("Improper resource method passed: FileURL not properly initialized for the resource");
		}
		
		//From the file URL specified, extract the extension of the resource, and the file name from the resource object
		String complPath = resource.fileURL.toString();
	 	String extn = complPath.substring(complPath.lastIndexOf("."), complPath.lastIndexOf("?"));
	 	String fn = folderPath + File.separator + resource.getFullname() + extn;
	 	
	 	//Call to helper method to download file
	 	downloadFile(fn, resource.fileURL);
	}
	
	/**
	 * Method for downloading a specified file. Helper function used by {@link MoodleTestXML#downloadFile(String, URL)} method
	 * 
	 * @param filename The filename with which specified file is to be saved
	 * @param downloadUrl The url from which the file can be downloaded
	 * @throws IOException IOException thrown when the file cannot be downloaded through passed URL
	 * @throws MoodleException MoodleException thrown when server replies with abnormal HTTP response code
	 */
	
	public static void downloadFile(String filename, URL downloadUrl) throws IOException, MoodleException
	{
		//Read data as a byte stream from the given URL, with the buffer size being specified by the variable below
		
		/**
		 * Final variable specifying the buffer size to be used by the byte stream for reading 
		 */
		final int BUFFER_SIZE = 4096;
		HttpURLConnection httpConn = (HttpURLConnection) downloadUrl.openConnection();
		int responseCode = httpConn.getResponseCode();
		
		//Proceed with the download only if normal response detected, else, throw MoodleException
		 if (responseCode == HttpURLConnection.HTTP_OK) {
	    	 	
	            InputStream inputStream = httpConn.getInputStream();
	            
	            FileOutputStream outputStream = new FileOutputStream(filename);
	 
	            int bytesRead = -1;
	            byte[] buffer = new byte[BUFFER_SIZE];
	            while ((bytesRead = inputStream.read(buffer)) != -1) {
	                outputStream.write(buffer, 0, bytesRead);
	            }
	 
	            outputStream.close();
	            inputStream.close();
	 
	            System.out.println("File downloaded");
	        } else {
	            throw new MoodleException("No file to download. Server replied HTTP code: " + responseCode);
	        }
	    httpConn.disconnect();
	}
	
	/**
	 * Helper method to obtain a Grade Publishing key for accessing the Grade report for the given activity. Uses custom web service function.	
	 * @param courseid The ID of the course internally used by Moodle, which is part of every course object
	 * @param userid The internal Moodle ID of the user in whose name the grade key is registered 
	 * @return The grade key which can be to used for obtaining grade reports for the specified course 
	 * @throws MoodleException {@link MoodleTestXML#connectMoodleXML(String, String)} forwarded Exception
	 */
	public static String getGradePublishKey(int courseid, int userid) throws MoodleException
	{
		//Straightforward code. Nothing to document
		Document doc = connectMoodleXML("local_surveyint_get_gradepublishkey", "&userid="+userid+"&courseid="+courseid);
		String key = doc.getRootElement().getChild("SINGLE").getChild("KEY").getChildText("VALUE");
		
		return key;
	}
	
	/**
	 * Method to obtain the data associated with a survey, in the form of a {@link SurveyResult} object. Uses custom web service function. Warning: doesn't check if valid ID has been entered!
	 * @param surveyActivityId The internal Moodle ID associated with the survey, part of every Activity object 
	 * @return {@link SurveyResult} object containing all the data retrieved from the webservice
	 * @throws MoodleException {@link MoodleTestXML#connectMoodleXML(String, String)} forwarded Exception
	 */
	public static SurveyResult getSurveyResults(int surveyActivityId) throws MoodleException
	{
		//Get and parse the XML data obtained by calling the web service function
		Document doc = connectMoodleXML("local_surveyint_get_results", "&id=" + surveyActivityId);
		
		//Store the data obtained in record form
		ArrayList<ArrayList<String>> records = new ArrayList<ArrayList<String>>();
		ArrayList<String> header = new ArrayList<String>();
		
		//The code below parses the XML tree, extracting relevant information. Refer to API doc of Moodle Webservices for exact XML response data
		List<Element> recs = doc.getRootElement().getChild("MULTIPLE").getChildren();
		
		for(int i = 0; i < recs.size(); i++)
		{
			ArrayList<String> temp = new ArrayList<String>(13);//Since the smallest survey result has 13 elements
			List<Element> keys = recs.get(i).getChildren();
			boolean flag = false;
			for(int j = 0; j < keys.size(); j++)
			{
				String tmp = keys.get(j).getChildText("VALUE");
				if(tmp.equals(""))
				{
					if(flag)
					{
						break;
					}
					else
					{
						flag = true;
						continue;
					}
				}
				else
				{
					if(flag)
					{
						flag = false;
					}
					temp.add(tmp);
				}
			}
			
			if(i == 0)
			{
				header = temp;
			}
			else
			{
				records.add(temp);
			}
		}
		
		return new SurveyResult(surveyActivityId, header, records);		
	}
	
	/**
	 * Method to obtain the course grades for given {@link Course} object, using Grade Publishing functionality.
	 * Uses the {@link MoodleTestXML#getGradePublishKey(int, int)} if the private key is absent and Apache Commons CSV API for CSV parsing. Fetches the grades for all the {@link GradedItem}s of that course and sets the grades obtained as mappings in the {@link GradedItem} (check documentation for the interface).
	 * @param course The course object whose grades are required
	 * @throws IOException IOException raised if the data cannot be retrieved
	 * @throws MoodleException {@link MoodleTestXML#connectMoodleXML(String, String)} forwarded Exception
	 */
	public static void getCourseGrades(Course course) throws IOException, MoodleException
	{
		final int numStudentColumns = 7; //Number of columns in the output containing student related information -- fixed!
		
		//Check if grade key is set, else fetch new grade key
		if(course.getGradeKey() == null)
		{
			//2 is the id of admin user on my system...any userid will do but it is HIGHLY recommended that a user with admin permissions be used
			course.setGradeKey(MoodleTestXML.getGradePublishKey(course.getCourseId(), adminID)); 
		}
		
		//URL for fetching the grade report
		URL r = new URL(MoodleTestXML.domainName + "grade/export/txt/dump.php?id=" + course.getCourseId() +"&groupid=&itemids=&export_letters=&export_feedback=0&updatedgradesonly=&displaytype=1&decimalpoints=2&export_onlyactive=1&usercustomfields=1&separator=comma&key=" + course.getGradeKey());

		//Parse the CSV data returned by the URL. Note that the Commons API function itself retrieves the data, without us fetching it
		CSVParser parser = CSVParser.parse(r, StandardCharsets.UTF_8, CSVFormat.EXCEL);
		CSVRecord header = parser.iterator().next();//The header record, containing column names
		int items = header.size();//Number of assignments/quizzes	
		GradedItem[] gis = new GradedItem[items - numStudentColumns];
		
		//IMP: The grade data for the student is stored as key-value pairs of the Student's email ID and the grade obtained by the student
		//	   Also note that utility functions exist for fetching a Student based on his/her email ID
		ArrayList<Map<String,Float>> lists = new ArrayList<Map<String, Float>>(items - numStudentColumns);
		
		//Code for processing the CSV data and extracting relevant information. For the exact structure of the CSV, try calling the above URL/using the grade export functionality of Grade module in Moodle
		for(int i = 0; i < (items - numStudentColumns); i++)
		{
			String[] st = header.get(i + numStudentColumns).toString().split(":");						
			gis[i] = (GradedItem)Activity.getActivityByName(course.getCourseActivities(), st[1].trim());
			lists.add(new HashMap<String, Float>());
		}
		
		for(CSVRecord rec : parser)
		{
			for(int i = 0; i < (items - numStudentColumns); i++)
			{
				String data = rec.get(i + numStudentColumns);
				if(data.equals("-"))
					lists.get(i).put(rec.get(5), Float.NaN);//IMP: If student hasn't attempted, then grade is stored as NaN
				else
					lists.get(i).put(rec.get(5), Float.parseFloat(data));
			}
		}
		
		for(int i = 0; i < gis.length; i++)
		{
			gis[i].setMarksMap(lists.get(i));
		}
		
//		Commented-out Code to display the obtained data		
//		for(GradedItem g : gis)
//		{
//			Map<String, Float> m = g.getMarksMap(); 
//			Set<String> s = m.keySet();
//			for(String st : s)
//			{
//				System.out.println(st + ": " + m.get(st));
//			}
//			System.out.println("-------");
//		}
	}
	
	/**
	 * Method to return all the courses registered in the Moodle Database. Returns the data in the form of an array of {@link Course}s
	 * @return Array of {@link Course} objects present in the database
	 * @throws MoodleException {@link MoodleTestXML#connectMoodleXML(String, String)} forwarded Exception
	 */
	public static Course[] getCourses() throws MoodleException
	{
		//Get and parse the XML data obtained by calling the web service function
		Document doc = connectMoodleXML("core_course_get_courses", "");
		
		//Code to do the actual processing of the XML tree and obtain relevant information. Note that the properties processed and saved as part of the course object are limited. 
		//For exact XML response structure, see Moodle Webservices API documentation
		Element response = doc.getRootElement();
		List<Element> courses = response.getChild("MULTIPLE").getChildren();
		Course[] courseList = new Course[courses.size()];
		Iterator<Element> iter = courses.iterator();
		
		for(int i = 0;iter.hasNext();i++)
		{
			int courseId = 0, categoryId = 0;
			String summary = null, summaryFormat = null, courseFormat = null, fullName=null, shortName=null;
			Element elem = iter.next();
			List<Element> keys = elem.getChildren("KEY");
			Iterator<Element> keyIter = keys.iterator();
			
			while(keyIter.hasNext())
			{
				Element temp = (Element) keyIter.next();
				switch(temp.getAttribute("name").getValue())
				{
				case "id": courseId = Integer.parseInt(temp.getChild("VALUE").getText());
					break;
				case "categoryid": categoryId = Integer.parseInt(temp.getChild("VALUE").getText());
					break;
				case "summary": summary = temp.getChild("VALUE").getText();
					break;
				case "summaryformat": summaryFormat = temp.getChild("VALUE").getText();
					break;
				case "format": courseFormat = temp.getChild("VALUE").getText();
					break;
				case "fullname": fullName = temp.getChild("VALUE").getText();
					break;
				case "shortname": shortName = temp.getChild("VALUE").getText();
					break;
				default:
						continue;				
				}
			}
			
			courseList[i] = new Course(courseId, categoryId, summary, summaryFormat, courseFormat, shortName, fullName);
			if(i>0)//Note: The site itself is always listed as a course -> remember that whenever processing the courses!
				courseList[i].setCourseActivities(MoodleTestXML.getCourseActivities(courseList[i].getCourseId()));
		}
		
		return courseList;
	}

	/**
	 * Method to retrieve the activities present in a given course. Warning: does not check whether valid course ID has been provided!
	 * @param courseid The internal Moodle ID associated with each course. Part of every {@link Course} object
	 * @return Array of {@link Activity} objects associated with the course. Does NOT set the course's Activity list
	 * @throws MoodleException {@link MoodleTestXML#connectMoodleXML(String, String)} forwarded Exception
	 */
	public static Activity[] getCourseActivities(int courseid) throws MoodleException
	{
		//Get and parse the XML data obtained by calling the web service function
		Document doc = connectMoodleXML("core_course_get_contents", "&courseid=" + courseid);
		
		//The code below parses the XML tree, extracting relevant information. Refer to API doc of Moodle Webservices for exact XML response data structure
		ArrayList<Activity> activities = new ArrayList<Activity>();
		Map<String, Assignment> assignments = new HashMap<String, Assignment>();
		List<Element> sections = doc.getRootElement().getChild("MULTIPLE").getChildren();
		Iterator<Element> secIter = sections.iterator();
		
		while(secIter.hasNext())
		{
			List<Element> keys = secIter.next().getChildren();
			Iterator<Element> keyIter = keys.iterator();			
			
			while(keyIter.hasNext())
			{
				Element key = keyIter.next();
				if(key.getAttribute("name").getValue().equals("modules"))
				{
					List<Element> activityRoot = key.getChild("MULTIPLE").getChildren();
					for(Element root : activityRoot)
					{
						int id = 0;
						URL urlLocation = null, fileURL=null;
						String name = null, description = null;
						ActivityType type = null;
						
						for(Element activityKey : root.getChildren())
						{
							Attribute attr = activityKey.getAttributes().get(0);
							switch(attr.getValue())
							{
							case "id": id = Integer.parseInt(activityKey.getChild("VALUE").getText());
								break;
							case "url": try {
									urlLocation = new URL(activityKey.getChild("VALUE").getText());
								} catch (MalformedURLException e) {
									// Auto-generated catch block
									e.printStackTrace();
								}
								break;
							case "name": name = activityKey.getChild("VALUE").getText();
								break;
							case "description": description = activityKey.getChild("VALUE").getText();
								break;
							case "modname":
								switch(activityKey.getChild("VALUE").getText())
								{
								case "resource": type = ActivityType.RESOURCE; 
									break;
								case "survey": type = ActivityType.SURVEY;
									break;
								case "assign": type = ActivityType.ASSIGNMENT;
									break;
								case "choice": type = ActivityType.CHOICE;
									break;
								case "quiz": type = ActivityType.QUIZ;
									break;
								default: type = ActivityType.UNREGISTERED;
								}
								break;
							case "contents": Element k = activityKey.getChild("MULTIPLE").getChild("SINGLE");
								if(k == null)
								{
									break;
								}
								else
								{
									List<Element> el = k.getChildren();
									for(Element e : el)
									{
										switch(e.getAttribute("name").getValue())
										{
										case "fileurl": try {
												fileURL = new URL(e.getChild("VALUE").getText() + "&token=" + MoodleTestXML.token);
											} catch (MalformedURLException e1) {
												// TODO Auto-generated catch block
												e1.printStackTrace();
											} 
											break;
										}
									}
								}
								break;
							}
						}
						if(type == ActivityType.ASSIGNMENT)
						{
							Assignment temp = (new Assignment(id, urlLocation, name, description, type, fileURL));
							activities.add(temp);
							assignments.put(temp.getFullname(), temp);
						}
						else if(type == ActivityType.QUIZ)
							activities.add(new Quiz(id, urlLocation, name, description, type, fileURL));
						else
							activities.add(new Activity(id, urlLocation, name, description, type, fileURL));
					}
				}
			}			
		}
		
		//For each assignment, the Assignment ID and the Activity ID assigned by Moodle are different. The assignment ID is required to obtain student submissions, whereas everything else requires activity ID
		//Following code obtains the assignment IDs for the assignments, as well as the submission type for the assignment
		doc = connectMoodleXML("mod_assign_get_assignments", "&courseids[0]=" + courseid);			
		List<Element> keys = doc.getRootElement().getChildren().get(0).getChildren();
		for(Element e : keys)
		{
			if(e.getAttributeValue("name").equals("courses"))
			{
				List<Element> le = e.getChild("MULTIPLE").getChild("SINGLE").getChildren();
				for(Element l : le)
				{
					if(l.getAttributeValue("name").equals("assignments"))
					{
						List<Element> assign = l.getChild("MULTIPLE").getChildren();
						String name=null;
						int id=0, configCode=0;
						
						for(Element a : assign)
						{
							List<Element> kys = a.getChildren();
							
							for(Element el : kys)
							{
								if(el.getAttributeValue("name").equals("id"))
								{
									id = Integer.parseInt(el.getChildText("VALUE"));
								}
								else if(el.getAttributeValue("name").equals("configs"))
								{
									List<Element> configs = el.getChild("MULTIPLE").getChildren();
									for(Element c : configs)
									{
										List<Element> k = c.getChildren();
										
										int i=0;//1 -> onlinetext 2-> filesubmission
										boolean flag1=false, flag2=false;										
																				
										for(Element ky : k)
										{
											if(ky.getAttributeValue("name").equals("plugin"))
											{
												if(ky.getChildText("VALUE").equals("onlinetext"))
													i=1;
												else if(ky.getChildText("VALUE").equals("file"))
													i=2;
											}
											else if(ky.getAttributeValue("name").equals("name") && ky.getChildText("VALUE").equals("enabled"))
											{
												flag1=true;
											}
											else if(ky.getAttributeValue("name").equals("value"))
											{
												String ab = ky.getChildText("VALUE");
												if(ab.equals("1"))
													flag2 = true;
											}
											
											if(flag1 && flag2)
											{
												if(i == 1)
												{
													configCode += 1;
												}
												else if(i == 2)
												{
													configCode += 2;
												}
											}
										}
									}
									
								}
								else if(el.getAttributeValue("name").equals("name"))
								{
									name = el.getChildText("VALUE");
								}
							}
							
							Assignment temp = assignments.get(name);
							temp.setAssignmentId(id);
							temp.setSubmissionType(configCode);
						}
					}
				}
			}
		}
		
		Activity[] a = new Activity[activities.size()]; 
		activities.toArray(a);
		
		return a;
	}
	
	/**
	 * Method to get the students enrolled in a given course. Note that it excludes any person whose role is not that of a Student.
	 * Also obtains all the data associated with a {@link Student}
	 * 
	 * @param courseid The internal Moodle ID of the course for which student list is being requested
	 * @param knownCourses The list of courses known uptil now. Used for cross-referencing the courses known with the a particular student's list of enrolled courses
	 * @return Array of {@link Student} objects who are enrolled in a particular course
	 * @throws MoodleException {@link MoodleTestXML#connectMoodleXML(String, String)} forwarded Exception
	 */
	public static Student[] getEnrolledUsers(int courseid, Course[] knownCourses) throws MoodleException
	{
		//Get and parse the XML data obtained by calling the web service function
		Document doc = connectMoodleXML("core_enrol_get_enrolled_users", "&courseid=" + courseid);
		
		//The code below parses the XML tree, extracting relevant information. Refer to API doc of Moodle Webservices for exact XML response data structure
		ArrayList<Student> students = new ArrayList<Student>();
		List<Element> coursePeople = doc.getRootElement().getChild("MULTIPLE").getChildren();
		
		for(Element person : coursePeople)
		{
			List<Element> keys = person.getChildren();
			
			int id = 0;
			String username = null, fullname = null, email = null;
			Role role;
			ArrayList<Course> courses = new ArrayList<Course>();
			boolean flag = true;//Flag variable to indicate whether current data is that of a student or not
			
			for(Element key : keys)
			{
				switch(key.getAttribute("name").getValue())
				{
				case "id": if(flag){id = Integer.parseInt(key.getChild("VALUE").getText());}
					break;
				case "username": if(flag){username = key.getChild("VALUE").getText();}
					break;
				case "fullname": if(flag){fullname = key.getChild("VALUE").getText();}
					break;
				case "email": if(flag){email = key.getChild("VALUE").getText();}
					break;
				case "roles":
					List<Element> roles = key.getChild("MULTIPLE").getChildren();
					for(Element el : roles)
					{
						List<Element> k = el.getChildren();
						for(Element ky : k)
						{
							if(ky.getAttribute("name").getValue().equals("shortname")){
													
								switch(ky.getChild("VALUE").getText())
								{
								case "student": role = Role.STUDENT;
									break;
									//As of now, necessity for roles other than that of Student wasn't observed, but code segment has been kept to enable extension in the future
			//					case "manager": role = Role.MANAGER;					
			//						break;
			//					case "teacher": role = Role.TEACHER;
			//						break;
			//					default: role = Role.UNREGISTERED;
								default: flag = false;
									break;
								}
							}
						}
					}
					break;
				case "enrolledcourses":
					if(flag){
						List<Element> enCourses = key.getChild("MULTIPLE").getChildren();
						for(Element el : enCourses)
						{
							for(Element courseKeys : el.getChildren())
							{
								if(courseKeys.getAttribute("name").getValue().equals("id"))
								{
									Course co = Course.getCourseById(knownCourses, Integer.parseInt(courseKeys.getChild("VALUE").getText()));
									if(co != null)
										courses.add(co);
								}
							}
						}
					}
					break;
				}
			}
			
			if(flag)
			{
				Course[] c = new Course[courses.size()];
				courses.toArray(c);
				Student stud = new Student(id, username, fullname, email, c);
				students.add(stud);
			}
		}		
		
		Student[] s = new Student[students.size()];
		students.toArray(s);
		return s;
	}	
	
//	Note: Rudimentary function to obtain user details, given Moodle internal user ID. Found to be not very useful, hence no concrete implementation
//	public static Document getUserById(int id)
//	{
//		return connectMoodleXML("core_user_get_users_by_field", "&field=id" + "&values[0]=" + id);
//	}
	
	/**
	 * Most fundamental function which actually interacts with Moodle webservices API, retrieves data in XML format, builds DOM tree and returns resulting document.
	 * Uses JDOM library.
	 * @param functionName Name of the Moodle Webservice function which is to be called. Name should be specified EXACTLY as mentioned in webservices API
	 * @param parameters String containing the POST parameters to be sent as arguments to the webservice function 
	 * @return Returns JDOM library's Document object containing the parsed XML tree, for further processing
	 * @throws MoodleException Umbrella exception thrown instead of MalformedURLException, IOException, JDOMException with details
	 */
	public static Document connectMoodleXML(String functionName, String parameters) throws MoodleException//throws MalformedURLException, IOException, JDOMException
	{	
		try
		{
			String urlParameters = "moodlewsrestformat=xml" + parameters;
			String serverurl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;
			
			HttpURLConnection con = (HttpURLConnection) new URL(serverurl).openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("Content-Language", "en-US");
			con.setDoOutput(true);
			con.setUseCaches(false);
			con.setDoInput(true);
			
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();
			
			InputStream is = con.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));	
			
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(new InputSource(rd));
			
			rd.close();
			con.disconnect();
			
			return doc;
		}
		catch(Exception e)
		{
			throw new MoodleException("Unable to connect to Moodle Webservices. Internal error: " + e.getMessage());			
		}
	}	
	
	/**
	 * Helper function to visualise the DOM XML tree generated.	
	 * @param node The node from which the tree is generated
	 */
	public static void iterateElements(Element node)
	{
		List<Element> children = node.getChildren();
		Iterator<Element> iter = children.iterator();
		
		if(children.isEmpty())
			return;
		
		while(iter.hasNext())
		{
			Element temp = iter.next();
			for(int i = 0; i <= indent; i++)
			{
				System.out.print("\t");
			}
			System.out.print((temp.getName() + ": " + temp.getText()).trim());
			Attribute attr = temp.getAttribute("name");
			if(attr != null)
			{
				System.out.print("\t" + attr.getValue());
			}
			System.out.println();
			indent++;
			iterateElements(temp);
			indent--;
		}
	}
}

