package com.google.appinventor.components.runtime.util;

public class LocationUtil {

	// Haversin method
	// (does not consider differences in elevation)
	
	public static double distance(double lat1, double lon1, double lat2, double lon2) {
		double earthRadius = 6371000; // meters
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lon2 - lon1);
		double sinDLat = Math.sin(dLat / 2);
		double sinDLng = Math.sin(dLng / 2);

		double a = Math.pow(sinDLat, 2)
				+ Math.pow(sinDLng, 2) * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double dist = (earthRadius * c);

		return dist;
	}

	public static void main(String[] args) {
		System.out.println("distance: " + distance(44.635614, -63.575676, 44.637269, -63.573997));
	}
}
