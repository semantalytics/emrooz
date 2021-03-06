/*
 * Copyright (C) 2015 see CREDITS.txt
 * All rights reserved.
 */

package fi.uef.envi.emrooz.sesame;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

import fi.uef.envi.emrooz.api.KnowledgeStore;
import fi.uef.envi.emrooz.api.QueryHandler;
import fi.uef.envi.emrooz.entity.qb.AttributeProperty;
import fi.uef.envi.emrooz.entity.qb.ComponentProperty;
import fi.uef.envi.emrooz.entity.qb.ComponentSpecification;
import fi.uef.envi.emrooz.entity.qb.DataStructureDefinition;
import fi.uef.envi.emrooz.entity.qb.Dataset;
import fi.uef.envi.emrooz.entity.qb.DimensionProperty;
import fi.uef.envi.emrooz.entity.qb.MeasureProperty;
import fi.uef.envi.emrooz.entity.qudt.QuantityValue;
import fi.uef.envi.emrooz.entity.qudt.Unit;
import fi.uef.envi.emrooz.entity.ssn.FeatureOfInterest;
import fi.uef.envi.emrooz.entity.ssn.Frequency;
import fi.uef.envi.emrooz.entity.ssn.MeasurementCapability;
import fi.uef.envi.emrooz.entity.ssn.Property;
import fi.uef.envi.emrooz.entity.ssn.Sensor;
import fi.uef.envi.emrooz.rdf.RDFEntityRepresenter;
import fi.uef.envi.emrooz.vocabulary.EV;
import fi.uef.envi.emrooz.vocabulary.QB;
import fi.uef.envi.emrooz.vocabulary.QUDTSchema;
import fi.uef.envi.emrooz.vocabulary.QUDTUnit;
import fi.uef.envi.emrooz.vocabulary.SDMXMetadata;
import fi.uef.envi.emrooz.vocabulary.SSN;

