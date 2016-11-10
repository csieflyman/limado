/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.model;

import java.io.Serializable;

/**
 * @author csieflyman
 */
public interface Identifiable<ID extends Serializable> {

    ID getId();
}
