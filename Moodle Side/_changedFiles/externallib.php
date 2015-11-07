<?php
/**
*@author Karthik Meher
*
*/

//Including the various libraries necessary for webservice
//These files are required in every external function and must always be present
require_once("$CFG->libdir/externallib.php");
require_once("$CFG->libdir/moodlelib.php");
require_once("../../config.php");

//These files are specific to this plugin only
require_once($CFG->dirroot.'/mod/resource/locallib.php');
require_once($CFG->libdir.'/completionlib.php');
require_once("$CFG->dirroot/mod/feedback/lib.php");

//Moodle naming convention dictates that the local plugin function names follow the pattern : local_PLUGINNAME_FUNCTION_NAME
//All external functions(including descriptors) must be public and static. They must be present in a class which extends the 'external_api' class

//Every Moodle webservice function declaration requires 3 functions in all: 2 descriptor functions and the actual implementation of the function itself
//
//1. The first descriptor function specifies the parameters accepted by the actual implementation, as well as the type of those parameters
//	 It is named as FUNCTION_NAME_parameters() and returns an external_function_parameters object. This object is instantiated by passing an array, with the
//   variable name as the key and the associated value being an 'external_value' object, that specifies the type and description of the variable.
//	 The different types present are : PARAM_INT -> integers
//									   PARAM_TEXT -> strings
//									   PARAM_RAW -> strings which have not been altered in any way (generally used for passwords)
//									   PARAM_FLOAT -> floating point values
//   These are the most commonly used types. Other types may be found in /lib/moodlelib.php, where are the basic constants are defined.
//	 Also note that the external_value class can be used to specify whether a given value is required(error if not present -> VALUE_REQUIRED), optional(ignored if not present -> VALUE_OPTIONAL) or default(default value used if not present -> VALUE_DEFAULT).
//	 By default, the value is VALUE_REQUIRED
//
//2. The second descriptor function specifies the return type of the implementation, as well as the type(s) of the return value(s)
//	 It is named as FUNCTION_NAME_returns() and it returns one of three different types of objects, which may also be nested in each other.
//   The three different possible return values are : external_multiple_structure -> used to indicate that the return value may contain an arbitrary number of objects in list form, of the type specified in the constructor. Generally used to represent an array
//													  external_single_structure -> used to indicate an object, with the properties of the object specified as an array of key-value pairs, with the value part being of the type of external_value
//													  external_value -> used to represent the primitive types, such as PARAM_INT, PARAM_FLOAT, PARAM_TEXT, PARAM_RAW.
//	 Examples: 
//			When only a string is to be returned : return new external_value(PARAM_TEXT, 'String is being returned')
//			When an array of objects with certain properties is to returned: return new external_multiple_structure(
//																						new external_single_structure(array(
//																								'key' => new external_value(PARAM_INT, 'an int return value'),
//																								'key2' => new external_value(PARAM_TEXT, 'an optional string return value', VALUE_OPTIONAL)
//																					))
//			When an array of arrays containing strings is to be returned: return new external_multiple_structure(
//																						new external_multiple_structure(
//																							new external_value(PARAM_TEXT, 'some string being returned')
//																					))
//			Note that is the returned object doesn't match the description, the server will return an error in the sent data, but if the syntax of the object description is wrong, the webservice won't compile and an error will be shown on the webservice pages.

class local_surveyint_external extends external_api
{
	//Common source of error: failing to include the $DB or $CFG superglobals
	public static $recordFields;
	
	public static function get_feedbackresults_parameters() {
	        return new external_function_parameters(
	            array(
	                'id' => new external_value(PARAM_INT, 'The id of the feedback module whose results are to be fetched'),
	            )
	        );
	    }

	public static function get_feedbackresults_returns()
	{
		return new external_multiple_structure(
			new external_multiple_structure(
					new external_value(PARAM_TEXT, 'Fields of the feedback')
				)
		);
	}

