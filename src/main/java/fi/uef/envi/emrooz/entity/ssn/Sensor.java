/*
 * Copyright (C) 2015 see CREDITS.txt
 * All rights reserved.
 */

package fi.uef.envi.emrooz.entity.ssn;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.URI;

import fi.uef.envi.emrooz.entity.AbstractEntity;
import fi.uef.envi.emrooz.entity.EntityVisitor;
import fi.uef.envi.emrooz.vocabulary.SSN;

/**
 * <p>
 * Title: Sensor
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

public class Sensor extends AbstractEntity {

	private Property observes;
	private Set<MeasurementCapability> capabilities;

	public Sensor(URI id) {
		this(id, SSN.Sensor);
	}

	public Sensor(URI id, URI type) {
		super(id, type);

		this.capabilities = new HashSet<MeasurementCapability>();
	}

	public void setObservedProperty(Property property) {
		this.observes = property;
	}

	public void addMeasurementCapability(MeasurementCapability capability) {
		if (capability == null)
			return;

		capabilities.add(capability);
	}

	public Set<MeasurementCapability> getMeasurementCapabilities() {
		return Collections.unmodifiableSet(capabilities);
	}

	public Property getObservedProperty() {
		return observes;
	}

	public void accept(EntityVisitor visitor) {
		visitor.visit(this);
	}

	public int hashCode() {
		return 31 * (id.hashCode() + type.hashCode());
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Sensor))
			return false;

		Sensor other = (Sensor) obj;

		if (other.id.equals(id) && other.type.equals(type))
			return true;

		return false;
	}

	public String toString() {
		return "Sensor [id = " + id + "; type = " + type + "; observes = "
				+ observes + "; capabilities = " + capabilities + "]";
	}

}
