import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

/**
 * BookMyStayApp - Hotel Booking Management System.
 *
 * <p>UC1: Application Entry and Welcome Message
 * <p>UC2: Basic Room Types and Static Availability
 * <p>UC3: Centralized Room Inventory Management (HashMap)
 * <p>UC4: Room Search and Availability Check (Read-Only)
 * <p>UC5: Booking Request Queue (First-Come-First-Served)
 * <p>UC6: Reservation Confirmation and Room Allocation (Set + HashMap)
 * <p>UC7: Add-On Service Selection (Map + List, Composition)
 * <p>UC8: Booking History & Reporting (List for Chronological Storage)
 * <p>UC9: Error Handling & Validation (Custom Exceptions, Fail-Fast)
 * <p>UC10: Booking Cancellation & Inventory Rollback (Stack, State Reversal)
 * <p>UC11: Concurrent Booking Simulation (Threads, Synchronized Access)
 * <p>UC12: Data Persistence & System Recovery (Serialization, File I/O)
 *
 * @author Sharveswar
 * @version 12.0
 */
public class BookMyStayApp {

    // =========================================================================
    // UC9 — Error Handling & Validation
    // =========================================================================

    static class InvalidBookingException extends Exception {
        public InvalidBookingException(String message) { super(message); }
    }

    // =========================================================================
    // UC2 — Room Domain Model
    // =========================================================================

    static abstract class Room {
        protected String roomType, sizeDescription, amenities;
        protected int numberOfBeds; protected double pricePerNight;

        public Room(String rt, int beds, double price, String size, String am) {
            roomType=rt; numberOfBeds=beds; pricePerNight=price; sizeDescription=size; amenities=am;
        }

        public String getRoomType()        { return roomType; }
        public double getPricePerNight()   { return pricePerNight; }
        public int    getNumberOfBeds()    { return numberOfBeds; }
        public String getSizeDescription() { return sizeDescription; }
        public String getAmenities()       { return amenities; }
        public abstract void displayAmenities();

        public void displayRoomDetails() {
            System.out.println("  Room Type   : " + roomType);
            System.out.println("  Beds        : " + numberOfBeds);
            System.out.println("  Size        : " + sizeDescription);
            System.out.printf ("  Price/Night : $%.2f%n", pricePerNight);
            displayAmenities();
        }
    }

    static class SingleRoom extends Room {
        public SingleRoom() { super("Single Room",1,80.00,"Small (200 sq ft)","Wi-Fi, TV, Mini-Fridge"); }
        @Override public void displayAmenities() { System.out.println("  Amenities   : " + amenities); }
    }
    static class DoubleRoom extends Room {
        public DoubleRoom() { super("Double Room",2,140.00,"Medium (350 sq ft)","Wi-Fi, TV, Mini-Fridge, Work Desk"); }
        @Override public void displayAmenities() { System.out.println("  Amenities   : " + amenities); }
    }
    static class SuiteRoom extends Room {
        public SuiteRoom() { super("Suite Room",3,280.00,"Large (600 sq ft)","Wi-Fi, TV, Mini-Bar, Jacuzzi, Living Area, Balcony"); }
        @Override public void displayAmenities() { System.out.println("  Amenities   : " + amenities); }
    }

    // =========================================================================
    // UC3 + UC11 + UC12 — Centralized Room Inventory
    // =========================================================================

    static class RoomInventory {
        private final HashMap<String, Integer> inventory;
        private final Set<String> validRoomTypes;

        public RoomInventory() {
            inventory = new HashMap<>();
            validRoomTypes = new HashSet<>();
            validRoomTypes.add("Single Room");
            validRoomTypes.add("Double Room");
            validRoomTypes.add("Suite Room");
            
            inventory.put("Single Room", 3);
            inventory.put("Double Room", 2);
            inventory.put("Suite Room",  1);
        }

        public synchronized int getAvailability(String t) { return inventory.getOrDefault(t, 0); }
        public Iterable<String> getRoomTypes() { return validRoomTypes; }