	public static function get_feedbackresults($id)
	{		
		global $DB;

		//Fetch the course module object from its id, validate it
		if (! $cm = get_coursemodule_from_id('feedback', $id)) {
		    print_error('invalidcoursemodule');
		}

		if (! $course = $DB->get_record("course", array("id"=>$cm->course))) {
		    print_error('coursemisconf');
		}

		if (! $feedback = $DB->get_record("feedback", array("id"=>$cm->instance))) {
		    print_error('invalidcoursemodule');
		}

		$context = context_module::instance($cm->id);

		//The basic strings used in the fields, fetched according to current language implementation
		$fstring = new stdClass();
		$fstring->questions = get_string('questions', 'feedback');
		$fstring->itemlabel = get_string('item_label', 'feedback');
		$fstring->question = get_string('question', 'feedback');
		$fstring->responses = get_string('responses', 'feedback');
		$fstring->idnumber = get_string('idnumber');//Note: here, ID number returns the internal moodle id of the student
		$fstring->username = get_string('username');
		$fstring->fullname = get_string('fullnameuser');
		$fstring->anonymous_user = get_string('anonymous_user', 'feedback');

		//Fetch items in the feedback, based on feedback id; if empty, throw error
		$params = array('feedback' => $feedback->id, 'hasvalue' => 1);
		if (!$items = $DB->get_records('feedback_item', $params, 'position')) {
		    print_error('no_items_available_yet',
		                'feedback',
		                $CFG->wwwroot.'/mod/feedback/view.php?id='.$id);
		    exit;
		}

		//if groups supported, get the different group ids and fetch results of the feedback
		$mygroupid = groups_get_activity_group($cm);
		$completeds = feedback_get_completeds_group($feedback, $mygroupid, '0');

		$recNum = 0;
		$colNum = 0;

		//Creating the header record, containing only column names
		$records[$recNum][$colNum++] = $fstring->idnumber;
		$records[$recNum][$colNum++] = $fstring->username;
		$records[$recNum][$colNum++] = $fstring->fullname;
		foreach ($items as $item) {
		    $records[$recNum][$colNum++] = $item->name;    
		}

		//Populating responses as records
		if (is_array($completeds)) {
			//For each person who has completed feedback, get details of the person and add in relevant columns, get feedback results of the person and also add in relevant columns
		    foreach ($completeds as $completed) {
		        $colNum = 0;
		        $recNum++;
		        $courseid = 0;

		        $feedback = $DB->get_record('feedback', array('id'=>$completed->feedback));
		        //get the username
		        //anonymous users are separated automatically because the userid in the completed is "0"
		        if ($user = $DB->get_record('user', array('id'=>$completed->userid))) {
		            if ($completed->anonymous_response == FEEDBACK_ANONYMOUS_NO) {
		                $records[$recNum][$colNum++] = $user->id;
		                $userfullname = fullname($user);
		                $records[$recNum][$colNum++] = $user->username;             
		            } 
		            else {
		                $userfullname = $fstring->anonymous_user;
		                $records[$recNum][$colNum++] = '-';//id number doesnt exist as anonymous
		                $records[$recNum][$colNum++] = '-';//username doesnt exist as anonymous
		            }
		        } else {
		            $userfullname = $fstring->anonymous_user;
		            $records[$recNum][$colNum++] = '-';//id number doesnt exist as anonymous
		            $records[$recNum][$colNum++] = '-';//username doesnt exist as anonymous
		        }

		        $records[$recNum][$colNum++] = $userfullname;

		        foreach ($items as $item) {
		            $params = array('item' => $item->id, 'completed' => $completed->id);
		            $value = $DB->get_record('feedback_value', $params);

		            $itemobj = feedback_get_item_class($item->typ);
		            $printval = $itemobj->get_printval($item, $value);
		            $printval = trim($printval);

		            if ($printval != '') {
		                $records[$recNum][$colNum++] = $printval;
		            }
		        }        
		    }
		}

		return $records;
	}	

	public static function get_submissions_parameters() {
	        return new external_function_parameters(
	            array(
	                'assignmentids' => new external_multiple_structure(
	                    new external_value(PARAM_INT, 'assignment id'),
	                    '1 or more assignment ids',
	                    VALUE_REQUIRED),
	                'status' => new external_value(PARAM_ALPHA, 'status', VALUE_DEFAULT, ''),
	                'since' => new external_value(PARAM_INT, 'submitted since', VALUE_DEFAULT, 0),
	                'before' => new external_value(PARAM_INT, 'submitted before', VALUE_DEFAULT, 0)
	            )
	        );
	    }

