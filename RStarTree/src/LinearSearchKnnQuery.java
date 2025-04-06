import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a linear search KNN query.
 * It extends the KnnQuery class.
 */
public class LinearSearchKnnQuery extends KnnQuery {

    /**
     * Constructs a new LinearSearchKnnQuery with the specified k and coordinates.
     *
     * @param k           the number of nearest neighbors to find
     * @param coordinates the coordinates of the point
     */
    LinearSearchKnnQuery(int k, ArrayList<Double> coordinates) {
        super(k, coordinates);
    }

    /**
     * Executes the KNN query using a linear search. It calculates the distance between the point and each record in the
     * data file, and if the distance is less than the distance of the furthest known neighbor, it adds the record to the
     * KNN queue.
     */
    protected void knnQuery() {
        ArrayList<Record> records = FileHandler.getDatafileRecords();
        for (Record record : records) {
            List<Double> recordCoords = List.of(record.getLAT(), record.getLON());
            double distance = calcDistBetweenPoints(new ArrayList<>(recordCoords), coordinates);
            if (distance > 0) {
                KnnDistanceRecordPair pair = new KnnDistanceRecordPair(record, distance);
                knn.add(pair);
                if (knn.size() > k) {
                    knn.poll();
                }
            }
        }
    }

}
