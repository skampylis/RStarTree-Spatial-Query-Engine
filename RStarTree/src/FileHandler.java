import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FileHandler {
    private static int root = -1;
    private static int noOfDatafileBlocks = 0;
    private static int noOfIndexfileBlocks = 0;
    private static int leafLevel = -1;
    private static final String OsmfilePath = "map2.osm";
    private static final String DatafilePath = "datafile.dat";
    private static final String IndexfilePath = "indexfile.dat";
    private static int dimensions;
    private static double[][] rootMBR;
    private static final char delimiter = '$';
    private static final char blockSeparator = '#';
    private static boolean bottomUp = false;
    private static BottomUp btm = null;
    private static int blockSize = 32768; //32KB (KB=1024B) // 512 | 32768
    private static final int blockSizedatafile = 32768;
    private static ArrayList<Record> records = new ArrayList<>();
    private static Queue<Integer> emptyBlocks = new LinkedList<>();

    /**
     * This method creates a data file for the B-tree. The method first checks if the number of dimensions is at least 2.
     * If it is, it sets the dimensions of the B-tree, creates the first block of the data file, and inserts the nodes
     * into the data file. If the number of dimensions is less than 2, it prints an error message and exits the program.
     *
     * @param dimensions the number of dimensions in the B-tree
     */
    static void createDataFile(int dimensions) {
        if (dimensions >= 2) {
            FileHandler.dimensions = dimensions;
            FileHandler.createFirstDatafileBlock();
            FileHandler.insertDatafileNodes();
        } else {
            System.out.println("Dimension number should be at least 2");
            System.exit(0);
        }
    }

    /**
     * This method creates the first block of the data file for the B-tree. The method first increments the number of
     * data file blocks. It then creates byte arrays for the dimensions, block size, and the number of blocks. It creates
     * a new byte array for the block data with the size of the data file block size. It initializes a byte counter to 0.
     * It then copies the dimensions array into the block data starting from the byte counter and increments the byte
     * counter by the size of the dimensions array. It does the same for the block size array and the number of blocks
     * array. It then opens the data file in read-write mode and writes the block data to the file. If an exception
     * occurs during this process, it is caught and the stack trace is printed.
     */
    private static void createFirstDatafileBlock() {
        try {
            FileHandler.noOfDatafileBlocks++;
            byte[] dimensionArray = ConversionToBytes.intToBytes(dimensions);
            byte[] blocksizeArray = ConversionToBytes.intToBytes(blockSizedatafile);
            byte[] noOfBlocksArray = ConversionToBytes.intToBytes(noOfDatafileBlocks);
            byte[] blockData = new byte[blockSizedatafile];
            int bytecounter = 0;
            // Copies dimensionArray in blockData starting from byte counter(0) then increments by dimensionArray size.
            // Copies block sizeArray starting from byte counter(dimensionArray.length) then increments by block sizeArray
            // size etc.
            System.arraycopy(dimensionArray, 0, blockData, bytecounter, dimensionArray.length);
            bytecounter += dimensionArray.length;
            System.arraycopy(blocksizeArray, 0, blockData, bytecounter, blocksizeArray.length);
            bytecounter += blocksizeArray.length;
            System.arraycopy(noOfBlocksArray, 0, blockData, bytecounter, noOfBlocksArray.length);
            RandomAccessFile file = new RandomAccessFile(DatafilePath, "rw");
            file.write(blockData);
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method retrieves the old file information from the data file and the index file. The method first reads all
     * bytes from the data file and the index file. It then copies the dimensions and the number of data file blocks from
     * the data file bytes into byte arrays and deserializes them. It does the same for the number of index file blocks
     * and the leaf level from the index file bytes. It also retrieves the data file records and initializes the root
     * minimum bounding rectangle. If an exception occurs during this process, it is caught and the stack trace is
     * printed.
     */
    static void retrieveOldFileInfo() {
        try {
            File file = new File(DatafilePath);
            // byte arrays to save serialized data from datafile inorder to deserialize them afterward and print them
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] dimensionArray = new byte[4];
            byte[] noOfBlocksArray = new byte[4];
            System.arraycopy(bytes, 0, dimensionArray, 0, dimensionArray.length);
            System.arraycopy(bytes, 8, noOfBlocksArray, 0, noOfBlocksArray.length);
            dimensions = ByteBuffer.wrap(dimensionArray).getInt();
            noOfDatafileBlocks = ByteBuffer.wrap(noOfBlocksArray).getInt();
            file = new File(IndexfilePath);
            // byte arrays to save serialized data from index file inorder to deserialize them afterward and print them
            bytes = Files.readAllBytes(file.toPath());
            noOfBlocksArray = new byte[4];
            byte[] leafLevelArray = new byte[4];
            System.arraycopy(bytes, 4, noOfBlocksArray, 0, noOfBlocksArray.length);
            System.arraycopy(bytes, 8, leafLevelArray, 0, leafLevelArray.length);
            noOfIndexfileBlocks = ByteBuffer.wrap(noOfBlocksArray).getInt();
            leafLevel = ByteBuffer.wrap(leafLevelArray).getInt();
            records = getDatafileRecords();
            rootMBR = new double[(int) Math.pow(2, dimensions)][dimensions];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method inserts data file nodes into the data file. It first creates a DocumentBuilderFactory and
     * DocumentBuilder, and uses the builder to parse the .osm file. It then normalizes the document element to remove
     * white spaces. The method iterates over each node in the .osm file, retrieves its attributes, and adds the node to
     * a list of records to be inserted. If the node has child nodes, it checks if there is a child node with the
     * attribute "k" having the value "name:en", and if so, it retrieves the value of the "v" attribute and sets it as
     * the name of the node. Finally, it calls the datafileMassInsert method to insert the records into the data file.
     * If an exception occurs during this process, it is caught and the stack trace is printed.
     */
    private static void insertDatafileNodes() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new File(OsmfilePath));
            //get rid of white spaces
            doc.getDocumentElement().normalize();
            //node block represents the <node> we are currently processing
            Node block;
            //noOfNodes is the number of <nodes> in the .osm file
            long nodeId;
            String name;
            ArrayList<Record> recordsToInsert = new ArrayList<>();
            long noOfNodes = doc.getElementsByTagName("node").getLength();
            for (int i = 0; i < noOfNodes; i++) {
                name = "";
                ArrayList<Double> coords = new ArrayList<>();
                block = doc.getElementsByTagName("node").item(i);
                //get its attributes
                NamedNodeMap attrList = block.getAttributes();
                nodeId = Long.parseLong(attrList.getNamedItem("id").getNodeValue());
                coords.add(Double.parseDouble(attrList.getNamedItem("lat").getNodeValue()));
                coords.add(Double.parseDouble(attrList.getNamedItem("lon").getNodeValue()));
                if (block.getChildNodes().getLength() > 0) {
                    NodeList children = block.getChildNodes();
                    for (int j = 1; j < children.getLength(); j += 2) {
                        //get its attributes and check if there is one called k with the value name
                        if (children.item(j).getAttributes().getNamedItem("k").getNodeValue().equals("name:en")) {
                            //if there is, save the value of attribute v as the name
                            name = children.item(j).getAttributes().getNamedItem("v").getNodeValue();
                            break;
                        }
                    }
                }
                recordsToInsert.add(new Record(coords, nodeId, name));
            }
            Insert.datafileMassInsert(recordsToInsert);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method retrieves the data file records from the data file. It first reads all bytes from the data file. It
     * then iterates over each block after the first one, and for every node inside the data block, it copies into the
     * appropriate array, deserializes, and adds the node to a list of records. If the data block has the block separator
     * at some point, it means the end of the data read in the current block. If an exception occurs during this process,
     * it is caught and the stack trace is printed.
     *
     * @return an ArrayList of Record objects, each representing a node in the data file
     */
    static ArrayList<Record> getDatafileRecords() {
        ArrayList<Record> datafileRecords = new ArrayList<>();
        try {
            File file = new File(DatafilePath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            // byte arrays to store the serialized data from datafile
            byte[] delimiterArray = new byte[2];
            byte[] blockSeparatorArray = new byte[2];
            byte[] NodeIdArray = new byte[8];
            byte[] LatArray = new byte[8];
            byte[] LonArray = new byte[8];
            // variable to store the deserialized data
            long tempNodeId;
            double tempLat, tempLon;
            String tempName = "";
            char newlinestr;
            // for each block after the first one copy from the bytes array which contains all the bytes of the datafile
            // into the dataBlock
            for (int i = 1; i < noOfDatafileBlocks; i++) {
                byte[] dataBlock = new byte[blockSizedatafile];
                System.arraycopy(bytes, i * blockSizedatafile, dataBlock, 0, blockSizedatafile);
                int bytecounter = 0;
                boolean flag = true;
                // for every node inside the dataBlock copy into the appropriate array, deserialize and print the result
                while (flag) {
                    System.arraycopy(dataBlock, bytecounter, NodeIdArray, 0, NodeIdArray.length);
                    System.arraycopy(dataBlock, bytecounter + 8, LatArray, 0, LatArray.length);
                    System.arraycopy(dataBlock, bytecounter + 16, LonArray, 0, LonArray.length);
                    System.arraycopy(dataBlock, bytecounter + 24, delimiterArray, 0, delimiterArray.length);
                    tempNodeId = ByteBuffer.wrap(NodeIdArray).getLong();
                    tempLat = ByteBuffer.wrap(LatArray).getDouble();
                    tempLon = ByteBuffer.wrap(LonArray).getDouble();
                    newlinestr = ByteBuffer.wrap(delimiterArray).getChar();
                    int tempcounter = 0;
                    // count bytes until you meet the delimiter if not already met above
                    while (newlinestr != delimiter) {
                        tempcounter += 1;
                        System.arraycopy(dataBlock, bytecounter + 24 + tempcounter, delimiterArray, 0, delimiterArray.length);
                        newlinestr = ByteBuffer.wrap(delimiterArray).getChar();
                    }
                    // read name if there is one
                    if (tempcounter != 0) {
                        byte[] nameArray = new byte[tempcounter];
                        System.arraycopy(dataBlock, bytecounter + 24, nameArray, 0, tempcounter);
                        tempName = new String(nameArray);
                    }
                    Record record;
                    if (tempcounter != 0) {
                        record = new Record(tempLat, tempLon, i, bytecounter, datafileRecords.size(), tempName, tempNodeId);
                        bytecounter += 26 + tempcounter;
                    } else {
                        record = new Record(tempLat, tempLon, i, bytecounter, datafileRecords.size(), tempNodeId);
                        bytecounter += 26;
                    }
                    datafileRecords.add(record);
                    // if datablock has the blockSeparator (#) at some point it means the end of the data read in the
                    // current block
                    System.arraycopy(dataBlock, bytecounter, blockSeparatorArray, 0, 2);
                    if (ByteBuffer.wrap(blockSeparatorArray).getChar() == blockSeparator) flag = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datafileRecords;
    }


    /**
     * This method creates an index file. It first creates the first block of the index file. It then retrieves the data
     * file records and stores them in an ArrayList. If the boolean parameter pbp is true, it initializes the root minimum
     * bounding rectangle and inserts the nodes into the index file.
     */
    static void createIndexFile(boolean pbp) {
        FileHandler.createFirstIndexfileBlock();
        records = new ArrayList<>(getDatafileRecords());
        if (pbp) {
            rootMBR = new double[(int) Math.pow(2, dimensions)][dimensions];
            FileHandler.insertIndexfileNodes();
        }
    }

    /**
     * This method creates the first block of the index file. It first creates byte arrays for the block size, the number
     * of index file blocks, and the leaf level. It creates a new byte array for the block data with the size of the index
     * file block size. It initializes a byte counter to 0. It then copies the block size array into the block data
     * starting from the byte counter and increments the byte counter by the size of the block size array. It does the
     * same for the number of index file blocks array and the leaf level array. It then opens the index file in read-write
     * mode and writes the block data to the file. If an exception occurs during this process, it is caught and the stack
     * trace is printed.
     */
    private static void createFirstIndexfileBlock() {
        try {
            byte[] blocksizeArray = ConversionToBytes.intToBytes(blockSize);
            byte[] noOfBlocksArray = ConversionToBytes.intToBytes(noOfIndexfileBlocks);
            byte[] leafLevelArray = ConversionToBytes.intToBytes(leafLevel);
            byte[] blockData = new byte[blockSize];
            //byte counter for blockData
            int bytecounter = 0;
            // Copies block sizeArray in blockData starting from byte counter(0) then increments by
            // block sizeArray size. Copies noOfBlocksArray starting from byte counter(dimensionArray.length) then increments
            // by noOfBlocksArray size etc.
            System.arraycopy(blocksizeArray, 0, blockData, bytecounter, blocksizeArray.length);
            bytecounter += blocksizeArray.length;
            System.arraycopy(noOfBlocksArray, 0, blockData, bytecounter, noOfBlocksArray.length);
            bytecounter += noOfBlocksArray.length;
            System.arraycopy(leafLevelArray, 0, blockData, bytecounter, leafLevelArray.length);
            RandomAccessFile file = new RandomAccessFile(IndexfilePath, "rw");
            file.write(blockData);
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method inserts nodes into the index file. It first closes and opens the index file, effectively clearing the
     * file. It then iterates over each record in the records ArrayList and inserts the record into the index file. If a
     * FileNotFoundException occurs during this process, it is caught and a RuntimeException is thrown.
     */
    private static void insertIndexfileNodes() {
        try {
            new PrintWriter(IndexfilePath).close();
            for (Record record : records) {
                Insert.insert(record);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method reads the index file and writes the output to a text file. It first creates a queue of pointers and a
     * BufferedWriter to write to a text file. It then checks if the number of index file blocks is greater than or equal
     * to 1. If it is, it adds the block id to the pointers queue and enters a while loop that continues until the
     * pointers queue is empty. For each block id in the queue, it retrieves the level of the block and the rectangle
     * entries. If the level is not equal to the leaf level, it writes the block id, level, number of rectangles, leaf
     * level, and parent block id to the text file. It then writes the coordinates of each rectangle to the text file
     * and adds the child pointer of each rectangle to the pointers queue. If the level is equal to the leaf level, it
     * writes the block id, level, number of entries, and parent block id to the text file. It then writes the coordinates,
     * name, and node id of each record to the text file. After writing all the information for a block, it writes a
     * newline to the text file and removes the block id from the pointers queue. After the while loop, it closes the
     * BufferedWriter. If an exception occurs during this process, it is caught and the stack trace is printed.
     */
    public static void readIndexFile() {
        try {
            Queue<Integer> pointers = new LinkedList<>();
            BufferedWriter writer = new BufferedWriter(new FileWriter("treeOutput.txt"));
            if (FileHandler.getNoOfIndexfileBlocks() >= 1) {
                pointers.add(1);
                int blockId, level;
                while (!pointers.isEmpty()) {
                    blockId = pointers.peek();
                    level = getMetaDataOfRectangle(blockId).get(0);
                    if (level != leafLevel) {
                        ArrayList<Rectangle> rectangles = getRectangleEntries(blockId);
                        writer.write("Block No: " + blockId + ", Level: " + level + ", No of rectangles: " + rectangles.size() + ", Leaf level: " + leafLevel + ", Parent block id: " + getMetaDataOfRectangle(blockId).get(2) + "\nRecords: \n");
                        for (Rectangle rectangle : rectangles) {
                            writer.write("LAT: " + rectangle.getCoordinates().get(0) + ", " + rectangle.getCoordinates().get(dimensions) + ", LON: " + rectangle.getCoordinates().get(1) + ", " + rectangle.getCoordinates().get(1 + dimensions) + "\n");
                            pointers.add(rectangle.getChildPointer());
                        }
                    } else {
                        ArrayList<Record> records = getRecords(blockId);
                        writer.write("Block No: " + blockId + ", Level: " + level + ", No of entries: " + records.size() + ", Parent block id: " + getMetaDataOfRectangle(blockId).get(2) + "\nRecords:" + "\n");
                        for (Record record : records) {
                            writer.write("LAT: " + record.getLAT() + ", LON: " + record.getLON() + ", Datafile block: " + record.getRecordLocation().getBlock() + ", Block slot: " + record.getRecordLocation().getSlot());
                            if (record.getName() != null && !record.getName().equals("")) {
                                writer.write(", Name: " + record.getName());
                            }
                            if (record.getNodeId() != 0) {
                                writer.write(", Node ID: " + record.getId());
                            }
                            writer.write("\n");
                        }
                    }
                    writer.write("\n");
                    pointers.remove();
                }
                writer.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method calculates the maximum number of rectangles that a block can have. It first saves in metadataSize the
     * size of the number of rectangles (Integer), tree level (Integer) and parent pointer (Integer). It then increments
     * rectangleInfoSize by the size of the LAT (double) and LON (double) for each dimension plus the child-pointer
     * (Integer). Finally, it returns the number of rectangles that a block can have, which is the total block size minus
     * the size of metadata minus the left child-pointer of the first rectangle (the first rectangle is the only one with
     * two child pointers) and all mod with rectangleInfoSize.
     *
     * @return the maximum number of rectangles that a block can have
     */
    public static int calculateMaxBlockRectangles() {
        int metadataSize = 3 * Integer.BYTES;
        int rectangleInfoSize = 2 * dimensions * Double.BYTES + Integer.BYTES;
        return (blockSize - metadataSize) / rectangleInfoSize;
    }

    /**
     * This method calculates the maximum number of nodes that a block can have. It first saves in metadataSize the size
     * of the number of nodes (Integer), level (Integer) and parent pointer (Integer). It then increments nodeInfoSize
     * by the size of the LAT (double) and LON (double) of the node plus the record id (Integer) of the node in the
     * record ArrayList. Finally, it returns the number of nodes that a block can have, which is the total block size
     * minus the size of metadata.
     *
     * @return the maximum number of nodes that a block can have
     */
    public static int calculateMaxBlockNodes() {
        int metadataSize = 3 * Integer.BYTES;
        int nodeInfoSize = 2 * Double.BYTES + Integer.BYTES;
        return (blockSize - metadataSize) / nodeInfoSize;
    }

    /**
     * This method retrieves the metadata of a rectangle from the index file. It first creates a new ArrayList to store
     * the metadata. It then creates byte arrays for the level, number of entries, and parent pointer of the rectangle.
     * It opens the index file and reads all bytes from the file. It then copies the level, number of entries, and parent
     * pointer from the bytes of the file into their respective arrays. After that, it adds the deserialized values of
     * the level, number of entries, and parent pointer to the metadata ArrayList. If an exception occurs during this
     * process, it is caught and the stack trace is printed.
     *
     * @param id the id of the rectangle for which the metadata is to be retrieved
     * @return an ArrayList of Integers representing the level, number of entries, and parent pointer of the rectangle
     */
    public static ArrayList<Integer> getMetaDataOfRectangle(int id) {
        ArrayList<Integer> metadata = new ArrayList<>();
        byte[] levelArray = new byte[Integer.BYTES];
        byte[] noOfEntries = new byte[Integer.BYTES];
        byte[] parentPointer = new byte[Integer.BYTES];
        try {
            File file = new File(FileHandler.getIndexfilePath());
            byte[] bytes = Files.readAllBytes(file.toPath());
            System.arraycopy(bytes, id * blockSize, levelArray, 0, Integer.BYTES);
            System.arraycopy(bytes, id * blockSize + Integer.BYTES, noOfEntries, 0, Integer.BYTES);
            System.arraycopy(bytes, id * blockSize + 2 * Integer.BYTES, parentPointer, 0, Integer.BYTES);
            metadata.add(ByteBuffer.wrap(levelArray).getInt());
            metadata.add(ByteBuffer.wrap(noOfEntries).getInt());
            metadata.add(ByteBuffer.wrap(parentPointer).getInt());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return metadata;
    }

    /**
     * This method retrieves the entries of a rectangle from the index file. It first creates a new ArrayList to store
     * the rectangles. It then opens the index file and reads all bytes from the file. It creates a new byte array for
     * the block and copies the bytes of the file into the block array. It retrieves the number of entries in the block
     * and creates byte arrays for the minimum LAT, minimum LON, maximum LAT, maximum LON, and child pointer of each
     * entry. It enters a loop that iterates over each entry in the block. For each entry, it copies the minimum LAT,
     * minimum LON, maximum LAT, maximum LON, and child pointer from the block into their respective arrays. It then
     * creates a list of doubles for the coordinates and a Rectangle object with the coordinates and child pointer. The
     * Rectangle object is added to the ArrayList of rectangles. If an exception occurs during this process, it is caught
     * and the stack trace is printed.
     *
     * @param id the id of the rectangle for which the entries are to be retrieved
     * @return an ArrayList of Rectangle objects representing the entries of the rectangle
     */
    public static ArrayList<Rectangle> getRectangleEntries(int id) {
        ArrayList<Rectangle> rectangles = new ArrayList<>();
        try {
            File file = new File(FileHandler.getIndexfilePath());
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] block = new byte[blockSize];
            System.arraycopy(bytes, id * blockSize, block, 0, blockSize);
            byte[] currentNoOfEntries = new byte[Integer.BYTES];
            System.arraycopy(block, Integer.BYTES, currentNoOfEntries, 0, Integer.BYTES);
            int tempCurrentNoOfEntries = ByteBuffer.wrap(currentNoOfEntries).getInt();
            byte[] minLAT = new byte[Double.BYTES];
            byte[] minLON = new byte[Double.BYTES];
            byte[] maxLAT = new byte[Double.BYTES];
            byte[] maxLON = new byte[Double.BYTES];
            byte[] childPointer = new byte[Integer.BYTES];
            int byteCounter = 3 * Integer.BYTES;
            for (int i = 0; i < tempCurrentNoOfEntries; i++) {
                System.arraycopy(block, byteCounter, minLAT, 0, Double.BYTES);
                System.arraycopy(block, byteCounter + Double.BYTES, minLON, 0, Double.BYTES);
                System.arraycopy(block, byteCounter + 2 * Double.BYTES, maxLAT, 0, Double.BYTES);
                System.arraycopy(block, byteCounter + 3 * Double.BYTES, maxLON, 0, Double.BYTES);
                System.arraycopy(block, byteCounter + 4 * Double.BYTES, childPointer, 0, Integer.BYTES);
                List<Double> coordinates = List.of(ByteBuffer.wrap(minLAT).getDouble(), ByteBuffer.wrap(minLON).getDouble(), ByteBuffer.wrap(maxLAT).getDouble(), ByteBuffer.wrap(maxLON).getDouble());
                Rectangle rectangle = new Rectangle(new ArrayList<>(coordinates), ByteBuffer.wrap(childPointer).getInt());
                rectangles.add(rectangle);
                byteCounter += 4 * Double.BYTES + Integer.BYTES;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rectangles;
    }

    /**
     * This method retrieves the records from the index file. It first creates a new ArrayList to store the records. It
     * then opens the index file and reads all bytes from the file. It creates a new byte array for the block and copies
     * the bytes of the file into the block array. It retrieves the number of entries in the block and creates a byte
     * array for the record id of each entry. It enters a loop that iterates over each entry in the block. For each entry,
     * it copies the record id from the block into the record id array. It then retrieves the record corresponding to the
     * record id from the records ArrayList and adds it to the result ArrayList. If an exception occurs during this
     * process, it is caught and the stack trace is printed.
     *
     * @param id the id of the record for which the records are to be retrieved
     * @return an ArrayList of Record objects representing the records of the block
     */
    public static ArrayList<Record> getRecords(int id) {
        ArrayList<Record> result = new ArrayList<>();
        try {
            File file = new File(FileHandler.getIndexfilePath());
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] block = new byte[blockSize];
            System.arraycopy(bytes, id * blockSize, block, 0, blockSize);
            byte[] currentNoOfEntries = new byte[Integer.BYTES];
            System.arraycopy(block, Integer.BYTES, currentNoOfEntries, 0, Integer.BYTES);
            int tempCurrentNoOfEntries = ByteBuffer.wrap(currentNoOfEntries).getInt();
            byte[] recordId = new byte[Integer.BYTES];
            int byteCounter = 3 * Integer.BYTES;
            for (int i = 0; i < tempCurrentNoOfEntries; i++) {
                System.arraycopy(block, byteCounter + 2 * Double.BYTES, recordId, 0, Integer.BYTES);
                result.add(records.get(ByteBuffer.wrap(recordId).getInt()));
                byteCounter += 2 * Double.BYTES + Integer.BYTES;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getIndexfilePath() {
        return IndexfilePath;
    }

    public static String getDatafilePath() {
        return DatafilePath;
    }

    public static int getBlockSize() {
        return blockSize;
    }

    public static int getBlockSizedatafile() {
        return blockSizedatafile;
    }

    public static void setBlockSize(int newblockSize) {
        blockSize = newblockSize;
    }

    public static int getLeafLevel() {
        return leafLevel;
    }

    public static double[][] getRootMBR() {
        return rootMBR;
    }

    public static int getRoot() {
        return root;
    }

    public static void setRoot(int root) {
        FileHandler.root = root;
    }

    public static void setRootMBR(double[][] rtmbr) {
        rootMBR = rtmbr;
    }

    public static int getDimensions() {
        return dimensions;
    }

    public static int getNoOfIndexfileBlocks() {
        return noOfIndexfileBlocks;
    }

    public static void setNoOfIndexfileBlocks(int noOfIndexfileBlocks) {
        FileHandler.noOfIndexfileBlocks = noOfIndexfileBlocks;
    }

    public static void setLeafLevel(int leafLevel) {
        FileHandler.leafLevel = leafLevel;
    }

    public static Record getRecord(int id) {
        return records.get(id);
    }

    public static ArrayList<Record> getRecords() {
        return records;
    }

    public static Queue<Integer> getEmptyBlocks() {
        return emptyBlocks;
    }

    public static void setEmptyBlocks(Queue<Integer> emptyBlocks) {
        FileHandler.emptyBlocks = emptyBlocks;
    }

    public static void setDimensions(int dimensions) {
        FileHandler.dimensions = dimensions;
    }

    public static char getDelimiter() {
        return delimiter;
    }

    public static char getBlockSeparator() {
        return blockSeparator;
    }

    public static void setBottomUp(boolean bottomUp) {
        FileHandler.bottomUp = bottomUp;
    }

    public static boolean isBottomUp() {
        return bottomUp;
    }

    public static int getNoOfDatafileBlocks() {
        return noOfDatafileBlocks;
    }

    public static void setNoOfDatafileBlocks(int noOfDatafileBlocks) {
        FileHandler.noOfDatafileBlocks = noOfDatafileBlocks;
    }

    public static void setRecords(ArrayList<Record> records) {
        FileHandler.records = records;
    }

    static void setBtm(BottomUp a) {
        btm = a;
    }

    static BottomUp getBtm() {
        return btm;
    }

}