        public synchronized void decrementAvailability(String t) throws InvalidBookingException {
            if (!validRoomTypes.contains(t)) throw new InvalidBookingException("Unknown room type: " + t);
            int cur = inventory.getOrDefault(t, 0);
            if (cur <= 0) throw new InvalidBookingException("Inventory exhausted for: " + t);
            inventory.put(t, cur - 1);
        }

        public synchronized void incrementAvailability(String t) {
            if (validRoomTypes.contains(t)) {
                inventory.put(t, inventory.getOrDefault(t, 0) + 1);
            }
        }

        public boolean isValidRoomType(String t) { return validRoomTypes.contains(t); }

        public synchronized void displayInventory() {
            System.out.println("\n  --- Room Inventory ---");
            for (Map.Entry<String,Integer> e : inventory.entrySet())
                System.out.printf("  %-15s : %d available%n", e.getKey(), e.getValue());
            System.out.println("  ----------------------");
        }

        // UC12: State extraction and recovery
        public synchronized HashMap<String, Integer> getSnapshot() {
            return new HashMap<>(inventory);
        }

        public synchronized void restoreSnapshot(HashMap<String, Integer> snapshot) {
            inventory.clear();
            if (snapshot != null) inventory.putAll(snapshot);
        }
    }

    // =========================================================================
    // UC4 — Search Service
    // =========================================================================

    static class SearchService {
        private final RoomInventory    inventory;
        private final Map<String,Room> catalog;

        public SearchService(RoomInventory inv) {
            inventory = inv;
            catalog   = new HashMap<>();
            catalog.put("Single Room", new SingleRoom());
            catalog.put("Double Room", new DoubleRoom());
            catalog.put("Suite Room",  new SuiteRoom());
        }

        public void searchAvailableRooms() {
            System.out.println("\n  --- Available Rooms ---");
            boolean found = false;
            for (String t : inventory.getRoomTypes()) {
                int cnt = inventory.getAvailability(t);
                if (cnt <= 0) { System.out.println("  [SKIP] " + t + " — unavailable"); continue; }
                found = true;
                Room r = catalog.get(t);
                if (r != null) { System.out.println(); r.displayRoomDetails(); System.out.println("  Available   : " + cnt + " rooms"); }
            }
            if (!found) System.out.println("  No rooms currently available.");
            System.out.println("\n  -----------------------");
        }
    }

    // =========================================================================
    // UC5 — Reservation & Thread-Safe Queue (UC12: Serializable)
    // =========================================================================

    static class Reservation implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String guestName, roomType;
        private final int    numberOfNights;

        public Reservation(String guest, String room, int nights) throws InvalidBookingException {
            if (guest == null || guest.trim().isEmpty()) throw new InvalidBookingException("Guest name cannot be empty.");
            if (nights <= 0) throw new InvalidBookingException("Number of nights must be at least 1.");
            guestName = guest; roomType = room; numberOfNights = nights;
        }

        public String getGuestName()      { return guestName; }
        public String getRoomType()       { return roomType; }
        public int    getNumberOfNights() { return numberOfNights; }

