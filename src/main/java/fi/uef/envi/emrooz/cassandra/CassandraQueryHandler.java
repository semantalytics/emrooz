/*
 * Copyright (C) 2015 see CREDITS.txt
 * All rights reserved.
 */

package fi.uef.envi.emrooz.cassandra;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.openrdf.model.URI;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import fi.uef.envi.emrooz.Registration;
import fi.uef.envi.emrooz.Rollover;
import fi.uef.envi.emrooz.query.SensorObservationQuery;

/**
 * <p>
 * Title: CassandraQueryHandler
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

public class CassandraQueryHandler extends CassandraRequestHandler {

	private Session session;
	private SensorObservationQuery query;
	private PreparedStatement sensorObservationSelectStatement;

	private static final Logger log = Logger
			.getLogger(CassandraQueryHandler.class.getName());

	public CassandraQueryHandler(Map<String, Registration> registrations,
			Map<URI, Map<URI, Map<URI, String>>> registrationIdsMap,
			Session session,
			PreparedStatement sensorObservationSelectStatement,
			SensorObservationQuery query) {
		super(registrations, registrationIdsMap);

		if (session == null)
			throw new NullPointerException("[session = null]");
		if (sensorObservationSelectStatement == null)
			throw new NullPointerException(
					"[sensorObservationSelectStatement = null]");

		this.session = session;
		this.sensorObservationSelectStatement = sensorObservationSelectStatement;
		this.query = query;
	}

	public Set<Iterator<Row>> evaluate() {
		return getSensorObservations(query.getSensorId(),
				query.getPropertyId(), query.getFeatureOfInterestId(),
				query.getTimeFrom(), query.getTimeTo());
	}

	private Set<Iterator<Row>> getSensorObservations(URI sensor, URI property,
			URI feature, DateTime timeFrom, DateTime timeTo) {
		if (sensor == null || property == null || feature == null
				|| timeFrom == null || timeTo == null) {
			if (log.isLoggable(Level.SEVERE))
				log.severe("At least one parameter is null; returned empty set [sensor = "
						+ sensor
						+ "; property = "
						+ property
						+ "; feature = "
						+ feature
						+ "; timeFrom = "
						+ timeFrom
						+ "; timeTo = "
						+ timeTo + "]");

			return Collections.emptySet();
		}

		String registrationId = getCachedRegistrationId(sensor, property,
				feature);

		if (registrationId == null) {
			if (log.isLoggable(Level.SEVERE))
				log.severe("Registration id not found in cache [sensor = "
						+ sensor + "; propery = " + property + "; feature = "
						+ feature + "]");

			return Collections.emptySet();
		}

		Registration registration = registrations.get(registrationId);

		if (registration == null) {
			if (log.isLoggable(Level.SEVERE))
				log.severe("Registration not found [sensor = " + sensor
						+ "; propery = " + property + "; feature = " + feature
						+ "]");

			return Collections.emptySet();
		}

		Rollover rollover = registration.getRollover();

		if (rollover == null) {
			if (log.isLoggable(Level.SEVERE))
				log.severe("Registration rollover is null [registration = "
						+ registration + "]");

			return Collections.emptySet();
		}

		DateTime time = timeFrom;
		Set<Iterator<Row>> ret = new HashSet<Iterator<Row>>();

		while (time.isBefore(timeTo)) {
			Iterator<Row> it = getSensorObservations(
					getRowKey(sensor, property, feature, time), time, timeTo);

			if (it != null)
				ret.add(it);

			if (rollover.equals(Rollover.YEAR))
				time = time.year().roundFloorCopy().plusYears(1);
			else if (rollover.equals(Rollover.MONTH))
				time = time.monthOfYear().roundFloorCopy().plusMonths(1);
			else if (rollover.equals(Rollover.DAY))
				time = time.dayOfMonth().roundFloorCopy().plusDays(1);
			else if (rollover.equals(Rollover.HOUR))
				time = time.hourOfDay().roundFloorCopy().plusHours(1);
			else if (rollover.equals(Rollover.MINUTE))
				time = time.minuteOfHour().roundFloorCopy().plusMinutes(1);
			else
				throw new RuntimeException("Unsupported rollover [rollover = "
						+ rollover + "]");
		}

		return Collections.unmodifiableSet(ret);
	}

	private Iterator<Row> getSensorObservations(String rowKey,
			DateTime timeFrom, DateTime timeTo) {
		if (timeFrom == null || timeTo == null) {
			if (log.isLoggable(Level.SEVERE))
				log.severe("Parameters cannot be null; returned empty result set [timeFrom = "
						+ timeFrom + "; timeTo = " + timeTo + "]");

			return null;
		}

		return getSensorObservations(rowKey, timeFrom.toDate(), timeTo.toDate());
	}

	private Iterator<Row> getSensorObservations(String rowKey,
			Date columnNameFrom, Date columnNameTo) {
		if (rowKey == null || columnNameFrom == null || columnNameTo == null) {
			if (log.isLoggable(Level.WARNING))
				log.warning("Returned empty result set [rowKey = " + rowKey
						+ "; columnNameFrom = " + columnNameFrom
						+ "; columnNameTo = " + columnNameTo + "]");

			return null;
		}

		return session.execute(
				new BoundStatement(sensorObservationSelectStatement).bind(
						rowKey, columnNameFrom, columnNameTo)).iterator();
	}
	
}