<?php

$functions = array(
	'local_surveyint_get_results' => array(
			'classname' => 'local_surveyint_external',
			'methodname' => 'get_results',
			'classpath' => 'local/surveyint/externallib.php',
			'description' => 'Gets survey results',
			'type' => 'read',
		),
	'local_surveyint_get_gradepublishkey' => array(
			'classname' => 'local_surveyint_external',
			'methodname' => 'get_gradepublishkey',
			'classpath' => 'local/surveyint/externallib.php',
			'description' => 'Gets the private session key required for grade publishing privileges',
			'type' => 'read',
		),
	'local_surveyint_get_submissions' => array(
                'classname' => 'local_surveyint_external',
                'methodname' => 'get_submissions',
                'classpath' => 'local/surveyint/externallib.php',
                'description' => 'Returns the submissions for assignments',
                'type' => 'read'
        ),
	'local_surveyint_get_feedbackresults' => array(
                'classname' => 'local_surveyint_external',
                'methodname' => 'get_feedbackresults',
                'classpath' => 'local/surveyint/externallib.php',
                'description' => 'Gets feedback results',
                'type' => 'read'
        ),
	);

