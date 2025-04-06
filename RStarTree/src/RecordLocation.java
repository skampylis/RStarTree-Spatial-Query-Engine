/**
 * This class represents the location of a record in a spatial database. Each location is defined by a block number and
 * a slot number.
 *
 */
public class RecordLocation {
    private final int block;
    private final long slot;

    /**
     * Constructs a new record location with the given block number and slot number.
     *
     * @param block the block number of the record location
     * @param slot the slot number of the record location
     */
    public RecordLocation(int block, long slot) {
        this.block = block;
        this.slot = slot;
    }

    public int getBlock() {
        return this.block;
    }

    public long getSlot() {
        return this.slot;
    }

}