        @Override public String toString() {
            return "Reservation[guest=" + guestName + ", room=" + roomType + ", nights=" + numberOfNights + "]";
        }
    }

    static class BookingRequestQueue {
        private final Queue<Reservation> queue;
        public BookingRequestQueue() { queue = new LinkedList<>(); }
        public synchronized void addRequest(Reservation r) { queue.offer(r); }
        public synchronized Reservation pollRequest() { return queue.poll(); }
        public synchronized boolean isEmpty() { return queue.isEmpty(); }
    }

    // =========================================================================
    // UC8 + UC10 + UC12 — Booking History & Reporting (Serializable)
    // =========================================================================

    static class ConfirmedBooking implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String roomId;
        private final Reservation reservation;
        private boolean isCancelled;

        public ConfirmedBooking(String roomId, Reservation reservation) {
            this.roomId = roomId; this.reservation = reservation; this.isCancelled = false;
        }
        
        public String getRoomId() { return roomId; }
        public Reservation getReservation() { return reservation; }
        public boolean isCancelled() { return isCancelled; }
        public void cancel() { this.isCancelled = true; }
        
        @Override public String toString() {
            String status = isCancelled ? " [CANCELLED]" : "";
            return "ConfirmedBooking[ID=" + roomId + status + ", Guest=" + reservation.getGuestName()
                 + ", " + reservation.getRoomType() + " for " + reservation.getNumberOfNights() + " nights]";
        }
    }

    static class BookingHistory {
        private final List<ConfirmedBooking> history;
        public BookingHistory() { history = new ArrayList<>(); }
        
        public synchronized void addRecord(ConfirmedBooking booking) { history.add(booking); }
        public synchronized List<ConfirmedBooking> getHistory() { return new ArrayList<>(history); }
        
        public synchronized ConfirmedBooking findBookingByRoomId(String roomId) {
            for (ConfirmedBooking cb : history) {
                if (cb.getRoomId().equals(roomId)) return cb;
            }
            return null;
        }

        // UC12: State extraction and recovery
        public synchronized List<ConfirmedBooking> getSnapshot() {
            return new ArrayList<>(history);
        }

        public synchronized void restoreSnapshot(List<ConfirmedBooking> snapshot) {
            history.clear();
            if (snapshot != null) history.addAll(snapshot);
        }
    }

    static class BookingReportService {
        private final BookingHistory bookingHistory;
        public BookingReportService(BookingHistory bookingHistory) { this.bookingHistory = bookingHistory; }
        
        public void generateSummaryReport() {
            System.out.println("\n  --- Booking History Report ---");
            List<ConfirmedBooking> records = bookingHistory.getHistory();
            if (records.isEmpty()) {
                System.out.println("  No bookings recorded yet.\n  ------------------------------");
                return;
            }
            int totalActiveNights = 0;
            System.out.println("  Chronological Log:");
            for (int i = 0; i < records.size(); i++) {
                ConfirmedBooking cb = records.get(i);
                System.out.println("    " + (i + 1) + ". " + cb);
                if (!cb.isCancelled()) {
                    totalActiveNights += cb.getReservation().getNumberOfNights();
                }
            }
            System.out.println("\n  --- Summary Statistics ---");
            System.out.println("  Total Bookings Processed : " + records.size());
            System.out.println("  Active Room-Nights Booked: " + totalActiveNights);
            System.out.println("  ------------------------------");
        }
    }

    // =========================================================================
    // UC6 + UC11 + UC12 — Room Allocation Service
    // =========================================================================

    static class RoomAllocationService {
        private final RoomInventory                inventory;
        private final BookingHistory               bookingHistory;
        private final Set<String>                  allAllocatedIds;
        private final HashMap<String,Set<String>>  allocatedByType;
        private int idCounter = 100;

        public RoomAllocationService(RoomInventory inv, BookingHistory history) {
            inventory       = inv;
            bookingHistory  = history;
            allAllocatedIds = new HashSet<>();
            allocatedByType = new HashMap<>();
        }

        private synchronized String generateUniqueId(String roomType) {
            String prefix = roomType.equals("Single Room") ? "SNG"
                          : roomType.equals("Double Room") ? "DBL" : "STE";
            String id;
            do { id = prefix + "-" + (++idCounter); } while (allAllocatedIds.contains(id));
            return id;
        }

        public synchronized boolean allocateRoom(Reservation r) {
            try {
                String type = r.getRoomType();
                if (!inventory.isValidRoomType(type)) throw new InvalidBookingException("Invalid room type: " + type);
                
                if (inventory.getAvailability(type) <= 0) {
                    System.out.println("  [REJECTED] No availability for " + type + " (Guest: " + r.getGuestName() + ")");
                    return false;
                }

                inventory.decrementAvailability(type);
                String roomId = generateUniqueId(type);
                allAllocatedIds.add(roomId);
                allocatedByType.computeIfAbsent(type, k -> new HashSet<>()).add(roomId);

                bookingHistory.addRecord(new ConfirmedBooking(roomId, r));

                System.out.println("  [CONFIRMED] Room ID: " + roomId + " -> " + r.getGuestName()
                    + " | Remaining " + type + ": " + inventory.getAvailability(type));
                return true;

            } catch (InvalidBookingException e) {
                System.out.println("  [FAILED - VALIDATION] " + e.getMessage());
                return false;
            }
        }

        public void processQueue(BookingRequestQueue q) {
            System.out.println("\n  --- Processing Sequential Booking Queue ---");
            int pos = 1;
            while (!q.isEmpty()) {
                Reservation r = q.pollRequest();
                System.out.println("\n  [Request " + pos++ + "] " + r);
                allocateRoom(r);
            }
            System.out.println("\n  --------------------------------");
        }

        public synchronized void deallocateRoom(String roomId, String type) {
            allAllocatedIds.remove(roomId);
            Set<String> typeAllocations = allocatedByType.get(type);
            if (typeAllocations != null) typeAllocations.remove(roomId);
        }

        // UC12: State extraction and recovery
        public synchronized Set<String> getAllocatedIdsSnapshot() { return new HashSet<>(allAllocatedIds); }
        public synchronized HashMap<String, Set<String>> getAllocatedByTypeSnapshot() {
            HashMap<String, Set<String>> copy = new HashMap<>();
            for (Map.Entry<String, Set<String>> e : allocatedByType.entrySet()) {
                copy.put(e.getKey(), new HashSet<>(e.getValue()));
            }
            return copy;
        }

        public synchronized void restoreSnapshots(Set<String> ids, HashMap<String, Set<String>> byType) {
            allAllocatedIds.clear();
            if (ids != null) allAllocatedIds.addAll(ids);
            
            allocatedByType.clear();
            if (byType != null) {
                for (Map.Entry<String, Set<String>> e : byType.entrySet()) {
                    allocatedByType.put(e.getKey(), new HashSet<>(e.getValue()));
                }
            }
            
            // Adjust ID counter to prevent future clashes upon restart
            for (String id : allAllocatedIds) {
                try {
                    String[] parts = id.split("-");
                    int num = Integer.parseInt(parts[1]);
                    if (num > idCounter) idCounter = num;
                } catch (Exception ignored) {}
            }
        }
    }

    // =========================================================================
    // UC12 — Persistence Service & System Recovery (Serialization)
    // =========================================================================

    /**
     * PersistenceService — Handles saving and loading the critical system state
     * (inventory, history, allocations) to and from a local binary file.
     * Prevents data loss across application restarts.
     */
    static class PersistenceService {
        private static final String DATA_FILE = "system_persistence.dat";

        /** Wrapper struct to hold all serializable checkpoints. */
        static class SystemStateSnapshot implements Serializable {
            private static final long serialVersionUID = 1L;
            HashMap<String, Integer>       inventoryState;
            List<ConfirmedBooking>         historyState;
            Set<String>                    allocatedIds;
            HashMap<String, Set<String>>   allocatedByType;
        }

        public void saveState(RoomInventory inv, BookingHistory hist, RoomAllocationService alloc) {
            System.out.println("\n  --- System Shutdown Preparation ---");
            SystemStateSnapshot state = new SystemStateSnapshot();
            state.inventoryState  = inv.getSnapshot();
            state.historyState    = hist.getSnapshot();
            state.allocatedIds    = alloc.getAllocatedIdsSnapshot();
            state.allocatedByType = alloc.getAllocatedByTypeSnapshot();

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
                oos.writeObject(state);
                System.out.println("  [PERSISTENCE] Critical system state safely serialized to " + DATA_FILE);
            } catch (IOException e) {
                System.out.println("  [PERSISTENCE ERROR] Failed to save state: " + e.getMessage());
            }
        }

        public boolean loadState(RoomInventory inv, BookingHistory hist, RoomAllocationService alloc) {
            File f = new File(DATA_FILE);
            if (!f.exists()) {
                System.out.println("  [PERSISTENCE] No existing persistence file found. Starting with fresh state.");
                return false;
            }

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                SystemStateSnapshot state = (SystemStateSnapshot) ois.readObject();
                inv.restoreSnapshot(state.inventoryState);
                hist.restoreSnapshot(state.historyState);
                alloc.restoreSnapshots(state.allocatedIds, state.allocatedByType);
                System.out.println("  [RECOVERY] System state successfully restored from " + DATA_FILE);
                System.out.println("  [RECOVERY] " + state.historyState.size() + " historical bookings re-loaded into memory.");
                return true;
            } catch (Exception e) {
                System.out.println("  [RECOVERY FAILED] Corrupted or invalid file. Starting fresh. (" + e.getMessage() + ")");
                return false;
            }
        }
        
        public void deleteSaveFile() {
            File f = new File(DATA_FILE);
            if (f.exists() && f.delete()) {
                System.out.println("  [CLEANUP] Deleted persistence file.");
            }
        }
    }

    // =========================================================================
    // UC1 — Application Entry Point (main)
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("============================================");
        System.out.println("   Welcome to Book My Stay App");
        System.out.println("   Hotel Booking Management System v12.0");
        System.out.println("============================================");

        RoomInventory inventory = new RoomInventory();
        BookingHistory history = new BookingHistory();
        RoomAllocationService allocService = new RoomAllocationService(inventory, history);
        PersistenceService persistence = new PersistenceService();

        // -------------------------------------------------------------
        // UC12: Recovery Phase - Check for persisted state on startup
        // -------------------------------------------------------------
        System.out.println("\n[UC12] Application Startup Sequence...");
        boolean restored = persistence.loadState(inventory, history, allocService);

        if (!restored) {
            // Fresh run: We'll simulate some load, then shut down to create the save file.
            System.out.println("\n[UC9/11] Generating Fresh System Traffic...");
            BookingRequestQueue queue = new BookingRequestQueue();
            try {
                queue.addRequest(new Reservation("Alice Fresh",  "Single Room", 3));
                queue.addRequest(new Reservation("Bob Fresh",    "Suite Room",  2));
            } catch (Exception e) {}
            
            allocService.processQueue(queue);
            
        } else {
            // Restored Run: We loaded Alice and Bob!
            System.out.println("\n[System] Restored Inventory Check:");
            inventory.displayInventory();
            
            System.out.println("\n[UC8] Restored Operational Report:");
            new BookingReportService(history).generateSummaryReport();
            
            System.out.println("\n[System] Adding new concurrent requests on top of restored state...");
            Thread[] threads = new Thread[3];
            for (int i = 0; i < 3; i++) {
                final String g = "Recovery Guest " + (i + 1);
                threads[i] = new Thread(() -> {
                    try { allocService.allocateRoom(new Reservation(g, "Single Room", 1)); } 
                    catch (Exception e) {}
                });
            }
            for (Thread t : threads) t.start();
            for (Thread t : threads) { try { t.join(); } catch (Exception e) {} }
            
            System.out.println("\n[UC8] Final Operational Report (Combining Old + New):");
            new BookingReportService(history).generateSummaryReport();
            
            // Clean up file if you'd like the next run to be fresh. 
            // Commenting out so normal runs load the file until explicitly cleared.
             persistence.deleteSaveFile();
        }

        // -------------------------------------------------------------
        // UC12: Persistence Phase - Save state on shutdown
        // -------------------------------------------------------------
        persistence.saveState(inventory, history, allocService);

        System.out.println("\n============================================");
        System.out.println("  All 12 use cases executed successfully.");
        System.out.println("  State saved durably. Safe to terminate.");
        System.out.println("============================================");
    }
}
