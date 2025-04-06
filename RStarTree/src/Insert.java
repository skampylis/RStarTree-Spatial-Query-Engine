import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * This class implements the insert operation for the R* Tree. It includes methods to insert a record, handle overflow,
 * and mass insert records during datafile build. It also provides a method to insert a record manually to the R* Tree.
 */
public class Insert {
    // Keep track of overflow status and overflow level
    private static boolean overflow_first_time = false;
    private static int overflowLevel = -1;

    /**
     * This method inserts a record into the R* Tree. It first calls the ChooseSubtree method to find the best block to
     * save the record. It then reads all the index file and copies the block that needs to be updated into a byte array.
     * It gets the current number of nodes inserted in the block. If there is still space in the block, it writes the
     * record to the block and updates the number of nodes. If the block is full, it calls the overflowTreatment method
     * to handle the overflow.
     *
     * @param record the record to be inserted
     */
    public static void insert(Record record) {
        // call ChooseSubtree to find the best block to save the node and save it to blockId
        int blockId = ChooseSubtree.chooseSubtree(record, 1);
        try {
            String IndexfilePath = FileHandler.getIndexfilePath();
            int blockSize = FileHandler.getBlockSize();
            // read all indexfile
            File file = new File(IndexfilePath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] block = new byte[blockSize];
            // copy only the block that we need for the Insert based on blockId
            System.arraycopy(bytes, blockId * blockSize, block, 0, blockSize);
            // get the current number of nodes inserted in the block
            byte[] treeLevelBytes = new byte[Integer.BYTES];
            byte[] currentNoOfEntries = new byte[Integer.BYTES];
            byte[] parentPointerArray = new byte[Integer.BYTES];
            System.arraycopy(block, 0, treeLevelBytes, 0, Integer.BYTES);
            System.arraycopy(block, Integer.BYTES, currentNoOfEntries, 0, Integer.BYTES);
            System.arraycopy(block, 2 * Integer.BYTES, parentPointerArray, 0, Integer.BYTES);
            int treeLevel = ByteBuffer.wrap(treeLevelBytes).getInt();
            int tempCurrentNoOfEntries = ByteBuffer.wrap(currentNoOfEntries).getInt();
            int parentPointer = ByteBuffer.wrap(parentPointerArray).getInt();
            if (tempCurrentNoOfEntries < FileHandler.calculateMaxBlockNodes()) {
                RandomAccessFile indexfile = new RandomAccessFile(IndexfilePath, "rw");
                // calculate the byte address which the node info will be written in the indexfile.
                // So, block location (blockId * blockSize), metadata size (2 * Integer.BYTES), currently added nodes
                // (tempCurrentNoOfEntries * (2 * Double.BYTES + Integer.BYTES)
                int ByteToWrite = 3 * Integer.BYTES + tempCurrentNoOfEntries * (2 * Double.BYTES + Integer.BYTES) + blockId * blockSize;
                byte[] datablock = new byte[2 * Double.BYTES + Integer.BYTES];
                System.arraycopy(ConversionToBytes.doubleToBytes(record.getLAT()), 0, datablock, 0, Double.BYTES);
                System.arraycopy(ConversionToBytes.doubleToBytes(record.getLON()), 0, datablock, Double.BYTES, Double.BYTES);
                System.arraycopy(ConversionToBytes.intToBytes(record.getId()), 0, datablock, 2 * Double.BYTES, Integer.BYTES);
                indexfile.seek(ByteToWrite);
                indexfile.write(datablock);
                if (blockId == 1)
                    Split.calculateMBRpointbypoint(FileHandler.getRootMBR(), record, tempCurrentNoOfEntries == 0, false);
                else ReadjustMBR.reAdjustRectangleBounds(blockId, parentPointer, record, false);
                tempCurrentNoOfEntries++;
                indexfile.seek((long) blockId * blockSize + Integer.BYTES);
                indexfile.write(ConversionToBytes.intToBytes(tempCurrentNoOfEntries));
                if (FileHandler.getRoot() == -1) FileHandler.setRoot(blockId);
                indexfile.close();
            } else if (tempCurrentNoOfEntries == FileHandler.calculateMaxBlockNodes()) {
                overflowTreatment(treeLevel, blockId, record);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method handles the overflow when a block is full. It first checks if the overflow level is the same as the
     * current tree level. If it is not, it sets the overflow level to the current tree level and sets the overflow flag
     * to true. If the tree level is not 0 and the overflow flag is true, it calls the Split method to reinsert the record.
     * Otherwise, it calls the Split method to split the block.
     *
     * @param treeLevel    the level of the tree
     * @param blockid      the id of the block
     * @param troublemaker the record that caused the overflow
     */

    public static void overflowTreatment(int treeLevel, int blockid, Record troublemaker) {
        if (treeLevel != overflowLevel) {
            overflow_first_time = true;
            overflowLevel = treeLevel;
        }
        if (treeLevel != 0 && overflow_first_time) {
            overflow_first_time = false;

            Split.reinsert(blockid, troublemaker);
        } else {
            Split.split(blockid, troublemaker);
            overflowLevel = -1;
        }
    }

    /**
     * This method inserts multiple records into the data file during the datafile build process. It reads all the data
     * file and copies the block that needs to be updated into a byte array. It then iterates over each record. For each
     * record, it checks if there is enough space left in the block to insert the record. If there is enough space, it
     * writes the record to the block. If there is not enough space, it writes the block to the file, updates the number
     * of blocks, and writes the block to the file again. Finally, it writes the last block to the file.
     *
     * @param records the list of records to be inserted
     */
    public static void datafileMassInsert(ArrayList<Record> records) {
        int blockSize = FileHandler.getBlockSizedatafile();
        int dimensions = FileHandler.getDimensions();
        int noOfDatafileBlocks = FileHandler.getNoOfDatafileBlocks();
        // data to save
        int byteCounter = 0;
        byte[] blockData = new byte[blockSize];
        byte[] delimiterArray = ConversionToBytes.charToBytes(FileHandler.getDelimiter());
        byte[] blockSeparatorArray = ConversionToBytes.charToBytes(FileHandler.getBlockSeparator());
        byte[] name = null;
        ArrayList<byte[]> coordsByteArrays = new ArrayList<>();
        byte[] nodeId;
        try {
            for (Record record : records) {
                coordsByteArrays.clear();
                // Adding the current byteCounter with the bytes of the incoming node in the var tempByteCounter
                int tempByteCounter = byteCounter + Long.BYTES + dimensions * Double.BYTES + delimiterArray.length + blockSeparatorArray.length;
                if (record.getName() != null) {
                    name = record.getName().getBytes(StandardCharsets.UTF_8);
                    tempByteCounter += name.length;
                }
                // If tempByteCounter is greater than blockSize then the block (blockData) gets written in the file
                // blockData is instantiated again to get empty, the byteCounter resets
                // the metadata in first block get updated
                if (tempByteCounter >= blockSize) {
                    System.arraycopy(blockSeparatorArray, 0, blockData, byteCounter, blockSeparatorArray.length);
                    RandomAccessFile file = new RandomAccessFile(FileHandler.getDatafilePath(), "rw");
                    file.seek((long) noOfDatafileBlocks * blockSize);
                    file.write(blockData);
                    noOfDatafileBlocks++;
                    FileHandler.setNoOfDatafileBlocks(FileHandler.getNoOfDatafileBlocks() + 1);
                    byteCounter = 0;
                    blockData = new byte[blockSize];
                    file.seek(8);
                    file.write(ConversionToBytes.intToBytes(noOfDatafileBlocks));
                    file.close();
                }
                nodeId = ConversionToBytes.longToBytes(record.getNodeId());
                // the arrays of serialized data get copied in the block
                System.arraycopy(nodeId, 0, blockData, byteCounter, Long.BYTES);
                byteCounter += Long.BYTES;
                for (int i = 0; i < dimensions; i++) {
                    coordsByteArrays.add(ConversionToBytes.doubleToBytes(record.getCoords().get(i)));
                    System.arraycopy(coordsByteArrays.get(i), 0, blockData, byteCounter, Double.BYTES);
                    byteCounter += Double.BYTES;
                }
                if (name != null) {
                    System.arraycopy(name, 0, blockData, byteCounter, name.length);
                    byteCounter += name.length;
                }
                System.arraycopy(delimiterArray, 0, blockData, byteCounter, delimiterArray.length);
                byteCounter += delimiterArray.length;

                name = null;
            }
            // write the last block
            System.arraycopy(blockSeparatorArray, 0, blockData, byteCounter, blockSeparatorArray.length);
            RandomAccessFile file = new RandomAccessFile(FileHandler.getDatafilePath(), "rw");
            file.seek((long) noOfDatafileBlocks * blockSize);
            file.write(blockData);
            noOfDatafileBlocks++;
            FileHandler.setNoOfDatafileBlocks(noOfDatafileBlocks);
            file.seek(8);
            file.write(ConversionToBytes.intToBytes(noOfDatafileBlocks));
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method inserts a record manually into the R* Tree and the data file. It first reads the last block in the
     * data file and copies it into a byte array. It then iterates over the block and finds the end of the block. If
     * there is enough space left in the block to insert the record, it writes the record to the block. If there is not
     * enough space, it writes the block to the file, updates the number of blocks, and writes the block to the file
     * again. Finally, it writes the last block to the file and inserts the record into the R* Tree.
     *
     * @param record the record to be inserted
     */
    public static void datafileRecordInsert(Record record) {
        int blockSize = FileHandler.getBlockSizedatafile();
        int dimensions = FileHandler.getDimensions();
        int noOfDatafileBlocks = FileHandler.getNoOfDatafileBlocks();
        // data to save
        int byteCounter = 0;
        byte[] dataBlock = new byte[blockSize];
        File datafile = new File(FileHandler.getDatafilePath());
        byte[] delimiterArray = ConversionToBytes.charToBytes(FileHandler.getDelimiter());
        byte[] blockSeparatorArray = ConversionToBytes.charToBytes(FileHandler.getBlockSeparator());
        char newlinestr;
        byte[] name = null;
        ArrayList<byte[]> coordsByteArrays = new ArrayList<>();
        byte[] nodeId;
        try {
            byte[] bytes = Files.readAllBytes(datafile.toPath());
            System.arraycopy(bytes, (noOfDatafileBlocks - 1) * blockSize, dataBlock, 0, blockSize);
            boolean flag = true;
            while (flag) {
                System.arraycopy(dataBlock, byteCounter + 24, delimiterArray, 0, delimiterArray.length);
                newlinestr = ByteBuffer.wrap(delimiterArray).getChar();
                int tempcounter = 0;
                // count bytes until you meet the delimiter if not already met above
                while (newlinestr != FileHandler.getDelimiter()) {
                    tempcounter += 1;
                    System.arraycopy(dataBlock, byteCounter + 24 + tempcounter, delimiterArray, 0, delimiterArray.length);
                    newlinestr = ByteBuffer.wrap(delimiterArray).getChar();
                }
                if (tempcounter != 0) {
                    byteCounter += 26 + tempcounter;
                } else {
                    byteCounter += 26;
                }
                // if datablock has the blockSeparator (#) at some point it means the end of the data read in the
                // current block
                System.arraycopy(dataBlock, byteCounter, blockSeparatorArray, 0, 2);
                if (ByteBuffer.wrap(blockSeparatorArray).getChar() == FileHandler.getBlockSeparator()) flag = false;
            }
            // Adding the current byteCounter with the bytes of the incoming node in the var tempByteCounter
            int tempByteCounter = byteCounter + Long.BYTES + dimensions * Double.BYTES + delimiterArray.length + blockSeparatorArray.length;
            if (record.getName() != null) {
                name = record.getName().getBytes(StandardCharsets.UTF_8);
                tempByteCounter += name.length;
            }
            if (tempByteCounter >= blockSize) {
                // byteCounter += blockSeparatorArray.length;
                RandomAccessFile file = new RandomAccessFile(FileHandler.getDatafilePath(), "rw");
                noOfDatafileBlocks++;
                FileHandler.setNoOfDatafileBlocks(FileHandler.getNoOfDatafileBlocks() + 1);
                byteCounter = 0;
                dataBlock = new byte[blockSize];
                file.seek(8);
                file.write(ConversionToBytes.intToBytes(noOfDatafileBlocks));
                file.close();
            }
            tempByteCounter = byteCounter;
            nodeId = ConversionToBytes.longToBytes(record.getNodeId());
            System.arraycopy(nodeId, 0, dataBlock, byteCounter, Long.BYTES);
            byteCounter += Long.BYTES;
            for (int i = 0; i < dimensions; i++) {
                coordsByteArrays.add(ConversionToBytes.doubleToBytes(record.getCoords().get(i)));
                System.arraycopy(coordsByteArrays.get(i), 0, dataBlock, byteCounter, Double.BYTES);
                byteCounter += Double.BYTES;
            }

            if (name != null) {
                System.arraycopy(name, 0, dataBlock, byteCounter, name.length);
                byteCounter += name.length;
            }
            System.arraycopy(delimiterArray, 0, dataBlock, byteCounter, delimiterArray.length);
            byteCounter += delimiterArray.length;
            System.arraycopy(blockSeparatorArray, 0, dataBlock, byteCounter, blockSeparatorArray.length);
            RandomAccessFile file1 = new RandomAccessFile(FileHandler.getDatafilePath(), "rw");
            file1.seek((long) (noOfDatafileBlocks - 1) * blockSize);
            file1.write(dataBlock);
            file1.close();
            FileHandler.setRecords(FileHandler.getDatafileRecords());
            FileHandler.setRoot(0);
            if (name == null) {
                insert(new Record(record.getCoords().get(0), record.getCoords().get(1), noOfDatafileBlocks, tempByteCounter, FileHandler.getDatafileRecords().size() - 1, record.getNodeId()));

            } else {
                insert(new Record(record.getCoords().get(0), record.getCoords().get(1), noOfDatafileBlocks, tempByteCounter, FileHandler.getDatafileRecords().size() - 1, record.getName(), record.getNodeId()));

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
