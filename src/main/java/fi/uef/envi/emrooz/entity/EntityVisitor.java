/*
 * Copyright (C) 2015 see CREDITS.txt
 * All rights reserved.
 */

package fi.uef.envi.emrooz.entity;

import fi.uef.envi.emrooz.entity.qb.AttributeProperty;
import fi.uef.envi.emrooz.entity.qb.ComponentSpecification;
import fi.uef.envi.emrooz.entity.qb.DataStructureDefinition;
import fi.uef.envi.emrooz.entity.qb.Dataset;
import fi.uef.envi.emrooz.entity.qb.DatasetObservation;
import fi.uef.envi.emrooz.entity.qb.DimensionProperty;
import fi.uef.envi.emrooz.entity.qb.MeasureProperty;
import fi.uef.envi.emrooz.entity.qudt.QuantityValue;
import fi.uef.envi.emrooz.entity.qudt.Unit;
import fi.uef.envi.emrooz.entity.ssn.FeatureOfInterest;
import fi.uef.envi.emrooz.entity.ssn.Frequency;
import fi.uef.envi.emrooz.entity.ssn.MeasurementCapability;
import fi.uef.envi.emrooz.entity.ssn.ObservationValue;
import fi.uef.envi.emrooz.entity.ssn.ObservationValueDouble;
import fi.uef.envi.emrooz.entity.ssn.Property;
import fi.uef.envi.emrooz.entity.ssn.Sensor;
import fi.uef.envi.emrooz.entity.ssn.SensorObservation;
import fi.uef.envi.emrooz.entity.ssn.SensorOutput;
import fi.uef.envi.emrooz.entity.time.Instant;
import fi.uef.envi.emrooz.entity.time.TemporalEntity;

/**
 * <p>
 * Title: EntityVisitor
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

public interface EntityVisitor {

	public void visit(SensorObservation entity);
	
	public void visit(Sensor entity);
	
	public void visit(Property entity);
	
	public void visit(FeatureOfInterest entity);
	
	public void visit(SensorOutput entity);
	
	public void visit(ObservationValue entity);
	
	public void visit(ObservationValueDouble entity);
	
	public void visit(MeasurementCapability entity);
	
	public void visit(Frequency entity);
	
	public void visit(TemporalEntity entity);
	
	public void visit(Instant entity);
	
	public void visit(QuantityValue entity);
	
	public void visit(Unit entity);
	
	public void visit(Dataset entity);
	
	public void visit(DataStructureDefinition entity);
	
	public void visit(ComponentSpecification entity);
	
	public void visit(DimensionProperty entity);
	
	public void visit(MeasureProperty entity);
	
	public void visit(AttributeProperty entity);
	
	public void visit(DatasetObservation entity);
	
}
