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
 *
 * @author Sharveswar
 * @version 11.0
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
    // UC3 + UC11 — Centralized Room Inventory (Synchronized)
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

        /** UC11: Thread-safe read access */
        public synchronized int getAvailability(String t) { return inventory.getOrDefault(t, 0); }
        public Iterable<String> getRoomTypes() { return inventory.keySet(); }

        /** UC11: Thread-safe mutation */
        public synchronized void decrementAvailability(String t) throws InvalidBookingException {
            if (!validRoomTypes.contains(t)) throw new InvalidBookingException("Unknown room type: " + t);
            int cur = inventory.getOrDefault(t, 0);
            if (cur <= 0) throw new InvalidBookingException("Inventory exhausted for: " + t);
            inventory.put(t, cur - 1);
        }

        /** UC11: Thread-safe mutation */
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
    // UC5 + UC11 — Reservation & Thread-Safe Queue
    // =========================================================================

    static class Reservation {
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
        
        /** UC11: Synchronized queue access */
        public synchronized void addRequest(Reservation r) {
            queue.offer(r);
        }
        
        /** UC11: Synchronized poll from queue */
        public synchronized Reservation pollRequest() {
            return queue.poll();
        }
        
        public synchronized boolean isEmpty() { return queue.isEmpty(); }
    }

    // =========================================================================
    // UC8 + UC10 — Booking History & Reporting
    // =========================================================================

    static class ConfirmedBooking {
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
        
        /** UC11: Synchronized access to shared history list */
        public synchronized void addRecord(ConfirmedBooking booking) { history.add(booking); }
        public synchronized List<ConfirmedBooking> getHistory() { return new ArrayList<>(history); }
        
        public synchronized ConfirmedBooking findBookingByRoomId(String roomId) {
            for (ConfirmedBooking cb : history) {
                if (cb.getRoomId().equals(roomId)) return cb;
            }
            return null;
        }
    }

    static class BookingReportService {
        private final BookingHistory bookingHistory;
        public BookingReportService(BookingHistory bookingHistory) { this.bookingHistory = bookingHistory; }
        public void generateSummaryReport() {
            System.out.println("\n  --- Booking History Report ---");
            List<ConfirmedBooking> records = bookingHistory.getHistory(); // thread-safe snapshot
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
    // UC6 + UC11 — Room Allocation Service (Critical Sections)
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

        /**
         * UC11: Critical Section for allocation.
         * The entire sequence (check availability -> decrement -> save) is synchronized
         * to prevent race conditions when multiple threads book simultaneously.
         */
        public synchronized boolean allocateRoom(Reservation r) {
            try {
                String type = r.getRoomType();
                if (!inventory.isValidRoomType(type)) throw new InvalidBookingException("Invalid room type: " + type);
                
                // Critical check
                if (inventory.getAvailability(type) <= 0) {
                    System.out.println("  [REJECTED] No availability for " + type + " (Guest: " + r.getGuestName() + ")");
                    return false;
                }

                // Mutate state consistently
                inventory.decrementAvailability(type);
                String roomId = generateUniqueId(type);
                allAllocatedIds.add(roomId);
                allocatedByType.computeIfAbsent(type, k -> new HashSet<>()).add(roomId);

                bookingHistory.addRecord(new ConfirmedBooking(roomId, r));

                System.out.println("  [CONFIRMED] Room ID: " + roomId + " -> " + r.getGuestName()
                    + " | Remaining " + type + ": " + inventory.getAvailability(type)
                    + " [Thread: " + Thread.currentThread().getName() + "]");
                return true;

            } catch (InvalidBookingException e) {
                System.out.println("  [FAILED - VALIDATION] " + e.getMessage());
                return false;
            }
        }

        /** Sequential queue processing helper (reused from earlier logic) */
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
    }

    // =========================================================================
    // UC10 — Booking Cancellation Service
    // =========================================================================

    static class CancellationService {
        private final RoomInventory         inventory;
        private final RoomAllocationService allocationService;
        private final BookingHistory        history;
        private final Stack<String>         cancelledRoomIds;

        public CancellationService(RoomInventory inventory, RoomAllocationService allocs, BookingHistory history) {
            this.inventory         = inventory;
            this.allocationService = allocs;
            this.history           = history;
            this.cancelledRoomIds  = new Stack<>();
        }

        public synchronized void cancelBooking(String roomId) {
            ConfirmedBooking booking = history.findBookingByRoomId(roomId);
            if (booking == null || booking.isCancelled()) return;

            String type = booking.getReservation().getRoomType();
            booking.cancel();
            allocationService.deallocateRoom(roomId, type);
            cancelledRoomIds.push(roomId);
            inventory.incrementAvailability(type);

            System.out.println("  [CANCELLED-SUCCESS] Room " + roomId + " released back to pool.");
        }
    }

    // =========================================================================
    // UC1 — Application Entry Point (main)
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("============================================");
        System.out.println("   Welcome to Book My Stay App");
        System.out.println("   Hotel Booking Management System v11.0");
        System.out.println("============================================");

        RoomInventory inventory = new RoomInventory();
        BookingHistory history = new BookingHistory();
        RoomAllocationService allocService = new RoomAllocationService(inventory, history);
        CancellationService cancelService = new CancellationService(inventory, allocService, history);
        
        System.out.println("\n[UC11] CONCURRENT BOOKING SIMULATION:");
        System.out.println("       10 Guests trying to book the last 3 Single Rooms simultaneously.");
        
        // Spawn 10 simultaneous threads fighting for 3 rooms
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final String guestName = "Concurrent Guest " + (i + 1);
            threads[i] = new Thread(() -> {
                try {
                    // Slight random sleep to force thread interleaving context switches
                    Thread.sleep((long)(Math.random() * 50)); 
                    Reservation r = new Reservation(guestName, "Single Room", 2);
                    allocService.allocateRoom(r);
                } catch (Exception e) {}
            }, "Thread-" + i);
        }

        // Start all threads
        for (Thread t : threads) t.start();
        
        // Wait for all threads to finish
        for (Thread t : threads) {
            try { t.join(); } catch (InterruptedException e) {}
        }

        System.out.println("\n[State Check] Inventory after concurrent bookings (Race conditions prevented):");
        inventory.displayInventory();

        System.out.println("\n[UC8] Generating Operational Booking Report:");
        new BookingReportService(history).generateSummaryReport();

        System.out.println("\n============================================");
        System.out.println("  All use cases executed successfully.");
        System.out.println("  One file. Eleven concepts. Real-world design.");
        System.out.println("============================================");
    }
}
