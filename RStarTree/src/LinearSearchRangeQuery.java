import java.util.ArrayList;

/**
 * This class represents a linear search range query. It extends the RangeQuery class.
 */
public class LinearSearchRangeQuery extends RangeQuery {

    /**
     * Constructs a new LinearSearchRangeQuery with the specified rectangle.
     *
     * @param rectangle the rectangle
     */
    LinearSearchRangeQuery(Rectangle rectangle) {
        super(rectangle);
    }

    /**
     * Executes the range query using a linear search. It checks each record in the data file to see if its coordinates
     * are within the range rectangle. If a record's coordinates are within the range rectangle, it is added to the
     * result.
     */
    protected void rangeQuery() {
        ArrayList<Record> datafileRecords = FileHandler.getDatafileRecords();
        for (Record record : datafileRecords) {
            if (record.getLAT() >= rangeRectangle.getCoordinates().get(0) && record.getLAT() <= rangeRectangle.getCoordinates().get(dimensions) && record.getLON() >= rangeRectangle.getCoordinates().get(1) && record.getLON() <= rangeRectangle.getCoordinates().get(1 + dimensions)) {
                result.add(record);
            }
        }
    }

}
