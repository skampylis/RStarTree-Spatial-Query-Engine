/**
 * This class represents a rectangle-distance pair used in the k-nearest neighbors (KNN) algorithm. The rectangle refers
 * to the data point, and the distance is the distance between the data point and a reference point.
 */
public class KnnDistanceRectanglePair {
    private final Rectangle rectangle;
    private final double distance;

    /**
     * Constructs a new KnnDistanceRectanglePair with the specified rectangle and distance.
     *
     * @param rectangle the rectangle
     * @param distance the distance
     */
    KnnDistanceRectanglePair(Rectangle rectangle, double distance) {
        this.rectangle = rectangle;
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }
}
