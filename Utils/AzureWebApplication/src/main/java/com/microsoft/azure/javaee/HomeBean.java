/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.javaee;

import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

@ManagedBean(name = "helloWorld", eager = true)
@RequestScoped
public class HomeBean {

    @EJB
    private SimpleStateless simpleStateless;

    public List<Movie> getMovies() {
        final HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext()
                .getSession(false);
        if (session != null && session.getAttribute("aDate") == null) {
            session.setAttribute("aDate", new Date());
        }
        return simpleStateless.getMovies();
    }
}
