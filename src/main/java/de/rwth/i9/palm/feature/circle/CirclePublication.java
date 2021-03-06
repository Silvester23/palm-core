package de.rwth.i9.palm.feature.circle;

import java.util.Map;

import de.rwth.i9.palm.model.Circle;

public interface CirclePublication
{
	public Map<String, Object> getCirclePublicationMap( Circle circle );

	public Map<String, Object> getCirclePublicationByCircleId( String circleId, String query, String year, Integer startPage, Integer maxresult, String orderBy );
}
