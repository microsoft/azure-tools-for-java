"use strict";

// to cache all job related object
var spark = {};

$(function () {
    initiate();
    commandBinding();
    getBasicInfoFromUrl();
    getJobHistory();
});

function initiate() {
    // hide the error messagae tab first
    // $('#myTab li:eq(0)').hide();
    // show the job output tab
    var myTable = $('#myTable');
    // myTable.colResizable will cause the coulumn width calculation error.
    // Some column width are calculated as 0% which leads to display error.
    // So we comment this line to fix the display error. Please see detailed bugs as
    // below.
    // Bug1: https://dev.azure.com/mseng/VSJava/_workitems/edit/1870900
    // Bug2: https://dev.azure.com/mseng/VSJava/_workitems/edit/1870841
    // myTable.colResizable({liveDrag:true});
    myTable.dragtable();

    var leftDiv = $('#leftDiv');
    var rightDiv = $('#rightDiv');
    leftDiv.resizable();

    myTable.find('li:eq(4) a').tab('show');

    var tableDiv =  $('#tableDIv');
    tableDiv.resizable();
    tableDiv.resize(function(){
        rightDiv.width($('#parent').width()-$('#tableDIv').width());
    });

    if (!String.prototype.format) {
        String.prototype.format = function() {
            var args = arguments;
            return this.replace(/{(\d+)}/g, function(match, number) {
                return typeof args[number] !== 'undefined'
                    ? args[number]
                    : match
                    ;
            });
        };
    }
}

function isEnterKeyPressedEvent(event) {
    var keycode = (event.keyCode ? event.keyCode : event.which);
    return keycode === 13
}

function onApplicationListRowSelected(event) {
    if (event.type === "click" || isEnterKeyPressedEvent(event)) {
        // clean all generated values
        $("#openSparkUIButton").css("visibility","visible");
        spark.isJobGraphGenerated = false;
        spark.currentSelectedJobs = null;
        spark.currentSelectedStages = null;
        spark.appId = null;
        spark.attemptId = null;
        spark.applicationName = null;

        $('#summaryTitle').html("Application details");
        $('#basicInformationTitle').html("Basic Application Information");
        d3.selectAll("#stageSummaryTbody tr").remove();
        d3.selectAll("#taskSummaryTbody tr").remove();
        $('#errorMessage').text("");
        $('#jobOutputTextarea').text("");
        $('#livyJobLog').text("");
        $('#sparkDriverLog').text("");
        var rows = $('#JobHistoryTbody').find('tr');
        rows.removeClass('selected-hight');
        $(this).addClass('selected-hight');


        //get Application Id
        spark.appId = $(this).find('td:eq(1)').text();
        spark.selectedApp = spark.applicationList.filter(function (item) {
            return item.id === spark.appId;
        })[0];
        // get last attempt
        spark.attemptId = $(this).find('td:eq(4)').text();
        spark.applicationName = $(this).find('td:eq(2)').text();
        $('#jobName').text("Application: " + spark.applicationName);

        if (spark.appId === 'undefined') {
            return;
        }
        // save current Application ID to LocalStorage
        localStorage.setItem('selectedAppID', spark.appId);

        renderApplicationGraph();
        renderStageDetails();
        renderExecutors();
        renderTaskDetails();
        renderYarnLogs();
        // setBasicInformation();
        // setAMcontainer();
        // setDiagnosticsLog();
        // setLivyLog();
        // setDebugInfo("end livy log");
        // setJobDetail();
        // setStoredRDD();
    }
}

