package com.moodletrial;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
* Class containing low-level methods for network communication with Moodle Webservice API.
* Contains various utility methods which fetch the data and store in an object. The data interchange format used is JSON
*  
* @author Karthik Meher D
*/

public class MoodleTestJSON {
	
	/**
	 * The domain name of the site where the Moodle application runs 
	 */
	static final String domainName = "http://localhost:16000/moodle";
	static final String token = "11da9b0e58ab136cec1af1793248ca5f";//Variable to hold the token which is used for communication - might have to change
	static final int adminID = 2;//the admin id on my system is 2 - used for registering for a grade publishing key
	static int indent = 0;//helper variable for formatting options when testing
	
	/**
	 * Method to download the submissions for a given {@link Assignment}.Warning: Non-submissions are not indicated
	 * @param folderPath The path of the folder where the downloaded files are to stored
	 * @param assignment The {@link Assignment} object whose submissions are to be downloaded
	 * @param students Array of {@link Student}s supposed to submit the {@link Assignment}
	 * @throws MoodleException MoodleException thrown to indicate failure in downloading assignments. Multiple reasons possible	 * 
	 * @throws IOException IOException thrown to indicate failure in writing to disk
	 */
	public static void downloadAssignmentSubmissions(String folderPath, Assignment assignment, Student[] students) throws MoodleException, IOException
	{
		JsonObject doc = ((JsonObject)connectMoodleJSON("mod_assign_get_submissions", "&assignmentids[0]=" + assignment.getAssignmentId()));
		
		//check warnings to ensure that submissions are present before getting down to processing them
		JsonArray warnings = doc.getJsonArray("warnings");
		if(warnings.size() != 0)
		{
			for(int i = 0; i < warnings.size(); i++)
			{
				JsonObject o = warnings.getJsonObject(i);
				if(o.getString("warningcode").equals("3"))
				{
					System.out.println("No submissions found for this assignment!");
					return;
				}
			}
		}
		
		JsonArray submissions = doc.getJsonArray("assignments").getJsonObject(0).getJsonArray("submissions");
		int submissionType = assignment.getSubmissionType();
				
		for(int i = 0; i < submissions.size(); i++)
		{
			JsonObject studSub = submissions.getJsonObject(i);
			Student s = Student.getStudentById(students, studSub.getInt("userid"));
			JsonArray plugins = studSub.getJsonArray("plugins");
			
			switch(submissionType)
			{
			case 1:	for(int j = 0; j < plugins.size(); j++)
					{
						String filename = s.getFullname() + "-" + "--Submission-" + assignment.getFullname() + ".txt";
						String fileContent = null;
						JsonObject ob = plugins.getJsonObject(j);
						if(ob.getString("type").equals("onlinetext"))
						{
							fileContent = ob.getJsonArray("editorfields").getJsonObject(0).getString("text");
							
							File f = new File(folderPath + File.separator + filename);
							FileOutputStream fos = new FileOutputStream(f);
							if(!f.exists())
							{
								f.createNewFile();
							}
							
							byte[] contentInBytes = fileContent.getBytes();		 
							fos.write(contentInBytes);
							fos.flush();
							fos.close();
							
							System.out.println("Downloaded file - " + filename);
						}
					}					
				break;
			
			case 2: for(int j = 0; j < plugins.size(); j++)
					{
						JsonObject ob = plugins.getJsonObject(j);
						if(ob.getString("type").equals("file"))
						{
							JsonArray files = ob.getJsonArray("fileareas").getJsonObject(0).getJsonArray("files");
							for(int k = 0; k < files.size(); k++)
							{
								JsonObject file = files.getJsonObject(k);
								String filepath = file.getString("filepath");
								String fname = s.getFullname() + "-" + "--Submission-" + k + "-" + filepath.substring(filepath.lastIndexOf("/") + 1);
								System.out.println(fname);
								downloadFile(folderPath + File.separator + fname, new URL(file.getString("fileurl") + "?token=" + MoodleTestXML.token));
								System.out.println("Downloaded file - " + fname);
							}
						}
						
						
					}
				break;
			case 3:for(int j = 0; j < plugins.size(); j++)
					{
						JsonObject ob = plugins.getJsonObject(j);
						if(ob.getString("type").equals("file"))
						{
							JsonArray files = ob.getJsonArray("fileareas").getJsonObject(0).getJsonArray("files");
							for(int k = 0; k < files.size(); k++)
							{
								JsonObject file = files.getJsonObject(k);
								String filepath = file.getString("filepath");
								String fname = s.getFullname() + "-" + "--Submission-" + k + "-" + filepath.substring(filepath.lastIndexOf("/") + 1);
								System.out.println(fname);
								downloadFile(folderPath + File.separator + fname, new URL(file.getString("fileurl") + "?token=" + MoodleTestXML.token));
							}
						}
						else if(ob.getString("type").equals("onlinetext"))
						{
							String filename = s.getFullname() + " - " + s.getEmail() + " : Submission - " + assignment.getFullname() + ".txt";
							String fileContent = studSub.getJsonArray("editorfields").getJsonObject(0).getString("text");
							
							File f = new File(folderPath + File.separator + filename);
							FileOutputStream fos = new FileOutputStream(f);
							if(!f.exists())
							{
								f.createNewFile();
							}
							
							byte[] contentInBytes = fileContent.getBytes();		 
							fos.write(contentInBytes);
							fos.flush();
							fos.close();
							
							System.out.println("Downloaded file - " + filename);
						}
					}
				break;
			default: throw new MoodleException("Improperly initialized assignment(assignment submission type not set)");
			}
		}		
	}
	
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
	            
	        } else {
	        	throw new MoodleException("No file to download. Server replied HTTP code: " + responseCode);
	        }
	    httpConn.disconnect();
	}
	
	/**
	 * Method to obtain the data associated with a survey, in the form of a {@link SurveyResult} object. Uses custom web service function. Warning: doesn't check if valid ID has been entered!
	 * @param surveyActivityId The internal Moodle ID associated with the survey, part of every Activity object 
	 * @return {@link SurveyResult} object containing all the data retrieved from the webservice
	 * @throws MoodleException {@link MoodleTestJSON#connectMoodleJSON(String, String)} forwarded Exception
	 */
	public static SurveyResult getSurveyResults(int surveyActivityId) throws MoodleException
	{
		//Obtain the JsonStructure and process it to extract relevant information
		JsonArray doc = (JsonArray)connectMoodleJSON("local_surveyint_get_results", "&id=" + surveyActivityId);
		
		ArrayList<String> header = new ArrayList<String>();
		ArrayList<ArrayList<String>> records = new ArrayList<ArrayList<String>>();
		
		for(int i = 0; i < doc.size(); i++)
		{
			JsonArray record = doc.getJsonArray(i);
			int size = record.size();
			if(i == 0)
			{
				for(int j = 0; j < size; j++)
				{
					header.add(record.getString(j));
				}
			}
			else
			{
				ArrayList<String> tmp = new ArrayList<String>();
				for(int j = 0; j < size; j++)
				{
					tmp.add(record.getString(j));
				}
				records.add(tmp);
			}
		}
		
		return new SurveyResult(surveyActivityId, header, records);
	}
	
	/**
	 * Method to obtain the course grades for given {@link Course} object, using Grade Publishing functionality.
	 * Uses the {@link MoodleTestXML#getGradePublishKey(int, int)} if the private key is absent and Apache Commons CSV API for CSV parsing. Fetches the grades for all the {@link GradedItem}s of that course and sets the grades obtained as mappings in the {@link GradedItem} (check documentation for the interface).
	 * @param course The course object whose grades are required
	 * @throws IOException IOException raised if the data cannot be retrieved
	 * @throws MoodleException {@link MoodleTestJSON#connectMoodleJSON(String, String)} forwarded Exception
	 */
	public static void getCourseGrades(Course course) throws IOException, MoodleException
	{
		final int numStudentColumns = 7; //Number of columns in the output containing student related information -- fixed!
		
		//Check if grade key is set, else fetch new grade key
		if(course.getGradeKey() == null)
		{
			//2 is the id of admin user on my system...any userid will do but it is HIGHLY recommended that a user with admin permissions be used
			course.setGradeKey(MoodleTestJSON.getGradePublishKey(course.getCourseId(), MoodleTestXML.adminID));
		}
		
		//URL for fetching the grade report
		URL r = new URL(MoodleTestJSON.domainName + "/grade/export/txt/dump.php?id=" + course.getCourseId() +"&groupid=&itemids=&export_letters=&export_feedback=0&updatedgradesonly=&displaytype=1&decimalpoints=2&export_onlyactive=1&usercustomfields=1&separator=comma&key=" + course.getGradeKey());

		//Parse the CSV data returned by the URL. Note that the Commons API function itself retrieves the data, without us fetching it
		CSVParser parser = CSVParser.parse(r, StandardCharsets.UTF_8, CSVFormat.EXCEL);
		CSVRecord header = parser.iterator().next();
		int items = header.size();//Number of assignments/quizzes	
		GradedItem[] gis = new GradedItem[items - numStudentColumns];
		
		//IMP: The grade data for the student is stored as key-value pairs of the Student's email ID and the grade obtained by the student
		//	   Also note that utility functions exist for fetching a Student based on his/her email ID
		ArrayList<Map<String,Float>> lists = new ArrayList<Map<String, Float>>(items - numStudentColumns);
		
		for(int i = 0; i < (items - numStudentColumns); i++)
		{
			String[] st = header.get(i + numStudentColumns).toString().split(":");
			if(course.getCourseActivities() == null)
			{
				course.setCourseActivities(getCourseActivities(course.getCourseId()));
			}
			gis[i] = (GradedItem)Activity.getActivityByName(course.getCourseActivities(), st[1].trim());
			lists.add(new HashMap<String, Float>());
		}
		
		//Code for processing the CSV data and extracting relevant information. For the exact structure of the CSV, try calling the above URL/using the grade export functionality of Grade module in Moodle
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
	 * Helper method to obtain a Grade Publishing key for accessing the Grade report for the given activity. Uses custom web service function.	
	 * @param courseid The ID of the course internally used by Moodle, which is part of every course object
	 * @param userid The internal Moodle ID of the user in whose name the grade key is registered 
	 * @return The grade key which can be to used for obtaining grade reports for the specified course 
	 * @throws MoodleException {@link MoodleTestXML#connectMoodleXML(String, String)} forwarded Exception
	 */
	public static String getGradePublishKey(int courseId, int adminid) throws MoodleException {
		JsonObject doc = (JsonObject)connectMoodleJSON("local_surveyint_get_gradepublishkey", "&userid="+adminid+"&courseid="+courseId);
		return doc.getString("key");
	}

	/**
	 * Method to get the students enrolled in a given course. Note that it excludes any person whose role is not that of a Student.
	 * Also obtains all the data associated with a {@link Student}
	 * 
	 * @param courseid The internal Moodle ID of the course for which student list is being requested
	 * @param knownCourses The list of courses known uptil now. Used for cross-referencing the courses known with the a particular student's list of enrolled courses
	 * @return Array of {@link Student} objects who are enrolled in a particular course
	 * @throws MoodleException {@link MoodleTestJSON#connectMoodleJSON(String, String)} forwarded Exception
	 */	
	public static Student[] getEnrolledUsers(int courseid, Course[] knownCourses) throws MoodleException
	{
		ArrayList<Student> students = new ArrayList<Student>();
		
		JsonArray doc = (JsonArray)connectMoodleJSON("core_enrol_get_enrolled_users", "&courseid=" + courseid);
		
		for(int i = 0; i < doc.size(); i++)
		{
			JsonObject user = doc.getJsonObject(i);
			//As of now, necessity for roles other than that of student hasn't been observed. If required, extend cases here
			if(user.getJsonArray("roles").getJsonObject(0).getString("shortname").equalsIgnoreCase("student"))
			{
				Course[] c=null;
				if(user.containsKey("enrolledcourses"))
				{
					ArrayList<Course> cs = new ArrayList<Course>();
					JsonArray cr = user.getJsonArray("enrolledcourses");
					for(int j = 0; j < cr.size(); j++)
					{
						JsonObject course = cr.getJsonObject(j);
						cs.add(Course.getCourseById(knownCourses, course.getInt("id")));
					}
					c = new Course[cs.size()];
					cs.toArray(c);
				}
				
				students.add(new Student(user.getInt("id"), user.containsKey("username")?user.getString("username"):"[username not available]", user.getString("fullname"), user.containsKey("email")?user.getString("email"):"", c));
			}
			else
			{
				continue;
			}
		}
		
		Student[] s = new Student[students.size()];
		students.toArray(s);
		
		return s;
	}
	
	/**
	 * Method to retrieve the activities present in a given course. Warning: does not check whether valid course ID has been provided!
	 * @param courseid The internal Moodle ID associated with each course. Part of every {@link Course} object
	 * @return Array of {@link Activity} objects associated with the course. Does NOT set the course's Activity list
	 * @throws MoodleException {@link MoodleTestXML#connectMoodleXML(String, String)} forwarded Exception
	 */
	public static Activity[] getCourseActivities(int courseid) throws MoodleException
	{
		JsonArray doc = (JsonArray)connectMoodleJSON("core_course_get_contents", "&courseid=" + courseid);
		
		ArrayList<Activity> activities = new ArrayList<Activity>();
		Map<String, Assignment> assignments = new HashMap<String, Assignment>();
						
		for(int i = 0; i < doc.size(); i++)
		{
			JsonObject sections = doc.getJsonObject(i);
			JsonArray modules = sections.getJsonArray("modules");
			
			for(int j = 0; j < modules.size(); j++)
			{
				JsonObject activity = modules.getJsonObject(j);
				try
				{
					switch(activity.getString("modname"))
					{
					//My moodle system doesn't return description in json format; if it is present, it will be included, else an error string will be written
					case "resource": activities.add(new Activity(activity.getInt("id"), new URL(activity.getString("url")), activity.getString("name"), activity.containsKey("description")?activity.getString("description"):"[Decription not available]", ActivityType.RESOURCE, new URL(activity.getJsonArray("contents").getJsonObject(0).getString("fileurl") + "&token=" + MoodleTestXML.token)));							
						break;
					case "survey": activities.add(new Activity(activity.getInt("id"), new URL(activity.getString("url")), activity.getString("name"),activity.containsKey("description")?activity.getString("description"):"[Decription not available]", ActivityType.SURVEY, null));
						break;
					case "assign": Assignment temp = new Assignment(activity.getInt("id"), new URL(activity.getString("url")), activity.getString("name"),activity.containsKey("description")?activity.getString("description"):"[Decription not available]", ActivityType.ASSIGNMENT, null); 
						activities.add(temp);
						assignments.put(temp.name, temp);
						break;
					case "choice": activities.add(new Activity(activity.getInt("id"), new URL(activity.getString("url")), activity.getString("name"),activity.containsKey("description")?activity.getString("description"):"[Decription not available]", ActivityType.CHOICE, null));
						break;
					case "quiz": activities.add(new Quiz(activity.getInt("id"), new URL(activity.getString("url")), activity.getString("name"),activity.containsKey("description")?activity.getString("description"):"[Decription not available]", ActivityType.QUIZ, null));
						break;
					default: activities.add(new Activity(activity.getInt("id"), new URL(activity.getString("url")), activity.getString("name"),activity.containsKey("description")?activity.getString("description"):"[Decription not available]", ActivityType.UNREGISTERED, null));
							break;
					}
				}
				catch(MalformedURLException me)
				{
					me.printStackTrace();
				}
			}			
		}
		
		//For each assignment, the Assignment ID and the Activity ID assigned by Moodle are different. The assignment ID is required to obtain student submissions, whereas everything else requires activity ID
		//Following code obtains the assignment IDs for the assignments, as well as the submission type for the assignment
		JsonArray assignment = (JsonArray) ((JsonObject)connectMoodleJSON("mod_assign_get_assignments", "&courseids[0]=" + courseid)).getJsonArray("courses").getJsonObject(0).getJsonArray("assignments");
		
		for(int i = 0; i < assignment.size(); i++)
		{
			JsonObject a = assignment.getJsonObject(i);
			Assignment an = assignments.get(a.getString("name"));
			an.setAssignmentId(a.getInt("id"));
			JsonArray configs = a.getJsonArray("configs");
			
			for(int j = 0; j < configs.size(); j++)
			{
				JsonObject ob = configs.getJsonObject(j);
				if(ob.getString("plugin").equals("onlinetext") && ob.getString("name").equals("enabled"))
				{
					if(ob.getString("value").equals("1"))
					{
						an.setSubmissionType(an.getSubmissionType() + 1);
					}					
				}
				else if(ob.getString("plugin").equals("file") && ob.getString("name").equals("enabled"))
				{
					if(ob.getString("value").equals("1"))
					{
						an.setSubmissionType(an.getSubmissionType() + 2);
					}
				}
			}
		}		
		
		Activity[] a = new Activity[activities.size()];
		activities.toArray(a);
		
		return a;
	}
	
	/**
	 * Method to retrieve the activities present in a given course. Warning: does not check whether valid course ID has been provided!
	 * @param courseid The internal Moodle ID associated with each course. Part of every {@link Course} object
	 * @return Array of {@link Activity} objects associated with the course. Does NOT set the course's Activity list
	 * @throws MoodleException {@link MoodleTestJSON#connectMoodleJSON(String, String)} forwarded Exception
	 */
	public static Course[] getCourses() throws MoodleException
	{
		//Extremely simplified code when parsed in JSON. Go through XML version for the difference :p
		ArrayList<Course> courses = new ArrayList<Course>();
		
		JsonArray doc = (JsonArray)connectMoodleJSON("core_course_get_courses", "");
		
		for(int i = 0; i < doc.size(); i++)
		{
			JsonObject course = doc.getJsonObject(i);
			courses.add(new Course(course.getInt("id"), course.getInt("categoryid"), course.getString("summary"), new Integer(course.getInt("summaryformat")).toString(), course.getString("format"), course.getString("shortname"), course.getString("fullname")));
		}
		
		Course[] courseList = new Course[courses.size()];
		courses.toArray(courseList);
		
		return courseList;
	}
		
	/**
	 * Most fundamental function which actually interacts with Moodle webservices API, retrieves data in JSON format, builds tree and returns JsonStructure.
	 * Uses JSON-P Library (JSR-353 specification)
	 * @param functionName Name of the Moodle Webservice function which is to be called. Name should be specified EXACTLY as mentioned in webservices API
	 * @param parameters String containing the POST parameters to be sent as arguments to the webservice function 
	 * @return Returns JsonStructure object containing the parsed XML tree, for further processing
	 * @throws MoodleException Umbrella exception thrown instead of MalformedURLException, IOException with details
	 */
	public static JsonStructure connectMoodleJSON(String functionName, String parameters) throws MoodleException
	{
		try
		{
			String urlParameters = "moodlewsrestformat=json" + parameters;
			String serverurl = MoodleTestXML.domainName + "/webservice/rest/server.php" + "?wstoken=" + MoodleTestXML.token + "&wsfunction=" + functionName;
			
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
			JsonReader reader = Json.createReader(is);
			JsonStructure doc = reader.read();
			
			return doc;
			
		}
		catch(Exception e)
		{
			throw new MoodleException("Unable to connect to Moodle Webservices. Internal error: " + e.getMessage());
		}		
	}
	
	/**
	 * Helper function to visualise the json data received from the webservice
	 * @param tree JsonValue object which is then explored to generate the tree
	 * @param key For printing out key value pairs. If not a JsonObject, provide null
	 */
	public static void navigateTree(JsonValue tree, String key) {
		   if (key != null)
		   {
			   for(int i = 0; i <= indent; i++)
		        	 System.out.print("\t");
		      System.out.print("Key " + key + ": ");
		   }
		   switch(tree.getValueType()) {
		      case OBJECT:
		    	  for(int i = 0; i <= indent; i++)
			        	 System.out.print("\t");
		    	 System.out.println("OBJECT");
		         JsonObject object = (JsonObject) tree;
		         indent++;
		         for (String name : object.keySet())
		            navigateTree(object.get(name), name);
		         indent--;
		         break;
		      case ARRAY:
		    	 System.out.println("ARRAY");
		         JsonArray array = (JsonArray) tree;
		         indent++;
		         for (JsonValue val : array)
		            navigateTree(val, null);
		         indent--;		         
		         break;
		      case STRING:
		         JsonString st = (JsonString) tree;
		         System.out.println("STRING " + st.getString());
		         break;
		      case NUMBER:
		         JsonNumber num = (JsonNumber) tree;
		         System.out.println("NUMBER " + num.toString());
		         break;
		      case TRUE:
		      case FALSE:
		      case NULL:
		    	 System.out.println(tree.getValueType().toString());
		         break;
		   }
		}
}