	//The get_submissions function as defined in Moodle v2.7, with the fileURL parameter value changed from VALUE_OPTIONAL to VALUE_REQUIRED
	public static function get_submissions($assignmentids, $status = '', $since = 0, $before = 0) {
	        global $DB, $CFG;
	        require_once("$CFG->dirroot/mod/assign/locallib.php");
	        $params = self::validate_parameters(self::get_submissions_parameters(),
	                        array('assignmentids' => $assignmentids,
	                              'status' => $status,
	                              'since' => $since,
	                              'before' => $before));

	        $warnings = array();
	        $assignments = array();

	        // Check the user is allowed to get the submissions for the assignments requested.
	        $placeholders = array();
	        list($inorequalsql, $placeholders) = $DB->get_in_or_equal($params['assignmentids'], SQL_PARAMS_NAMED);
	        $sql = "SELECT cm.id, cm.instance FROM {course_modules} cm JOIN {modules} md ON md.id = cm.module ".
	               "WHERE md.name = :modname AND cm.instance ".$inorequalsql;
	        $placeholders['modname'] = 'assign';
	        $cms = $DB->get_records_sql($sql, $placeholders);
	        $assigns = array();
	        foreach ($cms as $cm) {
	            try {
	                $context = context_module::instance($cm->id);
	                self::validate_context($context);
	                require_capability('mod/assign:grade', $context);
	                $assign = new assign($context, null, null);
	                $assigns[] = $assign;
	            } catch (Exception $e) {
	                $warnings[] = array(
	                    'item' => 'assignment',
	                    'itemid' => $cm->instance,
	                    'warningcode' => '1',
	                    'message' => 'No access rights in module context'
	                );
	            }
	        }

	        foreach ($assigns as $assign) {
	            $submissions = array();
	            $submissionplugins = $assign->get_submission_plugins();
	            $placeholders = array('assignid1' => $assign->get_instance()->id,
	                                  'assignid2' => $assign->get_instance()->id);

	            $submissionmaxattempt = 'SELECT mxs.userid, MAX(mxs.attemptnumber) AS maxattempt
	                                     FROM {assign_submission} mxs
	                                     WHERE mxs.assignment = :assignid1 GROUP BY mxs.userid';

	            $sql = "SELECT mas.id, mas.assignment,mas.userid,".
	                   "mas.timecreated,mas.timemodified,mas.status,mas.groupid,mas.attemptnumber ".
	                   "FROM {assign_submission} mas ".
	                   "JOIN ( " . $submissionmaxattempt . " ) smx ON mas.userid = smx.userid ".
	                   "WHERE mas.assignment = :assignid2 AND mas.attemptnumber = smx.maxattempt";

	            if (!empty($params['status'])) {
	                $placeholders['status'] = $params['status'];
	                $sql = $sql." AND mas.status = :status";
	            }
	            if (!empty($params['before'])) {
	                $placeholders['since'] = $params['since'];
	                $placeholders['before'] = $params['before'];
	                $sql = $sql." AND mas.timemodified BETWEEN :since AND :before";
	            } else {
	                $placeholders['since'] = $params['since'];
	                $sql = $sql." AND mas.timemodified >= :since";
	            }

	            $submissionrecords = $DB->get_records_sql($sql, $placeholders);

	            if (!empty($submissionrecords)) {
	                $fs = get_file_storage();
	                foreach ($submissionrecords as $submissionrecord) {
	                    $submission = array(
	                        'id' => $submissionrecord->id,
	                        'userid' => $submissionrecord->userid,
	                        'timecreated' => $submissionrecord->timecreated,
	                        'timemodified' => $submissionrecord->timemodified,
	                        'status' => $submissionrecord->status,
	                        'attemptnumber' => $submissionrecord->attemptnumber,
	                        'groupid' => $submissionrecord->groupid
	                    );
	                    foreach ($submissionplugins as $submissionplugin) {
	                        $plugin = array(
	                            'name' => $submissionplugin->get_name(),
	                            'type' => $submissionplugin->get_type()
	                        );
	                        // Subtype is 'assignsubmission', type is currently 'file' or 'onlinetext'.
	                        $component = $submissionplugin->get_subtype().'_'.$submissionplugin->get_type();

	                        $fileareas = $submissionplugin->get_file_areas();
	                        foreach ($fileareas as $filearea => $name) {
	                            $fileareainfo = array('area' => $filearea);
	                            $files = $fs->get_area_files(
	                                $assign->get_context()->id,
	                                $component,
	                                $filearea,
	                                $submissionrecord->id,
	                                "timemodified",
	                                false
	                            );
	                            foreach ($files as $file) {
	                                $filepath = $file->get_filepath().$file->get_filename();
	                                $fileurl = file_encode_url($CFG->wwwroot . '/webservice/pluginfile.php', '/' . $assign->get_context()->id .
	                                    '/' . $component. '/'. $filearea . '/' . $submissionrecord->id . $filepath);
	                                $fileinfo = array(
	                                    'filepath' => $filepath,
	                                    'fileurl' => $fileurl
	                                    );
	                                $fileareainfo['files'][] = $fileinfo;
	                            }
	                            $plugin['fileareas'][] = $fileareainfo;
	                        }

	                        $editorfields = $submissionplugin->get_editor_fields();
	                        foreach ($editorfields as $name => $description) {
	                            $editorfieldinfo = array(
	                                'name' => $name,
	                                'description' => $description,
	                                'text' => $submissionplugin->get_editor_text($name, $submissionrecord->id),
	                                'format' => $submissionplugin->get_editor_format($name, $submissionrecord->id)
	                            );
	                            $plugin['editorfields'][] = $editorfieldinfo;
	                        }

	                        $submission['plugins'][] = $plugin;
	                    }
	                    $submissions[] = $submission;
	                }
	            } else {
	                $warnings[] = array(
	                    'item' => 'module',
	                    'itemid' => $assign->get_instance()->id,
	                    'warningcode' => '3',
	                    'message' => 'No submissions found'
	                );
	            }

	            $assignments[] = array(
	                'assignmentid' => $assign->get_instance()->id,
	                'submissions' => $submissions
	            );

	        }

	        $result = array(
	            'assignments' => $assignments,
	            'warnings' => $warnings
	        );
	        return $result;
	    }