/**
 * <p>
 * Title: SesameKnowledgeStore
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

public class SesameKnowledgeStore implements KnowledgeStore {

	private Repository repository;
	private RepositoryConnection connection;
	private Map<URI, Sensor> sensors;
	private Map<URI, Dataset> datasets;
	private Map<URI, Property> properties;
	private Map<URI, FeatureOfInterest> features;
	private ValueFactory vf;
	private RDFEntityRepresenter representer;

	private static final Logger log = Logger
			.getLogger(SesameKnowledgeStore.class.getName());

	public SesameKnowledgeStore(Repository repository) {
		if (repository == null)
			throw new RuntimeException("[repository = null]");

		this.repository = repository;

		try {
			this.repository.initialize();
			this.connection = this.repository.getConnection();
		} catch (RepositoryException e) {
			if (log.isLoggable(Level.SEVERE))
				log.severe(e.getMessage());
		}

		this.vf = connection.getValueFactory();
		this.representer = new RDFEntityRepresenter();

		loadProperties();
		loadFeatures();
		loadSensors();
		loadDatasets();
	}

	@Override
	public void addSensor(Sensor sensor) {
		URI sensorId = sensor.getId();

		try {
			if (!connection.hasStatement(sensorId, RDF.TYPE, SSN.Sensor, false,
					new Resource[] {})) {
				load(representer.createRepresentation(sensor));
				return;
			}
		} catch (RepositoryException e) {
			if (log.isLoggable(Level.SEVERE))
				log.severe("Failed to check if sensor exists in knowledge store [sensor = "
						+ sensor + "]");
		}

		if (log.isLoggable(Level.INFO)) {
			log.info("Sensor already exists in knowledge store [sensor = "
					+ sensor + "]");
		}
	}

	@Override
	public void addDataset(Dataset dataset) {
		URI datasetId = dataset.getId();

		try {
			if (!connection.hasStatement(datasetId, RDF.TYPE, QB.DataSet,
					false, new Resource[] {})) {
				load(representer.createRepresentation(dataset));
				return;
			}
		} catch (RepositoryException e) {
			if (log.isLoggable(Level.SEVERE))
				log.severe("Failed to check if dataset exists in knowledge store [dataset = "
						+ dataset + "]");
		}

		if (log.isLoggable(Level.INFO)) {
			log.info("Dataset already exists in knowledge store [dataset = "
					+ dataset + "]");
		}
	}

	@Override
	public Set<Property> getProperties() {
		return Collections.unmodifiableSet(new HashSet<Property>(properties
				.values()));
	}

	@Override
	public Set<FeatureOfInterest> getFeaturesOfInterest() {
		return Collections.unmodifiableSet(new HashSet<FeatureOfInterest>(
				features.values()));
	}

	@Override
	public Set<Sensor> getSensors() {
		return Collections
				.unmodifiableSet(new HashSet<Sensor>(sensors.values()));
	}

	@Override
	public Set<Dataset> getDatasets() {
		return Collections.unmodifiableSet(new HashSet<Dataset>(datasets
				.values()));
	}

	@Override
	public SesameQueryHandler createQueryHandler(QueryHandler<Statement> other,
			ParsedQuery query) {
		return new SesameQueryHandler(other, query);
	}

	@Override
	public void close() {
		try {
			connection.close();
		} catch (RepositoryException e) {
			if (log.isLoggable(Level.SEVERE))
				log.severe(e.getMessage());
		}
	}

	@Override
	public void load(File file) {
		load(file, null);
	}

	public void load(Set<Statement> statements) {
		if (statements == null)
			return;

		for (Statement statement : statements) {
			try {
				connection.add(statement);
			} catch (RepositoryException e) {
				if (log.isLoggable(Level.SEVERE))
					log.severe(e.getMessage());
			}
		}

		loadProperties();
		loadFeatures();
		loadSensors();
		loadDatasets();
	}

	public void load(File file, String baseURI) {
		load(file, baseURI, RDFFormat.RDFXML);
	}

	public void load(File file, String baseURI, RDFFormat format) {
		try {
			connection.add(file, baseURI, format);
		} catch (RDFParseException | RepositoryException | IOException e) {
			if (log.isLoggable(Level.SEVERE))
				log.severe(e.getMessage());
		}

		loadProperties();
		loadFeatures();
		loadSensors();
		loadDatasets();
	}

	private void loadProperties() {
		properties = new HashMap<URI, Property>();

		String sparql = "prefix ssn: <" + SSN.ns + "#>" + "prefix rdf: <"
				+ RDF.NAMESPACE + ">" + "select ?id " + "where {"
				+ "?id rdf:type ssn:Property ." + "}";

		try {
			TupleQuery query = connection.prepareTupleQuery(
					QueryLanguage.SPARQL, sparql);
			TupleQueryResult rs = query.evaluate();

			while (rs.hasNext()) {
				BindingSet bs = rs.next();

				URI id = _uri(bs.getValue("id"));
				Property property = new Property(id);

				properties.put(id, property);
			}

		} catch (RepositoryException | MalformedQueryException
				| QueryEvaluationException e) {
			if (log.isLoggable(Level.SEVERE))
				log.severe(e.getMessage());
		}

		if (log.isLoggable(Level.INFO))
			log.info("Loaded properties (" + properties.size() + ") {"
					+ properties + "}");
	}

	private void loadFeatures() {
		features = new HashMap<URI, FeatureOfInterest>();

		String sparql = "prefix ssn: <" + SSN.ns + "#>" + "prefix rdf: <"
				+ RDF.NAMESPACE + ">" + "select ?id " + "where {"
				+ "?id rdf:type ssn:FeatureOfInterest ." + "}";

		try {
			TupleQuery query = connection.prepareTupleQuery(
					QueryLanguage.SPARQL, sparql);
			TupleQueryResult rs = query.evaluate();

			while (rs.hasNext()) {
				BindingSet bs = rs.next();

				URI id = _uri(bs.getValue("id"));
				FeatureOfInterest feature = new FeatureOfInterest(id);

				features.put(id, feature);
			}

		} catch (RepositoryException | MalformedQueryException
				| QueryEvaluationException e) {
			if (log.isLoggable(Level.SEVERE))
				log.severe(e.getMessage());
		}

		if (log.isLoggable(Level.INFO))
			log.info("Loaded features of interest (" + features.size() + ") {"
					+ features + "}");
	}

	private void loadSensors() {
		sensors = new HashMap<URI, Sensor>();

		String sparql = "prefix ssn: <"
				+ SSN.ns
				+ "#>"
				+ "prefix qudt: <"
				+ QUDTSchema.ns
				+ "#>"
				+ "prefix rdf: <"
				+ RDF.NAMESPACE
				+ ">"
				+ "prefix unit: <"
				+ QUDTUnit.ns
				+ "#>"
				+ "select ?sensorId ?propertyId ?featureId ?measCapabilityId ?measPropertyId ?valueId ?value "
				+ "where {"
				+ "?sensorId rdf:type ssn:Sensor ."
				+ "?sensorId ssn:observes ?propertyId ."
				+ "?propertyId rdf:type ssn:Property ."
				+ "?propertyId ssn:isPropertyOf ?featureId ."
				+ "?featureId rdf:type ssn:FeatureOfInterest ."
				+ "optional {"
				+ "?sensorId ssn:hasMeasurementCapability ?measCapabilityId ."
				+ "?measCapabilityId rdf:type ssn:MeasurementCapability ."
				+ "?measCapabilityId ssn:hasMeasurementProperty ?measPropertyId ."
				+ "?measPropertyId rdf:type ssn:Frequency ."
				+ "?measPropertyId ssn:hasValue ?valueId ."
				+ "?valueId rdf:type qudt:QuantityValue ."
				+ "?valueId qudt:unit unit:Hertz ."
				+ "?valueId qudt:numericValue ?value ." + "} }";

		try {
			TupleQuery query = connection.prepareTupleQuery(
					QueryLanguage.SPARQL, sparql);
			TupleQueryResult rs = query.evaluate();

			while (rs.hasNext()) {
				BindingSet bs = rs.next();

				URI sensorId = _uri(bs.getValue("sensorId"));
				URI propertyId = _uri(bs.getValue("propertyId"));
				URI featureId = _uri(bs.getValue("featureId"));

				Sensor sensor = sensors.get(sensorId);
				Property property = null;

				if (sensor == null) {
					// This sensor doesn't exist, create it
					sensor = new Sensor(sensorId);
					sensors.put(sensorId, sensor);
				} else {
					// The sensor exists, check the property
					property = sensor.getObservedProperty(propertyId);
				}

				if (property == null) {
					property = new Property(propertyId);
					sensor.addObservedProperty(property);
				}

				property.addPropertyOf(new FeatureOfInterest(featureId));

				if (bs.getValue("measCapabilityId") != null) {
					// Measurement capability is set optional. For applications
					// the frequency must be set, otherwise Cassandra doesn't
					// know when to rollover. However, for testing purposes it
					// is convenient not to have to specify the measurement
					// capability for each sensor.
					URI measCapabilityId = _uri(bs.getValue("measCapabilityId"));
					URI measPropertyId = _uri(bs.getValue("measPropertyId"));
					URI valueId = _uri(bs.getValue("valueId"));
					Double value = Double.valueOf(bs.getValue("value")
							.stringValue());

					MeasurementCapability measCapability = new MeasurementCapability(
							measCapabilityId);
					Frequency measProperty = new Frequency(measPropertyId);
					QuantityValue quantityValue = new QuantityValue(valueId);

					sensor.addMeasurementCapability(measCapability);
					measCapability.addMeasurementProperty(measProperty);
					measProperty.setQuantityValue(quantityValue);
					quantityValue.setNumericValue(value);
					quantityValue.setUnit(new Unit(QUDTUnit.Hertz));
				}
			}

		} catch (RepositoryException | MalformedQueryException
				| QueryEvaluationException e) {
			if (log.isLoggable(Level.SEVERE))
				log.severe(e.getMessage());
		}

		if (log.isLoggable(Level.INFO))
			log.info("Loaded sensors (" + sensors.size() + ") {" + sensors
					+ "}");
	}

	private void loadDatasets() {
		datasets = new HashMap<URI, Dataset>();

		String sparql = "prefix qb: <"
				+ QB.ns
				+ "#>"
				+ "prefix sdmx-metadata: <"
				+ SDMXMetadata.ns
				+ "#>"
				+ "prefix qudt: <"
				+ QUDTSchema.ns
				+ "#>"
				+ "prefix rdf: <"
				+ RDF.NAMESPACE
				+ ">"
				+ "prefix unit: <"
				+ QUDTUnit.ns
				+ "#>"
				+ "select ?datasetId ?frequencyId ?frequencyValue ?structureId ?componentId ?propertyId ?propertyType ?required ?order "
				+ "where {" + "?datasetId rdf:type qb:DataSet ."
				+ "?datasetId sdmx-metadata:freq ?frequencyId ."
				+ "sdmx-metadata:freq rdf:type qb:AttributeProperty ."
				+ "?frequencyId rdf:type qudt:QuantityValue ."
				+ "?frequencyId qudt:unit unit:Hertz ."
				+ "?frequencyId qudt:numericValue ?frequencyValue ."
				+ "optional {" + "?datasetId qb:structure ?structureId ."
				+ "?structureId rdf:type qb:DataStructureDefinition ."
				+ "?structureId qb:component ?componentId ."
				+ "?componentId rdf:type qb:ComponentSpecification ."
				+ "?componentId qb:componentProperty ?propertyId ."
				+ "?propertyId rdf:type ?propertyType ." + "optional {"
				+ "?componentId qb:componentRequired ?required ."
				+ "?componentId qb:order ?order ." + "}" + "}" + "}";

		try {
			TupleQuery query = connection.prepareTupleQuery(
					QueryLanguage.SPARQL, sparql);
			TupleQueryResult rs = query.evaluate();

			while (rs.hasNext()) {
				BindingSet bs = rs.next();

				URI datasetId = _uri(bs.getValue("datasetId"));

				Dataset dataset = datasets.get(datasetId);
				DataStructureDefinition structure = null;

				if (dataset == null) {
					// This dataset doesn't exist, create it
					URI frequencyId = _uri(bs.getValue("frequencyId"));
					Double frequencyValue = Double.valueOf(bs.getValue(
							"frequencyValue").stringValue());

					dataset = new Dataset(datasetId, new QuantityValue(
							frequencyId, frequencyValue, new Unit(
									QUDTUnit.Hertz)));
					datasets.put(datasetId, dataset);
				} else {
					structure = dataset.getStructure();
				}

				if (bs.getValue("structureId") != null) {
					URI structureId = _uri(bs.getValue("structureId"));

					if (structure == null) {
						structure = new DataStructureDefinition(structureId);
					}

					URI componentId = _uri(bs.getValue("componentId"));

					// These are defined by default, so skip them
					if (!(componentId.equals(EV.freqComponentSpecification) || componentId
							.equals(EV.timePeriodComponentSpecification))) {
						URI propertyId = _uri(bs.getValue("propertyId"));
						URI propertyType = _uri(bs.getValue("propertyType"));
						Value required = bs.getValue("required");
						Value order = bs.getValue("order");

						ComponentProperty property = null;

						if (propertyType.equals(QB.DimensionProperty))
							property = new DimensionProperty(propertyId);
						else if (propertyType.equals(QB.MeasureProperty))
							property = new MeasureProperty(propertyId);
						else if (propertyType.equals(QB.AttributeProperty))
							property = new AttributeProperty(propertyId);

						if (property != null) {
							ComponentSpecification component = new ComponentSpecification(
									componentId, property);

							if (required != null)
								component.setRequired(Boolean.valueOf(required
										.stringValue()));

							if (order != null)
								component.setOrder(Integer.valueOf(order
										.stringValue()));

							structure.addComponent(component);
						}
					}
				}
			}

		} catch (RepositoryException | MalformedQueryException
				| QueryEvaluationException e) {
			if (log.isLoggable(Level.SEVERE))
				log.severe(e.getMessage());
		}

		if (log.isLoggable(Level.INFO))
			log.info("Loaded datasets (" + datasets.size() + ") {" + datasets
					+ "}");
	}

	private URI _uri(Value value) {
		return vf.createURI(value.stringValue());
	}

}
