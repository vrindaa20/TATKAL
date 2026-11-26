package com.tatkalx.dao;

import java.util.List;

/**
 * Generic DAO contract demonstrating Java Generics for compile-time type-safe JDBC CRUD.
 */
public interface GenericDao<T, ID> {

    ID insert(T entity);

    T findById(ID id);

    List<T> findAll();

    boolean update(T entity);

    boolean deleteById(ID id);
}
