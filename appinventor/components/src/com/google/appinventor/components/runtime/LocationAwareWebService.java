package com.google.appinventor.components.runtime;

import android.util.Log;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.LocationUtil;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.gson.JsonElement;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;

/**
 * This class is meant to act as a superclass to concrete location-aware, web service components.
 *
 * @author william.van.woensel@gmail.com
 */
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.ACCESS_FINE_LOCATION")
public abstract class LocationAwareWebService extends WebService implements Probe.DataListener {

    private final String TAG = "PlacesWebService";

    protected LocationProbeSensor locationProbeSensor;

    protected int minimumLocationChange = 0; // 0 meters

    protected Location testLocation;

    protected Location priorLocation;

    /**
     * Creates a new LocationAwareWebService component.
     *
     * @param container the container that this component will be placed in
     */
    protected LocationAwareWebService(ComponentContainer container) {
        super(container);

        this.locationProbeSensor = new LocationProbeSensor(container);
        // override the data listener with this component
        // when run-once or scheduled location fixes are received,
        // the methods of this listener will be called
        locationProbeSensor.overrideListener(this);
    }

    /**
     *
     * @return defaultInterval
     */
    @SimpleProperty
    public int DefaultInterval(){return locationProbeSensor.DefaultInterval();}

    /**
     * The default interval (in seconds) between actions, where an action includes a location probe plus service call.
     *
     * @param defaultInterval
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "180")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public void DefaultInterval(int defaultInterval) { locationProbeSensor.DefaultInterval(defaultInterval); }

    /**
     *
     * @return defaultDuration
     */
    @SimpleProperty
    public int DefaultDuration(){ return locationProbeSensor.DefaultDuration();}

    /**
     * The default duration (in seconds) of each location probe scan.
     *
     * @param defaultDuration
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "10")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public void DefaultDuration(int defaultDuration) { locationProbeSensor.DefaultDuration(defaultDuration); }

    /**
     *
     * @return goodEnoughAccuracy
     */
    @SimpleProperty
    public int GoodEnoughAccuracy() {
        return locationProbeSensor.GoodEnoughAccuracy();
    }

    /**
     * The good-enough-accuracy of the location data (0-100).
     * If the location accuracy lies below this threshold, then the online service will not be called.
     *
     * @param goodEnoughAccuracy
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "80")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public void GoodEnoughAccuracy(int goodEnoughAccuracy) { locationProbeSensor.GoodEnoughAccuracy(goodEnoughAccuracy); }

    @SimpleProperty
    public boolean UseGPS() {
        return locationProbeSensor.UseGPS();
    }

    /**
     * Whether the location probe will use GPS or not.
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public void UseGPS(boolean useGPS) { locationProbeSensor.UseGPS(useGPS); }

    @SimpleProperty
    public boolean UseNetwork() { return locationProbeSensor.UseNetwork(); }

    /**
     * Whether the location probe will use the network or not.
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public void UseNetwork(boolean useNetwork) { locationProbeSensor.UseNetwork(useNetwork); }

    /**
     *
     * @return minimumLocationChange
     */
    @SimpleProperty
    public int MinimumLocationChange() { return minimumLocationChange; }

    /**
     * The minimal difference in location (in meters) compared to the prior location, before the service is called.
     * This avoids calling the online service for location-specific data when the user's location has not really changed much.
     *
     * @param minimumLocationChange
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public void MinimumLocationChange(int minimumLocationChange) { this.minimumLocationChange = minimumLocationChange; }

    /**
     *
     * @return testLocation
     */
    @SimpleProperty
    public YailList TestLocation() {
        return YailList.makeList(new Double[] { testLocation.getLat(), testLocation.getLon() });
    }

    /**
     * The location used for testing the component.
     * This location will be used to access the service instead of the user's actual location given by GPS or network.
     *
     * @param testLocation
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0")
    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public void TestLocation(YailList testLocation) {
        if (testLocation.length() == 2) {
            if (testLocation.get(0) instanceof Number &&
                testLocation.get(1) instanceof Number) {

                double lat = ((Number) testLocation.get(0)).doubleValue();
                double lon = ((Number) testLocation.get(1)).doubleValue();

                this.testLocation = new Location(lat, lon);
                return;
            }
        }

        ServiceError("Expecting the following format for location: (<latitude> <longitude>)");
    }

    /**
     * This method is called when receiving new location data from the location
     * probe sensor.
     *
     * @param completeProbeUri, data
     */
    @Override
    public void onDataReceived(IJsonObject completeProbeUri, IJsonObject data) {
        Location location = new Location(data.get(ProbeKeys.LocationKeys.LATITUDE).getAsDouble(),
                data.get(ProbeKeys.LocationKeys.LONGITUDE).getAsDouble(),
                data.get(ProbeKeys.LocationKeys.ACCURACY).getAsFloat(), data.get("mProvider").getAsString(),
                data.get(ProbeKeys.LocationKeys.TIMESTAMP).getAsLong());

        if (this.priorLocation != null && minimumLocationChange > 0) {
            double distance = LocationUtil.distance(priorLocation.lat, priorLocation.lon, location.lat, location.lon);

            if (distance < minimumLocationChange) {
                System.out.println("distance (" + distance + "m) did not exceed min. location change ("
                        + minimumLocationChange + "m) - ignoring location update");
                return;
            }
        }

        this.priorLocation = location;
        onLocationReceived(location);
    }

    @Override
    public void onDataCompleted(IJsonObject iJsonObject, JsonElement jsonElement) {
    }

    /**
     * Called whenever a new location is received from the location probe sensor. A
     * subclass will implement this method to do something with this location (e.g.,
     * call a service for nearby places) and likely raise an event.
     *
     * @param location
     */
    protected abstract void onLocationReceived(Location location);

    protected static class Location {

        double lat;
        double lon;
        float accuracy;
        String provider;
        long timestamp;

        public Location(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        public Location(double lat, double lon, float accuracy, String provider, long timestamp) {
            this.lat = lat;
            this.lon = lon;
            this.accuracy = accuracy;
            this.provider = provider;
            this.timestamp = timestamp;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public float getAccuracy() {
            return accuracy;
        }

        public String getProvider() {
            return provider;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return "LocationData{" + "lat=" + lat + ", lon=" + lon + ", accuracy=" + accuracy + ", provider='"
                    + provider + '\'' + ", timestamp=" + timestamp + '}';
        }
    }
}