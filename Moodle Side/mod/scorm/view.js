M.mod_scormform = {};
M.mod_scormform.init = function(Y) {
    var scormform = Y.one('#scormviewform');
    var cwidth = scormplayerdata.cwidth;
    var cheight = scormplayerdata.cheight;
    var poptions = scormplayerdata.popupoptions;
    var launch = scormplayerdata.launch;
    var currentorg = scormplayerdata.currentorg;
    var sco = scormplayerdata.sco;
    var scorm = scormplayerdata.scorm;
    var launch_url = M.cfg.wwwroot+"/mod/scorm/player.php?a="+scorm+"&currentorg="+currentorg+"&scoid="+sco+"&sesskey="+M.cfg.sesskey+"&display=popup";
    var course_url = scormplayerdata.courseurl;
    var winobj = null;

    poptions = poptions + ',resizable=yes'; // Added for IE (MDL-32506).

    if ((cwidth==100) && (cheight==100)) {
        poptions = poptions+',width='+screen.availWidth+',height='+screen.availHeight+',left=0,top=0';
    } else {
        if (cwidth<=100) {
            cwidth = Math.round(screen.availWidth * cwidth / 100);
        }
        if (cheight<=100) {
            cheight = Math.round(screen.availHeight * cheight / 100);
        }
        poptions = poptions+',width='+cwidth+',height='+cheight;
    }

    // Hide the form and toc if it exists - we don't want to allow multiple submissions when a window is open.
    var scormload = function () {
        if (scormform) {
            scormform.hide();
        }

        var scormtoc = Y.one('#toc');
        if (scormtoc) {
            scormtoc.hide();
        }
        // Hide the intro and display a message to the user if the window is closed.
        var scormintro = Y.one('#intro');
        scormintro.setHTML('<a href="'+ course_url + '">' + M.str.scorm.popuplaunched + '</a>');
    }

    // When pop-up is closed return to course homepage.
    var scormunload = function () {
        // Onunload is called multiple times in the SCORM window - we only want to handle when it is actually closed.
        setTimeout(function() {
            if (winobj.closed) {
                // Redirect the parent window to the course homepage.
                parent.window.location = course_url;
            }
        }, 800)
    }

    var scormredirect = function (winobj) {
        Y.on('load', scormload, winobj);
        Y.on('unload', scormunload, winobj);
        // Check to make sure pop-up has been launched - if not display a warning,
        // this shouldn't happen as the pop-up here is launched on user action but good to make sure.
        setTimeout(function() {
            if (!winobj) {
                var scormintro = Y.one('#intro');
                scormintro.setHTML(M.str.scorm.popupsblocked);
            }}, 800);
    }

    if (launch == true) {
        winobj = window.open(launch_url,'Popup', poptions);
        this.target='Popup';
        scormredirect(winobj);
    }
    // Listen for view form submit and generate popup on user interaction.
    if (scormform) {
        Y.on('submit', function(e) {
            winobj = window.open(launch_url, 'Popup', poptions);
            this.target='Popup';
            scormredirect(winobj);
            e.preventDefault();
        }, scormform);
    }
}