function commandBinding() {
    $('#JobHistoryTbody').on('click', 'tr', function (event) {
        onApplicationListRowSelected.apply(this, [event]);
    });

    $('#JobHistoryTbody').on('keypress', 'tr', function (event) {
        onApplicationListRowSelected.apply(this, [event]);
    });

    $('#sparkEventButton').click(function () {
        sendActionSingle("/actions/sparkEvent");
    });

    $('#livyLogButton').click(function() {
        sendActionSingle("/actions/livyLog");
    });

    $("#openSparkUIButton").click(function () {
        sendActionSingle("/actions/sparkui");
    });

    $("#openYarnUIButton").click(function () {
        sendActionSingle("/actions/yarnui");
    });

    // $("#refreshButton").click(function () {
    //     location.reload();
    //     refreshGetSelectedApplication();
    // });

    $('#jobGraphBackButton').click(function() {
        $('#applicationGraphDiv').removeClass('graph-disabled');
        renderJobGraphOnApplicationLevel(spark.currentSelectedJobs);
        $('#jobGraphDiv').addClass('graph-disabled');
    });
}

function getBasicInfoFromUrl() {

    spark.queriresMap = {};
    var urlinfo = window.location.href;
    var len = urlinfo.length;
    var offset = urlinfo.indexOf("?");

    var additionalInfo = urlinfo.substr(offset + 1, len);
    var infos = additionalInfo.split("&");
    for (var i = 0; i < infos.length; ++i) {
        var strs = infos[i].split("=");
        spark.queriresMap[strs[0]] = strs[1];
    }

    spark.sourceType = spark.queriresMap['sourcetype'] === undefined ? "intellij" : "eclipse";
    spark.clusterName = spark.queriresMap['clusterName'];
    spark.queryPort = spark.queriresMap['port'];
    spark.localhost = 'http://localhost:{0}'.format(spark.queryPort);

    // send empty request so we can get first response with access header quickly
    getMessageAsync('/try', 'spark', null, null);
}

function getJobHistory() {
    getMessageAsync("/applications/", 'spark', function (s) {
        writeToTable(s);
        initilizeGrid();
        refreshGetSelectedApplication();
    });
}

function refreshGetSelectedApplication() {
    var selectedAppId = localStorage.getItem("selectedAppID");
    if (!selectedAppId) {
        // try to click the first application
        $('#JobHistoryTbody').find('tr:eq(0)').click();
        return;
    }

    var tableRow = $('#myTable tbody tr').filter(function () {
        return $(this).children('td:eq(1)').text() === selectedAppId;
    }).closest("tr");
    tableRow.click();
}

function setMessageForLable(str) {
    var ss = document.getElementById("demo");
    ss.innerHTML = str;
}

function writeToTable(message) {
    spark.applicationList = JSON.parse(message);
    $('#myTable tbody').html("");
    d3.select("#myTable tbody")
        .selectAll('tr')
        .data(spark.applicationList)
        .enter()
        .append('tr')
        .attr("align","center")
        .attr("class","ui-widget-content")
        .selectAll('td')
        .data(function(d) {
            return appInformationList(d);
        })
        .enter()
        .append('td')
        .attr('class',"ui-widget-content")
        .attr('tabindex',"-1")
        .attr('id',function(d,i) {
            return i;
        })
        .html(function(d, i) {
            return d;
        });
}

function initilizeGrid() {
    var leftDiv = document.getElementById('leftDiv');
    var leftDivGrid = new aria.Grid(leftDiv.querySelector('[role="grid"]'));
    leftDivGrid.focusCell(0, 0)
}

function appInformationList(app) {
    var lists = [];
    var status = app.attempts[app.attempts.length - 1].completed;
    lists.push(getTheJobStatusImgLabel(status));
    lists.push(app.id);
    lists.push(app.name);
    lists.push(formatServerTime(app.attempts[0].startTime));
    if(app.attempts.length === 1 && typeof app.attempts[0].attemptId === 'undefined') {
        lists.push(0);
    } else {
        lists.push(app.attempts.length);
    }
    lists.push(app.attempts[0].sparkUser);
    return lists;
}

function getTheJobStatusImgLabel(str) {
    if (str === 'true') {
        return "<img src=\"resources/icons/Success.png\">";
    } else {
        return "<img src=\"resources/icons/Error.png\">";
    }
}

