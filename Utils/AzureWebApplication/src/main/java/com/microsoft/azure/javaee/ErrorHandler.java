/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.javaee;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

@ManagedBean(name = "errorHandler", eager = true)
@RequestScoped
public class ErrorHandler {
    public String getStatusCode() {
        return String.valueOf((Integer) FacesContext.getCurrentInstance().getExternalContext().getRequestMap()
                .get("javax.servlet.error.status_code"));
    }

    public String getMessage() {
        return (String) FacesContext.getCurrentInstance().getExternalContext().getRequestMap()
                .get("javax.servlet.error.message");
    }
}
