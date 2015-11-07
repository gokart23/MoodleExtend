package com.moodletrial;

enum Role
{
	STUDENT,
	MANAGER,
	TEACHER,
	UNREGISTERED//Any other role apart from these will appear as 'Unregistered' --> More roles can be added here
}

/**
 * Forms the template for any concrete implementation of a Role
 * 
 * @author Karthik Meher
 *
 */
abstract class Person
{
	protected int id;
	protected String username, fullname, email;
	protected Role role;
	protected Course[] courses;
	
	public int getId()
	{
		return id;
	}
	
	public Role getRole()
	{
		return role;
	}
	
	public String getUsername()
	{
		return username;
	}
	
	public String getFullname()
	{
		return fullname;
	}
	
	public String getEmail()
	{
		return email;
	}
	
	@Override
	public String toString()
	{
		StringBuffer r = new StringBuffer();
		
		r.append(this.fullname + "\t" + "ID: " + this.id);
		r.append("\n" + "Username: " + username + "\t" + "Email:" + email);
		r.append("\n" + "Enrolled courses:");
		for(Course c : courses)
		{
			r.append("\n" + c.getFullname());
		}
		
		return r.toString();
	}
}


/**
 * Concrete implementation of Person with the role of a Student
 * 
 * @author Karthik Meher
 *
 */
class Student extends Person{
	private int idNumber; // The optional Moodle field which may or may not be useful
	
	public Student(int id, String username, String fullname, String email, Course[] courses)
	{
		this.id = id;
		this.username = username;
		this.fullname = fullname;
		this.email = email;
		this.courses = courses;
		this.role = Role.STUDENT;
	}	
	
	public void setIdNumber(int idnum)
	{
		this.idNumber = idnum;
	}
	
	public int getId()
	{
		return id;
	}
	
	public String getFullname()
	{
		return fullname;
	}
	
	public static Student getStudentByEmail(Student[] courses, String email)
	{
		for(Student course : courses)
		{
			if(course.getEmail().equalsIgnoreCase(email))
			{
				return course;
			}						
		}
		
		return null;
	}
	
	public static Student getStudentById(Student[] courses, int id)
	{
		for(Student course : courses)
		{
			if(course.getId() == id)
			{
				return course;
			}						
		}
		
		return null;
	}
}
