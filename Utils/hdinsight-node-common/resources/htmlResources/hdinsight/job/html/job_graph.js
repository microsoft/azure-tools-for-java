document.jobGraphView = {
    minX: -10,
    width: 480,
    height: 480,
    tipsWidth: 200
}

var jobGraphFocusStack = [];
var jobGraphUpDownFocusStacks = {};

function initJobViewContext() {
    jobGraphFocusStack = [];
    jobGraphUpDownFocusStacks = {
        appView: {
            up: [],
            down: []
        },
        jobView: {
            up: [],
            down: []
        }
    };
}

function renderJobGraphOnApplicationLevel(jobs) {
    $('#applicationGraphDiv').removeClass('graph-disabled');
    $('#jobGraphDiv').addClass('graph-disabled');
    var g = new dagreD3.graphlib.Graph()
        .setGraph({})
        .setDefaultEdgeLabel(function() { return {}; });

    var counters = jobs.length, i = 0;
    g.setNode(0, {label :"Driver",   class : "type-TOP"});
    for(i = 1; i <= counters; ++i) {
        var s = "Job " + (i - 1);
        var currentClass = jobs[i - 1]["status"] === "SUCCEEDED" ? "sparkJob-success" : "sparkJob-error";
        g.setNode(i, {label: s,  class : currentClass});
    }

    for(i = 1; i <= counters; ++i) {
        g.setEdge(0, i);
    }

    // Create the renderer
    var render = new dagreD3.render();

// Set up an SVG group so that we can translate the final graph.
    var svg = d3.select("#applicationGraphSvg");

    // remove all graph first
    d3.selectAll("#applicationGraphSvg g").remove();

    var inner = svg.append("g");

// Run the renderer. This is what draws the final graph.
    render(d3.select("#applicationGraphSvg g"), g);

    document.jobGraphView.width = $('#applicationGraphDiv').width() * 0.7;
    var viewBoxValue = calculateVirtualBox(g.graph())
    svg.attr("viewBox", viewBoxValue);
    svg.attr("preserveAspectRatio", "xMidYMid meet");

    render(d3.select("#applicationGraphSvg g"), g);
    // Center the graph
    var applicationSvg = $('#applicationGraphSvg');
    if (!spark.graphsize) {
        spark.graphsize = {
            'width': applicationSvg.width(),
            'height': applicationSvg.height()
        };
    } else {
        applicationSvg.width(spark.graphsize.width);
        applicationSvg.height(spark.graphsize.height);
    }

    var zoom = d3.behavior.zoom().on("zoom", function() {
        inner.attr("transform", "translate(" + d3.event.translate + ")" +
            "scale(" + d3.event.scale + ")");
    });
    svg.call(zoom);
    svg.attr("style", "width:100%");
    // Simple function to style the tooltip for the given node.
    inner.selectAll("g.node")
        .attr('tabindex', "-1")
        .attr("title", function(v) {
            return setToolTips(jobs, v)
        })
        .each(function(v) {
            if (v === '0') {
                $(this).attr('tabindex', "0")
            }

            $(this).tipsy({
                gravity: "w",
                opacity: 1,
                trigger:'hover',
                html: true });
            $(this).tipsy({
                gravity: "w",
                opacity: 1,
                trigger:'focus',
                html: true });
        })
        .on('keydown', function(d) {
            var upDownFocusStack = jobGraphUpDownFocusStacks.appView;

            switch (d3.event.code) {
                case "Enter":
                    if (d == '0') {
                        return;
                    }

                    renderJobGraphForSelectedJob(d);
                    break;
                default:
                    moveFocusByArrows(d3.event.code, g, d, upDownFocusStack);
            }

           zoomGraph(inner);
        })
        .on('click',function(d) {
            if ( d == '0') {
                return;
            }
            renderJobGraphForSelectedJob(d);
        });

    var lastFocusNodeIdx = jobGraphFocusStack.pop();
    if (lastFocusNodeIdx) {
        focusOnNode(lastFocusNodeIdx)
    }
}

function moveFocusByArrows(arrowCode, g, current, upDownFocusStack) {
    switch (arrowCode) {
        case "ArrowUp":
            var parent = findParent(g, upDownFocusStack.down, current);
            focusOnNode(parent)

            if (current != parent) {
                upDownFocusStack.up.push(current);
            }

            break;
        case "ArrowDown":
            var child = findChild(g, upDownFocusStack.up, current);
            focusOnNode(child)

            if (current != child) {
                upDownFocusStack.down.push(current);
            }

            break;
        case "ArrowLeft":
            var buddies = findBuddy(g, current)
            focusOnNode(buddies.left)

            break;
        case "ArrowRight":
            var buddies = findBuddy(g, current)
            focusOnNode(buddies.right)

            break;
        default:
    }

}

