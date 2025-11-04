# **RStarTree Spatial Query Engine**

## **Description**

A **Java** program that implements the **R*-Tree** structure for storing and querying multidimensional spatial data from OpenStreetMap. Specifically, the system supports insertion, deletion, range queries, k-nearest neighbor, skyline queries, and bulk tree construction.

---

### 🚀 **Project Overview**

* **RStarTree Implementation**: Development of an optimized structure for efficient management of multidimensional data.
* **Spatial Queries**: Supports range, nearest neighbor, and skyline queries.
* **Bulk Construction**: Bottom-up tree building for optimized insertion performance.

---

### 🔍 **Key Features**

* **Dynamic Processing**: Insert and delete records with flexible dimensional configuration.
* **Efficient Search**: Optimized spatial query performance for large-scale datasets.
* **Scalability**: Supports data with more than two dimensions, enabling applications across diverse domains.

---

### 🛠️ **Technical Highlights**

* **Database Integration**: Reads spatial data from OpenStreetMap and stores it in a block-based data file.
* **Index Structure**: Uses a separate index file for organizing the tree and managing entries.
* **Algorithms**: Implements range, k-NN, and skyline query algorithms with performance comparison against sequential search.

---

### 📂 **Code Structure**

* **RStarTree.java**: Core class managing the tree and implementing primary operations.
* **Insert.java & Delete.java**: Handle record insertion and deletion in the R*-Tree.
* **RangeQuery.java, KNNQuery.java & SkylineQuery.java**: Implement the respective spatial queries.
* **BottomUp.java**: Performs bulk tree construction using the bottom-up method.
* **FileHandler.java**: Manages data and index files.

---

**🏷️ Tags**: `Java`, `RStarTree`, `Spatial Data`, `OpenStreetMap`, `Range Query`, `k-NN`, `Skyline Query`
**🌟 Concept**: *"An advanced tool for the processing and analysis of multidimensional spatial data, with applications in geographic systems and spatial analytics."*
