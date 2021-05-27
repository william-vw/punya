package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.appinventor.components.runtime.util.YailDictionary;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.gson.JsonElement;
import java.time.LocalTime;
import java.util.Arrays;
import com.google.maps.GeoApiContext;
import com.google.maps.NearbySearchRequest;
import com.google.maps.PendingResult.Callback;
import com.google.maps.PlacesApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceType;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;
import com.google.maps.model.OpeningHours.Period;

import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.builtin.ProbeKeys;

@DesignerComponent(version = YaVersion.GPLACES_COMPONENT_VERSION,
        description = "<p>A component that (periodically) returns a set of nearby places using the Google Places API " +
            "(see also OverpassPlacesService). " +
            "It supports periodically getting the user's location (at a configurable time-interval), and" +
            "a period for scanning (returning the location with highest accuracy found during the scan, " +
            "or the first one with good-enough accuracy). After getting the location, " +
            "the component will get nearby places based on the given radius.</p>" +
            "You can also specify a minimum-location-change, where the Places API" +
            " will only be called in case the user's location has changed significantly.</p>",
        category = ComponentCategory.CONNECTIVITY, nonVisible = true, iconName = "images/googlePlaces.png")
@SimpleObject
@UsesLibraries(libraries = "google-maps-services-0.18.1.jar")
public class GooglePlacesService extends PlacesWebService implements Callback<PlacesSearchResponse> {

    private GeoApiContext context;

    /**
     * Creates a new GooglePlacesService component.
     *
     * @param container the container that this component will be placed in
     */
    protected GooglePlacesService(ComponentContainer container) {
        super(container);

        apiKeyRequired = true;
    }

    @Override
    protected boolean checkInput() {
        if (super.checkInput()) {
            if (placeType != null) {
                try {
                    PlaceType.valueOf(placeType);
                } catch (IllegalArgumentException e) {
                    ServiceError("Unknown type of place: " + placeType + ". See "
                            + "https://developers.google.com/maps/documentation/places/web-service/supported_types "
                            + "for a list of supported types.");
                    return false;
                }
            }
            return true;

        } else
            return false;
    }

    /**
     *
     * @return placeType
     */
    @SimpleProperty
    public String PlaceType() {
        return placeType;
    }

    /**
     * The type of nearby places that should be returned. See
     * https://developers.google.com/maps/documentation/places/web-service/supported_types
     * for the list of supported types.
     *
     * @param type
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
    @SimpleProperty(category = PropertyCategory.BEHAVIOR)
    public void PlaceType(String type) {
        this.placeType = type.toUpperCase();
    }

    /**
     * This method is called when receiving new location data, as per the
     * configuration of the location probe sensor. A subclass will do something with
     * this location (e.g., call a service for nearby places) and likely raise an
     * event.
     *
     * @param location
     */
    @Override
    public void onLocationReceived(Location location) {
        System.out.println("location: " + location);

        double lat = location.getLat();
		double lon = location.getLon();

        context = new GeoApiContext.Builder().apiKey(apiKey).build();
        LatLng myLocation = new LatLng(lat, lon);

        try {
            NearbySearchRequest request = PlacesApi.nearbySearchQuery(context, myLocation).radius(nearbyRadius);
            if (placeType != null)
                request.type(PlaceType.valueOf(placeType));

            // TODO does not get called
//			request.setCallback(this);

            onResult(request.await());

        } catch (IOException | InterruptedException | ApiException e) {
            onFailure(e);
        }
    }

    @Override
    public void onResult(PlacesSearchResponse response) {
        System.out.println("response? " + response);

        List<YailDictionary> places = new ArrayList<>();
        for (PlacesSearchResult result : response.results)
            places.add(toDictionary(result));

        NearbyPlacesReceived(YailList.makeList(places));

        // TODO apparently need to wait 2 seconds
        // https://developers.google.com/maps/documentation/javascript/places#PlaceSearchPaging

//		if (response.nextPageToken != null) {
//			try {
//				onResult(PlacesApi.nearbySearchNextPage(context, response.nextPageToken).await());
//
//			} catch (IOException | InterruptedException | ApiException e) {
//				e.printStackTrace();
//				ServiceError(e.getMessage());
//			}
//		}

        context.shutdown();
    }

    @Override
    public void onFailure(Throwable e) {
//		StringWriter sw = new StringWriter();
//		PrintWriter pw = new PrintWriter(sw);
//		e.printStackTrace(pw);
//		ServiceError(sw.toString());

        e.printStackTrace();
        ServiceError(e.getMessage());
    }

    private YailDictionary toDictionary(PlacesSearchResult result) {
        System.out.println(result);

        YailDictionary place = new YailDictionary();

        LatLng latLng = result.geometry.location;
        place.put("location", YailList.makeList(new Double[] { latLng.lat, latLng.lng }));

        place.put("types", YailList.makeList(result.types));

        place.put("permanentlyClosed", result.permanentlyClosed);

        if (result.openingHours != null) {

            if (result.openingHours.openNow)
                place.put("openNow", result.openingHours.openNow);

            if (result.openingHours.periods != null) {
                YailList hours = YailList.makeEmptyList();
                place.put("hours", hours);

                for (Period orPeriod : result.openingHours.periods) {
                    YailList open = YailList.makeList(new Object[] { orPeriod.open.day.ordinal(), orPeriod.open.time });
                    YailList close = YailList
                            .makeList(new Object[] { orPeriod.close.day.ordinal(), orPeriod.close.time });

                    YailList period = YailList.makeList(new Object[] { open, close });
                    hours.add(period);
                }
            }
        }
        System.out.println(place + "\n");

        return place;
    }
}