function focusOnNode(index) {
    var nodes = d3.selectAll("#applicationGraphSvg g.node");
    var lastFocus = document.activeElement
    
    nodes.attr('tabindex', "-1");
    var toFocus = nodes.filter((datum, i) =>
        datum == index)
    .attr('tabindex', "0")
    .node()
    .focus();
}

function findParent(g, stack, current) {
    var last = stack.pop();

    if (!last) {
       var firstParent = g.edges().find(edge => edge.w == current);

       if (!firstParent || firstParent.w != current) {
           return 0;
       }

       return firstParent.v;
    }

    return last;
}

function findChild(g, stack, current) {
    var last = stack.pop();

    if (!last) {
       var firstChild = g.edges().find(edge => edge.v == current);

       if (!firstChild || firstChild.v != current) {
           return current;
       }

       return firstChild.w;
    }

    return last;
}

function findBuddy(g, current) {
    var parents = g.edges().filter(edge => edge.w === current).map(edge => edge.v);

    var buddies = g.edges().filter(edge => parents.indexOf(edge.v) >= 0);

    var left = right = current;
    var i;
    for(i = 0; i < buddies.length; i++) {
        if (buddies[i].w == current) {
            break;
        }

        left = buddies[i].w;
    }
    
    if (i < buddies.length - 1) {
        right = buddies[i + 1].w;
    }

    return { left: left, right: right }
}

function renderJobGraphForSelectedJob(d) {
    var job = spark.jobStartEvents[d - 1];
    jobGraphFocusStack.push(d);

    renderJobGraph(job);
}

function setToolTips(jobs, v) {
    if(v !== "0") {
        var counter = parseInt(v) - 1;
        var job = jobs[counter];
        return getFormattedTipsForJob(job);
    } else {
        // driver
        return getFormattedTipsForDriver();
    }
}

function getFormattedTipsForDriver() {
    var containerLogs = spark.selectedYarnApp.amContainerLogs;
    var paths = containerLogs.split('/');
    var amContainer = paths[paths.length - 2];

    return "<p class='name'>Application Details:</p>"
            + "<p class='description jobtips' align='left'>AM Container: {0}<br>".format(amContainer)
            + "<hr class='jobview-hr'/>"
            + "Start time: {0}<br>End Time: {1} <br>Duration(mins): {2}<br> Memory Seconds: {3}<br>Core Seconds: {4}".format( formatServerTime(spark.selectedApp.startTime),
                formatServerTime(spark.selectedApp.endTime),
                ((spark.selectedYarnApp.finishedTime - spark.selectedYarnApp.startedTime)/(1000 * 60)).toFixed(2),
                spark.selectedYarnApp.memorySeconds,
                spark.selectedYarnApp.vcoreSeconds)
            + "<hr class='jobview-hr'/>"
            + "</p>";
}

function getFormattedTipsForJob(job) {
    var timeDuration = getTimeIntervalByMins(job.completionTime, job.submissionTime);
    return "<p class='name jobtips' align='left'>Job ID: {0}<br> {1}</p>".format(job.jobId , job.name) +
        "<p class='description jobtips' align='left'>Time Duration(Mins): {0}<br>".format(timeDuration) +
        "Completed Tasks: {0} &nbsp;&nbsp;&nbsp;Failed Tasks: {1}<br>".format(job.numTasks, job.numFailedTasks) +
        "Completed Stages: {0} &nbsp;&nbsp;&nbsp;Failed Stages:{1}</p>".format(job.numCompletedStages, job.numFailedStages);
}