function setAMcontainer() {
    // filter out local application
    if (spark.appId.substr(0, 5) === "local") {
        $("#containerNumber").text("Local Task");
    } else {
        getMessageAsync('/applications', 'spark', function (str) {
            var myAttempts = JSON.parse(str);
            spark.containerId = myAttempts.appAttempts.appAttempt[0].containerId;
            spark.nodeId = myAttempts.appAttempts.appAttempt[0].nodeId;
            $("#containerNumber").text(spark.containerId);
            if (spark.appId.substr(0, 5) !== "local" && spark.attemptId !== 0) {
                getJobResult();
                getSparkDriverLog();
            }
        }, spark.appId);
    }
}

function setDiagnosticsLog() {
    if (spark.appId.substr(0, 5) === "local") {
        $("#errorMessage").text("No Yarn Error Message");
    } else {
        getMessageAsync('/apps', 'yarn', function (s) {
            var responseObject = JSON.parse(s);
            var message = responseObject.app.diagnostics;
            if (message === 'undefined' || message === "") {
                message = "No Error Message";
            }
            $('#errorMessage').text(message);
        }, spark.appId);
    }
}

function getSparkDriverLog() {
    if (spark.attemptId === 0 || !spark.containerId) {
        return;
    }
    getMessageAsync('/applications/driverLog', 'yarn', function (s) {
        var executorsObject = JSON.parse(s);
        // var hostPort = getDriverPortFromExecutor(executorsObject);
        // var ipAddress = hostPort.split(":")[0];
        // var url = localhost + projectId + "/jobhistory/logs/" + ipAddress + "/port/30050/" + containerId + "/" + containerId + "/livy/stderr?restType=yarnhistory";
        // getResultFromSparkHistory(url, function (result) {
        //     $("#sparkDriverLog").text(result);
        // });
    }, spark.appId);
}

function getJobResult() {
    // there's no attemptId for non-spark job
    if (spark.attemptId === 0 || typeof spark.containerId === 'undefined') {
        return;
    }

    getMessageAsync("/yarnui/jobresult?appId" + spark.appId, 'yarnhistory', function (s) {
        // var executorsObject = JSON.parse(s);
        // var hostPort = getDriverPortFromExecutor(executorsObject);
        // var ipAddress = hostPort.split(":")[0];
        // var url = localhost + projectId + "/jobhistory/logs/" + ipAddress + "/port/30050/" + containerId + "/" + containerId + "/livy/stdout?restType=yarnhistory";
        // getResultFromSparkHistory(url, function (result) {
        //     if (result == "") {
        //         result = "No out put";
        //     }
        //     $("#jobOutputTextarea").text(result);
        // });
    });
}

function renderApplicationGraph() {
    getMessageAsync('/applications/application_graph', 'spark', function (s) {
        var yarnAppWithJobs = JSON.parse(s);
        spark.selectedYarnApp = yarnAppWithJobs.app;

        spark.currentSelectedJobs =
            yarnAppWithJobs.jobs.sort(function(left, right) {
                return left['jobId'] > right['jobId'];
            });

        spark.jobStartEvents = yarnAppWithJobs.startEventLogs.sort(function(left, right) {
            return left['Job ID'] > right['Job ID'];
        });

        initJobViewContext();
        renderJobGraphOnApplicationLevel(spark.currentSelectedJobs);
    }, spark.appId);
}

function renderStageDetails() {
    if(spark.attemptId === 0) {
        $("#stage_detail_info_message").text("No Stage Info");
        return;
    }
    $('#stage_detail_info_message').text('');
    getMessageAsync('/applications/stages_summary', 'spark', function (s) {
        spark.currentSelectedStages = JSON.parse(s);
        renderStageSummary(spark.currentSelectedStages);
    }, spark.appId);
}

function renderTaskDetails() {
    getMessageAsync('/applications/tasks_summary','spark', function(s){
        var tasks = JSON.parse(s);
        renderTaskSummary(tasks);
    }, spark.appId);
}

function renderExecutors() {
    getMessageAsync('/applications/executors_summary', 'spark', function (s) {
        var executors = JSON.parse(s);
        renderExecutorsOnPage(executors);
    }, spark.appId);
}

