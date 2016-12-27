package com.limado.collab.model;

import java.io.Serializable;

/**
 * @author csieflyman
 */
public interface Identifiable<ID extends Serializable> {

    ID getId();
}