function renderJobGraph(job) {
    $('#applicationGraphDiv').addClass('graph-disabled');
    $('#jobGraphDiv').removeClass('graph-disabled');
    var g = new dagreD3.graphlib.Graph()
        .setGraph({})
        .setDefaultEdgeLabel(function() { return {}; });

    var id = job['Job ID'];
    var stageIds = job['Stage IDs'];
    var stageInfos = job['Stage Infos'];
    stageInfos.sort(function(left, right) {
        return left['Stage ID'] > right['Stage ID'];
    });
    spark.stageMap = {};
    stageInfos.forEach(function(stage) {
        var id = stage['Stage ID'];
        var parentNodes = stage['Parent IDs'];
        spark.stageMap[id] = stage;
        if (parentNodes.length === 0) {
            g.setNode(id, {'label' : stage['Stage Name'], class: 'type-TOP'});
        } else {
            g.setNode(id, {'label' : stage['Stage Name']});
            parentNodes.forEach(function(parentId) {
                g.setEdge(parentId, id);
            });
        }
    });

    // Create the renderer
    var render = new dagreD3.render();

    // Set up an SVG group so that we can translate the final graph.
    var svg = d3.select('#jobGraphSvg');

    // remove all graph first
    svg.selectAll('g').remove();

    var inner = svg.append("g");

    // Run the renderer. This is what draws the final graph.
    render(inner, g);

    svg.attr('viewBox', calculateVirtualBox(g.graph()));
    svg.attr('preserveAspectRatio', 'xMidYMid meet');

    render(inner, g);

    // Center the graph
    var zoom = d3.behavior.zoom().on('zoom', function() {
        inner.attr('transform', 'translate(' + d3.event.translate + ')' +
            'scale(' + d3.event.scale + ')');
    });
    svg.call(zoom);

    inner.selectAll('g.node')
        .attr('tabindex', "0")
        .attr('title', function(d, v) {
            return setToolTipForStage(d);
        })
        .on('keydown', function(d) {
            var upDownFocusStack = jobGraphUpDownFocusStacks.jobView;
            moveFocusByArrows(d3.event.code, g, d, upDownFocusStack);
            zoomGraph(inner);
        })
        .each(function(v) {
            $(this).tipsy({
                gravity: "w",
                opacity: 1,
                trigger:'hover',
                html: true });
            $(this).tipsy({
                gravity: "w",
                opacity: 1,
                trigger:'focus',
                html: true });
        })

    inner.selectAll("g.node")[0][0].focus()
}

function setToolTipForStage(stageId) {
    var stageInfo = spark.stageMap[stageId];
    if (stageInfo) {
        var filterStages = spark.currentSelectedStages.filter(function(s) {
           return s['stageId'] === parseInt(stageId);
        });
        if (filterStages.length === 1) {
            var selectedStage = filterStages[0];
            return "<p class='name'>Stage ID:{0}</p>".format(selectedStage['stageId'])
                + "<p class='description jobtips' align='left'>Input Bytes: {0}<br>".format(selectedStage['inputBytes'])
                +  "Output Bytes: {0}<br>".format(selectedStage['outputBytes'])
                + "Shuffle Read Bytes: {0}<br>".format(selectedStage['shuffleReadBytes'])
                + "Shuffle Write Bytes: {0}<br>".format(selectedStage['shuffleWriteBytes'])
                + "executorRunTime: {0}<br>".format(selectedStage['executorRunTime'])
                + "<hr class='jobview-hr'/>"
                + "Complete Tasks: {0}<br>".format(selectedStage['numCompleteTasks'])
                + "Failed Tasks: {0}<br>".format(selectedStage['numFailedTasks'])
                + "</p>";
        }

    }
}

function calculateVirtualBox(graph) {
    var minX = document.jobGraphView.minX
     - Math.min(
         document.jobGraphView.width / 3,
         Math.abs((graph.width + document.jobGraphView.tipsWidth - document.jobGraphView.width) / 2))

    return minX + " -10 " + document.jobGraphView.width + " " + document.jobGraphView.height;
}

function zoomGraph(node){
    var currentx = d3.transform(node.attr("transform")).translate[0];
    var currenty = d3.transform(node.attr("transform")).translate[1];
    var currentk = d3.transform(node.attr("transform")).scale[0];
    if(d3.event.ctrlKey && (d3.event.keyCode == 187 || d3.event.keyCode == 107)){
        currentk = currentk + 0.05;
        currentx = currentx + 1;
        currenty = currenty + 1;
        node.attr("transform",`translate(${currentx},${currenty}),scale(${currentk})`);
    }
    else if(d3.event.ctrlKey && (d3.event.keyCode == 189 || d3.event.keyCode == 109)){
        if (currentk > 0)
            currentk = currentk - 0.05;
        currentx = currentx  - 1;
        currenty = currenty  - 1;
        node.attr("transform",`translate(${currentx},${currenty}),scale(${currentk})`);
    }
}