	    /**
	     * Creates an assign_submissions external_single_structure
	     *
	     * @return external_single_structure
	     * @since Moodle 2.5
	     */
	    private static function get_submissions_structure() {
	        return new external_single_structure(
	            array (
	                'assignmentid' => new external_value(PARAM_INT, 'assignment id'),
	                'submissions' => new external_multiple_structure(
	                    new external_single_structure(
	                        array(
	                            'id' => new external_value(PARAM_INT, 'submission id'),
	                            'userid' => new external_value(PARAM_INT, 'student id'),
	                            'attemptnumber' => new external_value(PARAM_INT, 'attempt number'),
	                            'timecreated' => new external_value(PARAM_INT, 'submission creation time'),
	                            'timemodified' => new external_value(PARAM_INT, 'submission last modified time'),
	                            'status' => new external_value(PARAM_TEXT, 'submission status'),
	                            'groupid' => new external_value(PARAM_INT, 'group id'),
	                            'plugins' => new external_multiple_structure(
	                                new external_single_structure(
	                                    array(
	                                        'type' => new external_value(PARAM_TEXT, 'submission plugin type'),
	                                        'name' => new external_value(PARAM_TEXT, 'submission plugin name'),
	                                        'fileareas' => new external_multiple_structure(
	                                            new external_single_structure(
	                                                array (
	                                                    'area' => new external_value (PARAM_TEXT, 'file area'),
	                                                    'files' => new external_multiple_structure(
	                                                        new external_single_structure(
	                                                            array (
	                                                                'filepath' => new external_value (PARAM_TEXT, 'file path'),
	                                                                'fileurl' => new external_value (PARAM_URL, 'file download url')
	                                                            )
	                                                        ), 'files', VALUE_OPTIONAL
	                                                    )
	                                                )
	                                            ), 'fileareas', VALUE_OPTIONAL
	                                        ),
	                                        'editorfields' => new external_multiple_structure(
	                                            new external_single_structure(
	                                                array(
	                                                    'name' => new external_value(PARAM_TEXT, 'field name'),
	                                                    'description' => new external_value(PARAM_TEXT, 'field description'),
	                                                    'text' => new external_value (PARAM_RAW, 'field value'),
	                                                    'format' => new external_format_value ('text')
	                                                )
	                                            )
	                                            , 'editorfields', VALUE_OPTIONAL
	                                        )
	                                    )
	                                )
	                                , 'plugins', VALUE_OPTIONAL
	                            )
	                        )
	                    )
	                )
	            )
	        );
	    }

