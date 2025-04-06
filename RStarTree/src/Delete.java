import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This class provides methods for deleting a point from a tree. The tree is represented using a B-tree structure.
 * The delete method is the main method for deleting a point from the tree. It takes the latitude and longitude of the
 * point to be deleted as arguments. The deletePoint method is a helper method that deletes the point from the tree.
 * If the point is successfully deleted, it returns true; otherwise, it returns false. The deletePointFromBlock method
 * is another helper method that deletes the point from a specific block in the tree.
 */
public class Delete {
    private static final int minEntries = FileHandler.calculateMaxBlockNodes() * 40 / 100; // 670 | FileHandler.calculateMaxBlockNodes() * 40 / 100 | 969

    /**
     * This method deletes a point from the tree.
     *
     * @param LAT the latitude of the point to be deleted
     * @param LON the longitude of the point to be deleted
     */
    public static void delete(double LAT, double LON) {
        boolean result = deletePoint(LAT, LON);
        if (result) {
            System.out.println("The node with LAT: " + LAT + ", and LON: " + LON + ", was successfully deleted.");
        } else {
            System.out.println("The node with the given coordinates didn't get found.");
        }
    }

    /**
     * This method deletes a point from the tree.
     *
     * @param LAT the latitude of the point to be deleted
     * @param LON the longitude of the point to be deleted
     * @return true if the point was successfully deleted, false otherwise
     */
    private static boolean deletePoint(double LAT, double LON) {
        try {
            int leafLevel = FileHandler.getLeafLevel();
            Queue<Integer> pointers = new LinkedList<>();
            if (FileHandler.getNoOfIndexfileBlocks() >= 1) {
                pointers.add(1);
                int level;
                while (!pointers.isEmpty()) {
                    int blockId = pointers.peek();
                    level = FileHandler.getMetaDataOfRectangle(blockId).get(0);
                    if (level != leafLevel) {
                        ArrayList<Rectangle> rectangles = FileHandler.getRectangleEntries(blockId);
                        for (Rectangle rectangle : rectangles) {
                            pointers.add(rectangle.getChildPointer());
                        }
                    } else {
                        ArrayList<Record> records = FileHandler.getRecords(blockId);
                        for (Record record : records) {
                            if (LAT == record.getLAT() && LON == record.getLON()) {
                                if (deletePointFromBlock(LAT, LON, blockId)) {
                                    return true;
                                }
                            }
                        }
                    }
                    pointers.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * This method deletes a point from a specific block in the tree.
     *
     * @param LAT     the latitude of the point to be deleted
     * @param LON     the longitude of the point to be deleted
     * @param blockId the id of the block from which the point will be deleted
     * @return true if the point was successfully deleted, false otherwise
     */
    private static boolean deletePointFromBlock(double LAT, double LON, int blockId) {
        try {
            int blockSize = FileHandler.getBlockSize();
            int leafLevel = FileHandler.getLeafLevel();
            String IndexfilePath = FileHandler.getIndexfilePath();
            File file = new File(IndexfilePath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] dataBlock = new byte[blockSize];
            System.arraycopy(bytes, blockId * blockSize, dataBlock, 0, blockSize);
            byte[] noOfEntries = new byte[Integer.BYTES];
            byte[] parentPointer = new byte[Integer.BYTES];
            System.arraycopy(dataBlock, Integer.BYTES, noOfEntries, 0, Integer.BYTES);
            System.arraycopy(dataBlock, 2 * Integer.BYTES, parentPointer, 0, Integer.BYTES);
            int tempNoOfEntries = ByteBuffer.wrap(noOfEntries).getInt();
            int tempParentPointer = ByteBuffer.wrap(parentPointer).getInt();
            // NoOfEntries + block level + parent pointer
            int bytecounter = 3 * Integer.BYTES;
            byte[] LatArray = new byte[Double.BYTES];
            byte[] LonArray = new byte[Double.BYTES];
            for (int i = 0; i < tempNoOfEntries; i++) {
                System.arraycopy(dataBlock, bytecounter, LatArray, 0, Double.BYTES);
                System.arraycopy(dataBlock, bytecounter + Double.BYTES, LonArray, 0, Double.BYTES);
                if (LAT == ByteBuffer.wrap(LatArray).getDouble() && LON == ByteBuffer.wrap(LonArray).getDouble()) {
                    int tempBytecounter = bytecounter + (tempNoOfEntries - i - 1) * (2 * Double.BYTES + Integer.BYTES);
                    // copy the data from the last entry into the space of the entry that gets deleted
                    System.arraycopy(dataBlock, tempBytecounter, dataBlock, bytecounter, Double.BYTES);
                    System.arraycopy(dataBlock, tempBytecounter + Double.BYTES, dataBlock, bytecounter + Double.BYTES, Double.BYTES);
                    System.arraycopy(dataBlock, tempBytecounter + 2 * Double.BYTES, dataBlock, bytecounter + 2 * Double.BYTES, Integer.BYTES);
                    // empty the data from the entry copied
                    System.arraycopy(new byte[Double.BYTES], 0, dataBlock, tempBytecounter, Double.BYTES);
                    System.arraycopy(new byte[Double.BYTES], 0, dataBlock, tempBytecounter + Double.BYTES, Double.BYTES);
                    System.arraycopy(new byte[Integer.BYTES], 0, dataBlock, tempBytecounter + 2 * Double.BYTES, Integer.BYTES);
                    // decrease the noOfEntries
                    tempNoOfEntries--;
                    System.arraycopy(ConversionToBytes.intToBytes(tempNoOfEntries), 0, dataBlock, Integer.BYTES, Integer.BYTES);
                    RandomAccessFile indexfile = new RandomAccessFile(IndexfilePath, "rw");
                    indexfile.seek((long) blockSize * blockId);
                    indexfile.write(dataBlock);
                    indexfile.close();
                    // If the entry was not in the root and the minimum number of entries wasn't reached, the
                    // rectangle bounds are readjusted
                    // else delete the rectangle and reinsert the nodes
                    if (blockId != 1 && tempNoOfEntries >= minEntries) {
                        ReAdjustRectangleBounds.reAdjustRectangleBounds(blockId, tempParentPointer);
                    } else if (blockId != 1) {
                        byte[] idArray = new byte[Integer.BYTES];
                        // Arraylist that will hold the nodes' data of the rectangle that will be deleted
                        ArrayList<Record> nodesToReInsert = new ArrayList<>();
                        // Start from the first node after the metadata
                        // Loop until the number of nodes is reached
                        // Increment by the size of each node
                        for (int j = 3 * Integer.BYTES; j < tempNoOfEntries * (2 * Double.BYTES + Integer.BYTES) + 3 * Integer.BYTES; j += 2 * Double.BYTES + Integer.BYTES) {
                            System.arraycopy(dataBlock, j, LatArray, 0, Double.BYTES);
                            System.arraycopy(dataBlock, j + Double.BYTES, LonArray, 0, Double.BYTES);
                            System.arraycopy(dataBlock, j + 2 * Double.BYTES, idArray, 0, Integer.BYTES);
                            Record record = new Record(ByteBuffer.wrap(LatArray).getDouble(), ByteBuffer.wrap(LonArray).getDouble(), ByteBuffer.wrap(idArray).getInt());
                            nodesToReInsert.add(record);
                        }
                        // delete the block
                        System.arraycopy(new byte[blockSize], 0, dataBlock, 0, blockSize);
                        indexfile = new RandomAccessFile(IndexfilePath, "rw");
                        indexfile.seek((long) blockSize * blockId);
                        indexfile.write(dataBlock);
                        indexfile.close();
                        ReAdjustRectangleBounds.reAdjustRectangleBounds(blockId, tempParentPointer);
                        // Reinsert the entries from the deleted rectangle
                        for (Record record : nodesToReInsert) {
                            Insert.insert(record);
                        }
                    }
                    return true;
                }
                bytecounter += 2 * Double.BYTES + Integer.BYTES;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}