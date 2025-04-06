import java.util.*;

/**
 * This class represents a K-Nearest Neighbors (KNN) query. It uses a priority queue to keep track of the KNN and a
 * priority queue to keep track of the pointers.
 */
public class KnnQuery {
    protected final PriorityQueue<KnnDistanceRecordPair> knn;
    private final PriorityQueue<KnnDistanceRectanglePair> pointers;
    protected final int dimensions, k;
    protected final ArrayList<Double> coordinates;

    /**
     * Constructs a new KnnQuery with the specified k and coordinates.
     *
     * @param k the number of nearest neighbors to find
     * @param coordinates the coordinates of the point
     */
    KnnQuery(int k, ArrayList<Double> coordinates) {
        this.coordinates = new ArrayList<>(coordinates);
        dimensions = FileHandler.getDimensions();
        this.k = k;
        knn = new PriorityQueue<>(k, (o1, o2) -> Double.compare(o2.getDistance(), o1.getDistance()));
        pointers = new PriorityQueue<>(Comparator.comparingDouble(KnnDistanceRectanglePair::getDistance));
        this.knnQuery();
    }

    /**
     * Executes the KNN query. It calculates the distance between the point and each rectangle in the file and if the
     * distance is less than the distance of the furthest known neighbor, it adds the record to the KNN queue.
     */
    protected void knnQuery() {
        try {
            if (FileHandler.getNoOfIndexfileBlocks() > 1) {
                int blockId = 1, level;

                do {
                    level = FileHandler.getMetaDataOfRectangle(blockId).get(0);
                    if (level != FileHandler.getLeafLevel()) {
                        ArrayList<Rectangle> rectangles = FileHandler.getRectangleEntries(blockId);
                        for (Rectangle rectangle : rectangles) {
                            double distance = calcDistBetweenPointAndRectangle(coordinates, rectangle);
                            if (!knn.isEmpty() && knn.size() == k && distance >= knn.peek().getDistance()) {
                                continue;
                            }
                            KnnDistanceRectanglePair pair = new KnnDistanceRectanglePair(rectangle, distance);
                            pointers.add(pair);
                        }
                    } else {
                        boolean condition = blockId == 1 || knn.isEmpty() || (pointers.peek() != null && knn.peek() != null && calcDistBetweenPointAndRectangle(coordinates, pointers.peek().getRectangle()) < Objects.requireNonNull(knn.peek()).getDistance());
                        if (condition) {
                            ArrayList<Record> records = FileHandler.getRecords(blockId);
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
                    if (blockId != 1) {
                        pointers.remove();
                    }
                    if (pointers.peek() != null) {
                        blockId = pointers.peek().getRectangle().getChildPointer();
                    }
                } while (!pointers.isEmpty());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates the distance between two points.
     *
     * @param firstCoords the coordinates of the first point
     * @param secondCoords the coordinates of the second point
     * @return the distance between the two points
     */
    protected double calcDistBetweenPoints(ArrayList<Double> firstCoords, ArrayList<Double> secondCoords) {
        ArrayList<Double> distances = new ArrayList<>();
        double squareSum = 0.0;
        for (int i = 0; i < firstCoords.size(); i++) {
            distances.add(Math.abs(secondCoords.get(i) - firstCoords.get(i)));
            squareSum += distances.get(i) * distances.get(i);
        }
        return Math.sqrt(squareSum);
    }

    /**
     * Calculates the distance between a point and a rectangle.
     *
     * @param coordinates the coordinates of the point
     * @param rectangle the rectangle
     * @return the distance between the point and the rectangle
     */
    private double calcDistBetweenPointAndRectangle(ArrayList<Double> coordinates, Rectangle rectangle) {
        ArrayList<Double> distances = new ArrayList<>();
        double squareSum = 0.0;
        for (int i = 0; i < dimensions; i++) {
            distances.add(Math.max(rectangle.getCoordinates().get(i) - coordinates.get(i), Math.max(0, coordinates.get(i) - rectangle.getCoordinates().get(i + dimensions))));
            squareSum += distances.get(i) * distances.get(i);
        }
        return Math.sqrt(squareSum);
    }

    /**
     * Prints the KNN.
     */
    void print() {
        ArrayList<KnnDistanceRecordPair> pairs = new ArrayList<>(knn);
        Collections.reverse(pairs);
        System.out.println("The " + k + " nearest neighbors are: ");
        for (int i = 0; i < pairs.size(); i++) {
            System.out.print(i + 1 + ")" + " Distance: " + pairs.get(i).getDistance() + ", LAT: " + pairs.get(i).getRecord().getLAT() + ", LON: " + pairs.get(i).getRecord().getLON() + ", Datafile block: " + pairs.get(i).getRecord().getRecordLocation().getBlock() + ", Block slot: " + pairs.get(i).getRecord().getRecordLocation().getSlot());
            if (pairs.get(i).getRecord().getName() != null && !pairs.get(i).getRecord().getName().equals("")) {
                System.out.print(", Name: " + pairs.get(i).getRecord().getName());
            }
            if (pairs.get(i).getRecord().getNodeId() != 0) {
                System.out.print(", Node ID: " + pairs.get(i).getRecord().getNodeId());
            }
            System.out.println();
        }
    }
}