function renderYarnLogs() {
    getMessageAsync('/apps/logs', 'yarn', function (s) {
        spark.logs = JSON.parse(s);
        $('#driverErrorTextArea').text(spark.logs.stderr);
        $('#jobOutputTextArea').text(spark.logs.stdout);
        $('#directoryInfoTextArea').text(spark.logs.directoryInfo);
    }, spark.appId);
}

function setJobGraph(jobs) {
    spark.isJobGraphGenerated = true;
    d3.select("#job-graph-menu")
        .selectAll('li')
        .data(jobs)
        .enter()
        .append('li')
        .attr("role","presentation")
        .append("a")
        .attr("role","menuitem")
        .attr("tabindex", -1)
        .text(function(job) {
            return "Job " + job['jobId'];
        }).on('click', function(job, i) {
        setJobGraphForOneJob(job);
    });
}

function setJobGraphForOneJob(job) {
    var stageIds = job['stageIds'];
    var selectedStages = [];
    stageIds.forEach(function(stageId) {
       selectedStages.push(spark.currentSelectedStages.find(function(d) {
           return d['stageId'] === stageId;
       }));
    });
    renderJobGraph(selectedStages);
}

function stagesInfo(jobs, url) {
        getMessageAsync("/applications/stages?appId=" + spark.appId, 'spark', function (s) {
            var data = new Object();
            var stages = JSON.parse(s);
            data.jobs = jobs;
            data.stages = stages;
            data.stageDetails = [];
            data.jobs.stageIds.forEach(function(stageNumber) {
                getMessageAsync("/applications/stages?appId=", function(s) {
                    var detail = JSON.parse(s);
                });
            });
        });
}

///applications/[app-id]/storage/rdd
function setStoredRDD() {
    if(spark.attemptId === 0) {
        renderStoredRDD('');
        return;
    }
    getMessageAsync("/applications/storage?appId=" + spark.appId, 'spark', function(s) {
        var rdds = JSON.parse(s);
        renderStoredRDD(rdds);
    });
}

function filterTaskSummaryTable() {
    var input, filter, table, tr, td, i;
    filter = $("#filterTableInput").val().toLowerCase();
    tr = $("#taskSummaryTable tbody tr");
    tr.each(function () {
        var text = $(this).html().toLowerCase();
        if(text.indexOf(filter) > -1) {
            $(this).css("display","");
        } else {
            $(this).css("display","none");
        }
    });
}

function filterStageTaskTableWithStageIds(stageIds) {
    var tr = $("#stageSummaryTable tbody tr");
    tr.each(function (i) {
        var id = $("#stageSummaryTbody>tr>td:nth-child(2)")[i].innerHTML;
        if( $.inArray( parseInt(id), stageIds) > -1 ) {
            $(this).css("display","");
            filterTaskTableWithStageId(id);
        } else {
            $(this).css("display","none");
        }
    });
}

function filterTaskTableWithStageId(stageId) {
    var httpQuery = localhost + projectId + "/applications/" + appId + "/" + attemptId + "/stages/" + stageId;
    getMessageAsync(httpQuery, function (s) {
        var stageDetails = JSON.parse(s);
        if(getJsonLength(stageDetails)) {
            var filteredTaskIds = Object.keys(stageDetails[0].tasks);
            filterTaskTableWithTaskIds(filteredTaskIds);
        }
    })
}
function filterTaskTableWithTaskIds(taskIds) {
    var tr = $("#taskSummaryTable tbody tr");
    tr.each(function (i) {
        var id = $("#taskSummaryTbody>tr>td:nth-child(1)")[i].innerHTML;
        if( $.inArray( id, taskIds) > -1 ) {
            $(this).css("display","");
        } else {
            $(this).css("display","none");
        }
    });
}
function getJsonLength(jsonObject) {
    var length = 0;
    for(var item in jsonObject) {
        length++;
    }
    return length;
}