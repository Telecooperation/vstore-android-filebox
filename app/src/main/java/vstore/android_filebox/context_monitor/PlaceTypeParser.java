package vstore.android_filebox.context_monitor;

import java.util.Arrays;
import java.util.List;

import vstore.framework.context.types.place.PlaceType;

import static com.google.android.gms.location.places.Place.TYPE_AIRPORT;
import static com.google.android.gms.location.places.Place.TYPE_AMUSEMENT_PARK;
import static com.google.android.gms.location.places.Place.TYPE_BAKERY;
import static com.google.android.gms.location.places.Place.TYPE_BANK;
import static com.google.android.gms.location.places.Place.TYPE_BAR;
import static com.google.android.gms.location.places.Place.TYPE_BOOK_STORE;
import static com.google.android.gms.location.places.Place.TYPE_CAFE;
import static com.google.android.gms.location.places.Place.TYPE_CITY_HALL;
import static com.google.android.gms.location.places.Place.TYPE_FOOD;
import static com.google.android.gms.location.places.Place.TYPE_GROCERY_OR_SUPERMARKET;
import static com.google.android.gms.location.places.Place.TYPE_GYM;
import static com.google.android.gms.location.places.Place.TYPE_LOCALITY;
import static com.google.android.gms.location.places.Place.TYPE_LODGING;
import static com.google.android.gms.location.places.Place.TYPE_MOVIE_THEATER;
import static com.google.android.gms.location.places.Place.TYPE_MUSEUM;
import static com.google.android.gms.location.places.Place.TYPE_NIGHT_CLUB;
import static com.google.android.gms.location.places.Place.TYPE_PARK;
import static com.google.android.gms.location.places.Place.TYPE_POINT_OF_INTEREST;
import static com.google.android.gms.location.places.Place.TYPE_RESTAURANT;
import static com.google.android.gms.location.places.Place.TYPE_SCHOOL;
import static com.google.android.gms.location.places.Place.TYPE_SHOPPING_MALL;
import static com.google.android.gms.location.places.Place.TYPE_STADIUM;
import static com.google.android.gms.location.places.Place.TYPE_STORE;
import static com.google.android.gms.location.places.Place.TYPE_UNIVERSITY;
import static vstore.framework.context.types.place.PlaceType.UNKNOWN;

public class PlaceTypeParser {
    private PlaceTypeParser() {}

    /**
     * Contains types from Google Places that should be treated as POI by the algorithm.
     */
    private static final List<Integer> POI_TYPES = Arrays.asList(
            TYPE_POINT_OF_INTEREST,
            TYPE_LOCALITY,
            TYPE_SCHOOL,
            TYPE_UNIVERSITY,
            TYPE_MUSEUM,
            TYPE_GYM,
            TYPE_PARK,
            TYPE_AMUSEMENT_PARK,
            TYPE_LODGING,
            TYPE_AIRPORT,
            TYPE_BANK
    );
    private static final String TEXT_POI = "Point of interest";

    private static final List<Integer> SHOPPING_TYPES = Arrays.asList(
            TYPE_SHOPPING_MALL,
            TYPE_GROCERY_OR_SUPERMARKET,
            TYPE_STORE,
            TYPE_BAKERY,
            TYPE_BOOK_STORE
    );
    private static final String TEXT_SHOPPING = "Shopping";

    /**
     * Contains types from Google Places that should be treated as event places by the algorithm.
     */
    private static final List<Integer> EVENT_TYPES = Arrays.asList(
            TYPE_STADIUM,
            TYPE_CITY_HALL,
            TYPE_NIGHT_CLUB
    );
    private static final String TEXT_EVENT = "Event";

    private static final List<Integer> SOCIAL_TYPES = Arrays.asList(
            TYPE_FOOD,
            TYPE_RESTAURANT,
            TYPE_CAFE,
            TYPE_MOVIE_THEATER,
            TYPE_BAR
    );
    private static final String TEXT_SOCIAL = "Social";

    private static final String TEXT_UNKNOWN = "Unknown";

    /**
     * Check if the type is supported by the framework.
     * @param gApiType An int constant from the Google Places API
     * @return True, if the type is supported
     */
    public static boolean isPlaceTypeKnown(int gApiType) {
        return POI_TYPES.contains(gApiType)
                || EVENT_TYPES.contains(gApiType)
                || SHOPPING_TYPES.contains(gApiType)
                || SOCIAL_TYPES.contains(gApiType);
    }

    /**
     * Returns the category a place type from the google places api belongs to.
     * @param gApiType The place type, see {@link com.google.android.gms.location.places.Place}.
     * @return The name of the category
     */
    public static PlaceType getPlaceCategory(int gApiType) {
        if(POI_TYPES.contains(gApiType)) {
            return PlaceType.POI;
        } else if(EVENT_TYPES.contains(gApiType)) {
            return PlaceType.EVENT;
        } else if(SHOPPING_TYPES.contains(gApiType)) {
            return PlaceType.SHOPPING;
        } else if(SOCIAL_TYPES.contains(gApiType)) {
            return PlaceType.SOCIAL;
        }
        return UNKNOWN;
    }

    public static List<PlaceType> getPossiblePlaceTypes() {
        return Arrays.asList(PlaceType.values());
    }

    /**
     * Returns a readable english string for the given place type
     * @param type The place type to get the string for. See {@link PlaceType}.
     * @return The readable string.
     */
    public static String getReadableString(PlaceType type) {
        String result;
        switch(type) {
            case EVENT:
                result = TEXT_EVENT;
                break;
            case POI:
                result = TEXT_POI;
                break;
            case SHOPPING:
                result = TEXT_SHOPPING;
                break;
            case SOCIAL:
                result = TEXT_SOCIAL;
                break;
            case UNKNOWN:
            default:
                result = TEXT_UNKNOWN;
                break;
        }
        return result;
    }

    /**
     * Returns a place type from the readable string.
     * @param type A readable place type string (must be supported by the framework).
     * @return Type "UNKNOWN" if the string is not supported. A type from {@link PlaceType} otherwise.
     */
    public static PlaceType getPlaceTypeFromString(String type) {
        PlaceType result;
        switch(type) {
            case TEXT_EVENT:
                result = PlaceType.EVENT;
                break;
            case TEXT_POI:
                result = PlaceType.POI;
                break;
            case TEXT_SHOPPING:
                result = PlaceType.SHOPPING;
                break;
            case TEXT_SOCIAL:
                result = PlaceType.SOCIAL;
                break;
            case TEXT_UNKNOWN:
            default:
                result = UNKNOWN;
                break;
        }
        return result;
    }


}