	    /**
	     * Describes the get_submissions return value
	     *
	     * @return external_single_structure
	     * @since Moodle 2.5
	     */
    public static function get_submissions_returns() {
        return new external_single_structure(
            array(
                'assignments' => new external_multiple_structure(self::get_submissions_structure(), 'assignment submissions'),
                'warnings' => new external_warnings()
            )
        );
    }

	public static function get_gradepublishkey_parameters()
	{
		return new external_function_parameters(
				array(
					'courseid' => new external_value(PARAM_INT, 'course id for which private key is required'),
					'userid' => new external_value(PARAM_INT, 'userid of the user who is being authorized to use the key'),
					'iprestriction' => new external_value(PARAM_TEXT, 'IPs which are allowed to use the key', VALUE_DEFAULT, null),
					'validuntil' => new external_value(PARAM_INT, 'The unix time stamp of the date until when the given key is valid', VALUE_DEFAULT, null),
				)
			);
	}

	public static function get_gradepublishkey_returns()
	{
		return new external_single_structure(array(
			'key' => new external_value(PARAM_TEXT, 'private key to be used for grade publishing'),
			)
		);
	}

	public static function get_gradepublishkey($courseid, $userid, $iprestriction, $validuntil)
	{
		$keyString['key'] = get_user_key('grade/export', $userid, $courseid, $iprestriction, $validuntil);
		return $keyString;
	}

	public static function get_results_parameters()
	{
		return new external_function_parameters(
				array(
					'id' => new external_value(PARAM_INT, 'activity id of the survey'),
			));
	}

	public static function get_results_returns()
	{
		return new external_multiple_structure(
			new external_multiple_structure(
					new external_value(PARAM_TEXT, 'Field of the survey')
				)			
		);
	}

