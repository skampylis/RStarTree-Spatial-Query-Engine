# **RStarTree Spatial Query Engine**

## **Περιγραφή**
Πρόγραμμα σε **Java** που υλοποιεί τη δομή **R*-Tree** για αποθήκευση και αναζήτηση πολυδιάστατων χωρικών δεδομένων από το OpenStreetMap. Συγκεκριμένα, το σύστημα υποστηρίζει λειτουργίες εισαγωγής, διαγραφής, range queries, k‑nearest neighbor, skyline queries και μαζική κατασκευή δέντρου.

---

### 🚀 **Project Overview**
- **RStarTree Implementation**: Ανάπτυξη βελτιωμένης δομής για αποτελεσματική διαχείριση πολυδιάστατων δεδομένων.
- **Χωρικά Queries**: Υποστήριξη ερωτημάτων περιοχής, πλησιέστερων γειτόνων, και κορυφογραμμής.
- **Μαζική Κατασκευή**: Bottom‑up κατασκευή του δέντρου για βελτιστοποίηση της απόδοσης εισαγωγής.

---

### 🔍 **Key Features**
- **Δυναμική Επεξεργασία**: Εισαγωγή και διαγραφή εγγραφών με ευέλικτη παραμετροποίηση διαστάσεων.
- **Αποτελεσματική Αναζήτηση**: Βελτιστοποιημένη αναζήτηση μέσω spatial queries για μεγάλους όγκους δεδομένων.
- **Επεκτασιμότητα**: Υποστήριξη δεδομένων άνω των 2 διαστάσεων, επιτρέποντας εφαρμογές σε ποικίλα πεδία.

---

### 🛠️ **Technical Highlights**
- **Βάση Δεδομένων**: Ανάγνωση χωρικών δεδομένων από OpenStreetMap και αποθήκευση τους σε αρχείο δεδομένων με blocks.
- **Δομή Δείκτη**: Χρήση ξεχωριστού αρχείου για την οργάνωση του δέντρου (index file) που περιέχει τις εγγραφές.
- **Αλγόριθμοι**: Υλοποίηση αλγορίθμων για range queries, k‑NN και skyline queries, με σύγκριση αποδόσεων έναντι σειριακής αναζήτησης.

---

### 📂 **Code Structure**
- **RStarTree.java**: Κύρια κλάση για τη διαχείριση του δέντρου και την υλοποίηση βασικών λειτουργιών.
- **Insert.java & Delete.java**: Κλάσεις για την εισαγωγή και διαγραφή εγγραφών στο R*-Tree.
- **RangeQuery.java, KNNQuery.java & SkylineQuery.java**: Υλοποίηση των αντίστοιχων ερωτημάτων.
- **BottomUp.java**: Μαζική κατασκευή του δέντρου με τη μέθοδο bottom‑up.
- **FileHandler.java**: Διαχείριση αρχείων δεδομένων και index.

---

**🏷️ Tags**: `Java`, `RStarTree`, `Spatial Data`, `OpenStreetMap`, `Range Query`, `k-NN`, `Skyline Query`  
**🌟 Concept**: *"Ένα προηγμένο εργαλείο για την επεξεργασία και ανάλυση πολυδιάστατων χωρικών δεδομένων, με εφαρμογές σε γεωγραφικά συστήματα και spatial analytics."*
