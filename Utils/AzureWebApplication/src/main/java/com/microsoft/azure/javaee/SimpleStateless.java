/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.javaee;

import java.util.List;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * Session Bean implementation class SimpleStateless
 */
@Stateless
@LocalBean
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SimpleStateless {

    @PersistenceContext(unitName = "demo-unit")
    private EntityManager entityManager;

    public SimpleStateless() {
    }

    public void addMovie(Movie movie) {
        entityManager.persist(movie);
    }

    public void deleteMovie(Movie movie) {
        entityManager.remove(movie);
    }

    @SuppressWarnings("unchecked")
    public List<Movie> getMovies() {
        Query query = entityManager.createQuery("SELECT m from Movie as m");
        return query.getResultList();
    }
}
