package com.microsoft.azure.toolkit.ide.guidance.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.microsoft.azure.toolkit.ide.guidance.Course;

import javax.annotation.Nonnull;
import javax.swing.*;

public class GuidanceView extends SimpleToolWindowPanel {
    private final Project project;
    private JPanel pnlRoot;
    private CourseView pnlCourse;
    private CoursesView pnlCourses;

    public GuidanceView(final Project project) {
        super(true);
        this.project = project;
        $$$setupUI$$$();
        this.setContent(pnlRoot);
        showCoursesView();
    }

    public void showCoursesView() {
        if (pnlCourse.closeCourse()) {
            pnlCourses.setVisible(true);
        }
    }

    public void showCourseView(@Nonnull Course course) {
        pnlCourses.setVisible(false);
        pnlCourse.openCourse(course);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.pnlCourse = new CourseView(project);
        this.pnlCourses = new CoursesView(project);
    }
}
