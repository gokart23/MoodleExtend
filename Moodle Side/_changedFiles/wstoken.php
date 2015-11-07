<?php

require_once("../../config.php");
require_once("$CFG->dirroot/webservice/lib.php");

$username = required_param('username', PARAM_USERNAME);
$password = required_param('password', PARAM_RAW);
$serviceid = required_param('serviceid', PARAM_INT);   
$username = trim(textlib::strtolower($username));
$user = authenticate_user_login($username, $password);
if($user !== false)
{

	if ($user->id == $CFG->siteguest or $user->deleted or !$user->confirmed or $user->suspended) {
        throw new moodle_exception('forbiddenwsuser', 'webservice');
    }

	$webservicemanager = new webservice();
	
	$selectedservice = $webservicemanager->get_external_service_by_id($serviceid);
    if ($selectedservice->restrictedusers) {
        if ($webservicemanager->get_ws_authorised_user($serviceid, $user->id)->id != $user->id) {
            throw new moodle_exception('forbiddenwsuser', 'moodle');
        }
    }

	$tokens = $webservicemanager->get_user_ws_tokens($user->id);
	if(count($tokens) == 0)
	{
		$webservicemanager = new webservice();
		external_generate_token(EXTERNAL_TOKEN_PERMANENT, $serviceid, $user->id, context_system::instance(), 0, '');
		$tokens = $webservicemanager->get_user_ws_tokens($user->id);
	}

	echo current($tokens)->token;
}
else
{
	throw new moodle_exception('usernamenotfound', 'moodle');
}