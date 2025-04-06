/**
 * This class represents a record-distance pair used in the k-nearest neighbors (KNN) algorithm. The record refers to the
 * data point, and the distance is the distance between the data point and a reference point.
 */
public class KnnDistanceRecordPair {
    private final Record record;
    private final double distance;

    /**
     * Constructs a new KnnDistanceRecordPair with the specified record and distance.
     *
     * @param record the record
     * @param distance the distance
     */
    KnnDistanceRecordPair(Record record, double distance) {
        this.record = record;
        this.distance = distance;
    }

    public Record getRecord() {
        return record;
    }

    public double getDistance() {
        return distance;
    }
}