	public static function get_results($id)
	{
	    global $DB;

	    if (! $cm = get_coursemodule_from_id('survey', $id)) {
	        print_error('invalidcoursemodule');
	    }

	    if (! $course = $DB->get_record("course", array("id"=>$cm->course))) {
	        print_error('coursemisconf');
	    }

	    $context = context_module::instance($cm->id);

	    if (! $survey = $DB->get_record("survey", array("id"=>$cm->instance))) {
	        print_error('invalidsurveyid', 'survey');
	    }

	    // Check to see if groups are being used in this survey
	    $groupmode = groups_get_activity_groupmode($cm);   // Groups are being used

	    if ($groupmode and $group) {
	        $users = get_users_by_capability($context, 'mod/survey:participate', '', '', '', '', $group, null, false);
	    } else {
	        $users = get_users_by_capability($context, 'mod/survey:participate', '', '', '', '', '', null, false);
	        $group = false;
	    }

	    // The order of the questions
	    $order = explode(",", $survey->questions);

	    // Get the actual questions from the database
	    $questions = $DB->get_records_list("survey_questions", "id", $order);

	    // Get an ordered array of questions
	    $orderedquestions = array();

	    $virtualscales = false;
	    foreach ($order as $qid) {
	        $orderedquestions[$qid] = $questions[$qid];
	        // Check if this question is using virtual scales
	        if (!$virtualscales && $questions[$qid]->type < 0) {
	            $virtualscales = true;
	        }
	    }
	    $nestedorder = array();//will contain the subquestions attached to the main questions
	    $preparray = array();

	    foreach ($orderedquestions as $qid=>$question) {
	        if (!empty($question->multi)) {
	            $actualqids = explode(",", $questions[$qid]->multi);
	            foreach ($actualqids as $subqid) {
	                if (!empty($orderedquestions[$subqid]->type)) {
	                    $orderedquestions[$subqid]->type = $questions[$qid]->type;
	                }
	            }
	        } else {
	            $actualqids = array($qid);
	        }
	        if ($virtualscales && $questions[$qid]->type < 0) {
	            $nestedorder[$qid] = $actualqids;
	        } else if (!$virtualscales && $question->type >= 0) {
	            $nestedorder[$qid] = $actualqids;
	        } else {
	            //todo andrew this was added by me. Is it correct?
	            $nestedorder[$qid] = array();
	        }
	    }

	    $reversednestedorder = array();
	    foreach ($nestedorder as $qid=>$subqidarray) {
	        foreach ($subqidarray as $subqui) {
	            $reversednestedorder[$subqui] = $qid;
	        }
	    }

	    //need to get info on the sub-questions from the db and merge the arrays of questions
	    $allquestions = array_merge($questions, $DB->get_records_list("survey_questions", "id", array_keys($reversednestedorder)));

	    //array_merge() messes up the keys so reinstate them
	    $questions = array();
	    foreach($allquestions as $question) {
	        $questions[$question->id] = $question;

	        //while were iterating over the questions get the question text
	        $questions[$question->id]->text = get_string($questions[$question->id]->text, "survey");
	    }
	    unset($allquestions);

	    // Get and collate all the results in one big array
	    if (! $surveyanswers = $DB->get_records("survey_answers", array("survey"=>$survey->id), "time ASC")) {
	        print_error('cannotfindanswer', 'survey');
	    }

	    $results = array();

	    foreach ($surveyanswers as $surveyanswer) {
	        if (!$group || isset($users[$surveyanswer->userid])) {
	            $questionid = $surveyanswer->question;
	            if (!array_key_exists($surveyanswer->userid, $results)) {
	                $results[$surveyanswer->userid] = array('time'=>$surveyanswer->time);
	            }
	            $results[$surveyanswer->userid][$questionid]['answer1'] = $surveyanswer->answer1;
	            $results[$surveyanswer->userid][$questionid]['answer2'] = $surveyanswer->answer2;
	        }
	    }
	
	$records = array();

    $records[0][0] = "surveyid";
    $records[0][1] = "surveyname";
    $records[0][2] = "userid";
    $records[0][3] = "firstname";
    $records[0][4] = "lastname";
    $records[0][5] = "email";
    $records[0][6] = "idnumber";
    $records[0][7] = "time";

    $i = 8;

    foreach ($nestedorder as $key => $nestedquestions) {
        foreach ($nestedquestions as $key2 => $qid) 
        {
            $question = $questions[$qid];
            if ($question->type == "0" || $question->type == "1" || $question->type == "3" || $question->type == "-1")  {
                
                $records[0][$i] = $question->text;
                $i++;
                
            }
            if ($question->type == "2" || $question->type == "3")  {
                $records[0][$i] = $question->text + " (preferred)";
                $i++;
            }        
        }
    }

    $i = 1;
    foreach ($results as $user => $rest) {
        if (! $u = $DB->get_record("user", array("id"=>$user))) {
            print_error('invaliduserid');
        }
 
        $j = 8;
 
        $records[$i][0] = $survey->id;
        $records[$i][1] = strip_tags(format_string($survey->name,true));
        $records[$i][2] = $user;
        $records[$i][3] = $u->firstname;
        $records[$i][4] = $u->lastname;
        $records[$i][5] = $u->email;
        $records[$i][6] = $u->idnumber;
        $records[$i][7] = userdate($results[$user]["time"], "%d-%b-%Y %I:%M:%S %p");

        foreach ($nestedorder as $key => $nestedquestions) {
            foreach ($nestedquestions as $key2 => $qid) {
                $question = $questions[$qid];

                if ($question->type == "0" || $question->type == "1" || $question->type == "3" || $question->type == "-1")  {
                    $records[$i][$j] = $results[$user][$qid]["answer1"];
                    $j++;
                }
                if ($question->type == "2" || $question->type == "3")  {
                    $records[$i][$j] = $results[$user][$qid]["answer2"];
                    $j++;
                }
            }
        }
        
       $i++;
    }
    	return $records;
	}

}