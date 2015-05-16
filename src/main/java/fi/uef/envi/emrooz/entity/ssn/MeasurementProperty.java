/*
 * Copyright (C) 2015 see CREDITS.txt
 * All rights reserved.
 */

package fi.uef.envi.emrooz.entity.ssn;

import org.openrdf.model.URI;

import fi.uef.envi.emrooz.entity.AbstractEntity;
import fi.uef.envi.emrooz.entity.EntityVisitor;

/**
 * <p>
 * Title: MeasurementProperty
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Project: Emrooz
 * </p>
 * <p>
 * Copyright: Copyright (C) 2015
 * </p>
 * 
 * @author Markus Stocker
 */

public abstract class MeasurementProperty extends AbstractEntity {
	
	public MeasurementProperty(URI id, URI type) {
		super(id, type);
	}

	public void accept(EntityVisitor visitor) {
		visitor.visit(this);
	}

}
