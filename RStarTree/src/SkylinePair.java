import java.util.ArrayList;

/**
 * This class represents a pair of skyline in a spatial database. Each pair is defined by a set of coordinates and an ID.
 *
 */
public class SkylinePair {
    private int id;
    private ArrayList<Double> coordinates;

    /**
     * Constructs a new skyline pair with the given ID and coordinates.
     *
     * @param id the ID of the skyline pair
     * @param coordinates the coordinates of the skyline pair
     */
    SkylinePair(int id, ArrayList<Double> coordinates) {
        this.id = id;
        this.coordinates = coordinates;
    }

    public int getId() {
        return id;
    }

    public ArrayList<Double> getCoordinates() {
        return coordinates;
    }
}
