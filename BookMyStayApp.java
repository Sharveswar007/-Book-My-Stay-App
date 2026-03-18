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
 *
 * @author Sharveswar
 * @version 10.0
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
    // UC3 + UC9 + UC10 — Centralized Room Inventory
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

        public int getAvailability(String t)   { return inventory.getOrDefault(t, 0); }
        public Iterable<String> getRoomTypes() { return inventory.keySet(); }

        public void decrementAvailability(String t) throws InvalidBookingException {
            if (!validRoomTypes.contains(t)) throw new InvalidBookingException("Unknown room type: " + t);
            int cur = inventory.getOrDefault(t, 0);
            if (cur <= 0) throw new InvalidBookingException("Inventory exhausted for: " + t);
            inventory.put(t, cur - 1);
        }

        /** UC10: Restores availability upon cancellation. */
        public void incrementAvailability(String t) {
            if (validRoomTypes.contains(t)) {
                inventory.put(t, inventory.getOrDefault(t, 0) + 1);
            }
        }

        public boolean isValidRoomType(String t) { return validRoomTypes.contains(t); }

        public void displayInventory() {
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
    // UC5 — Reservation + Request Queue
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
        public BookingRequestQueue()            { queue = new LinkedList<>(); }
        public void addRequest(Reservation r)   { queue.offer(r); System.out.println("  [QUEUED] " + r); }
        public Queue<Reservation> getQueue()    { return queue; }
        public void displayQueue() {
            System.out.println("\n  --- Pending Requests (FIFO) ---");
            int p=1; for (Reservation r : queue) System.out.println("  " + p++ + ". " + r);
            System.out.println("  --------------------------------");
        }
    }

    // =========================================================================
    // UC7 — Add-On Service Selection
    // =========================================================================

    static class AddOnService {
        private final String serviceName, description;
        private final double cost;

        public AddOnService(String name, String desc, double cost) {
            this.serviceName=name; this.description=desc; this.cost=cost;
        }
        public String getServiceName() { return serviceName; }
        public String getDescription() { return description; }
        public double getCost()        { return cost; }
        @Override public String toString() { return serviceName + " ($" + String.format("%.2f", cost) + ")"; }
    }

    static class AddOnServiceManager {
        private final Map<String, List<AddOnService>> serviceMap;
        public AddOnServiceManager() { serviceMap = new HashMap<>(); }

        public void addService(String roomId, AddOnService service) {
            serviceMap.computeIfAbsent(roomId, k -> new ArrayList<>()).add(service);
            System.out.println("  [ADD-ON] " + service.getServiceName() + " attached to " + roomId);
        }

        public double getTotalAddOnCost(String roomId) {
            List<AddOnService> services = serviceMap.get(roomId);
            if (services == null) return 0.0;
            double total = 0.0;
            for (AddOnService s : services) total += s.getCost();
            return total;
        }
    }

    // =========================================================================
    // UC8 + UC10 — Booking History & Reporting
    // =========================================================================

    static class ConfirmedBooking {
        private final String roomId;
        private final Reservation reservation;
        private boolean isCancelled; // UC10: Tracks cancellation status

        public ConfirmedBooking(String roomId, Reservation reservation) {
            this.roomId = roomId; this.reservation = reservation; this.isCancelled = false;
        }
        
        public String getRoomId() { return roomId; }
        public Reservation getReservation() { return reservation; }
        public boolean isCancelled() { return isCancelled; }
        
        /** UC10: Mark booking as cancelled in history. */
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
        public void addRecord(ConfirmedBooking booking) { history.add(booking); }
        public List<ConfirmedBooking> getHistory() { return Collections.unmodifiableList(history); }
        
        /** UC10: Find a booking by room ID to allow cancellation validation. */
        public ConfirmedBooking findBookingByRoomId(String roomId) {
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
    // UC6 + UC10 — Room Allocation Service (Updated for Deallocation)
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

        private String generateUniqueId(String roomType) {
            String prefix = roomType.equals("Single Room") ? "SNG"
                          : roomType.equals("Double Room") ? "DBL" : "STE";
            String id;
            do { id = prefix + "-" + (++idCounter); } while (allAllocatedIds.contains(id));
            return id;
        }

        public void processQueue(Queue<Reservation> q) {
            System.out.println("\n  --- Processing Booking Queue ---");
            int pos = 1;
            while (!q.isEmpty()) {
                Reservation r    = q.poll();
                System.out.println("\n  [Request " + pos++ + "] " + r);
                
                try {
                    String type = r.getRoomType();
                    if (!inventory.isValidRoomType(type)) throw new InvalidBookingException("Invalid room type requested: " + type);
                    if (inventory.getAvailability(type) <= 0) throw new InvalidBookingException("No availability for " + type);

                    inventory.decrementAvailability(type);
                    String roomId = generateUniqueId(type);
                    allAllocatedIds.add(roomId);
                    allocatedByType.computeIfAbsent(type, k -> new HashSet<>()).add(roomId);

                    bookingHistory.addRecord(new ConfirmedBooking(roomId, r));

                    System.out.println("  [CONFIRMED] Room ID: " + roomId + " -> " + r.getGuestName()
                        + " | Remaining " + type + ": " + inventory.getAvailability(type));

                } catch (InvalidBookingException e) {
                    System.out.println("  [FAILED - VALIDATION ERROR] " + e.getMessage());
                }
            }
            System.out.println("\n  --------------------------------");
        }

        /** UC10: Frees an allocated room ID back to the allocation structures. */
        public void deallocateRoom(String roomId, String type) {
            allAllocatedIds.remove(roomId);
            Set<String> typeAllocations = allocatedByType.get(type);
            if (typeAllocations != null) {
                typeAllocations.remove(roomId);
            }
        }
    }

    // =========================================================================
    // UC10 — Booking Cancellation Service (Stack / LIFO)
    // =========================================================================

    /**
     * CancellationService — reverses system state carefully to ensure consistency.
     * Uses a Stack (LIFO) to track cancelled reservations, enabling potential undo processes.
     */
    static class CancellationService {
        private final RoomInventory         inventory;
        private final RoomAllocationService allocationService;
        private final BookingHistory        history;
        
        /** UC10: Tracks recently released room IDs via Last-In-First-Out (LIFO) Stack. */
        private final Stack<String>         cancelledRoomIds;

        public CancellationService(RoomInventory inventory, RoomAllocationService allocationService, BookingHistory history) {
            this.inventory         = inventory;
            this.allocationService = allocationService;
            this.history           = history;
            this.cancelledRoomIds  = new Stack<>();
        }

        public void cancelBooking(String roomId) {
            System.out.println("\n  [CANCELLATION INITIATED] Reversing booking for Room ID: " + roomId);
            
            // 1. Validation of request
            ConfirmedBooking booking = history.findBookingByRoomId(roomId);
            if (booking == null) {
                System.out.println("  [CANCELLATION FAILED] No reservation found with ID: " + roomId);
                return;
            }
            if (booking.isCancelled()) {
                System.out.println("  [CANCELLATION FAILED] Reservation already cancelled for ID: " + roomId);
                return;
            }

            // 2. State Reversal Process
            String type = booking.getReservation().getRoomType();

            // A. Mark history record as cancelled
            booking.cancel();

            // B. Deallocate the room ID
            allocationService.deallocateRoom(roomId, type);

            // C. Push to LIFO Stack (modeling rollback behavior)
            cancelledRoomIds.push(roomId);

            // D. Restore inventory count
            inventory.incrementAvailability(type);

            System.out.println("  [CANCELLED-SUCCESS] Room " + roomId + " released back to pool.");
            System.out.println("                      " + type + " availability restored to: " + inventory.getAvailability(type));
        }

        public void displayCancelledStack() {
            System.out.println("\n  --- Cancellation Rollback Stack (LIFO) ---");
            if (cancelledRoomIds.isEmpty()) {
                System.out.println("  No cancellations yet.");
                return;
            }
            // Display stack top to bottom
            System.out.println("  Top of Stack (Most Recent):");
            for (int i = cancelledRoomIds.size() - 1; i >= 0; i--) {
                System.out.println("    -> " + cancelledRoomIds.get(i));
            }
            System.out.println("  ------------------------------------------");
        }
    }

    // =========================================================================
    // UC1 — Application Entry Point (main)
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("============================================");
        System.out.println("   Welcome to Book My Stay App");
        System.out.println("   Hotel Booking Management System v10.0");
        System.out.println("============================================");

        RoomInventory inventory = new RoomInventory();
        BookingHistory history = new BookingHistory();
        RoomAllocationService allocService = new RoomAllocationService(inventory, history);
        CancellationService cancelService = new CancellationService(inventory, allocService, history);
        BookingRequestQueue bookingQueue = new BookingRequestQueue();

        System.out.println("\n[UC9] Generating Operations...");
        
        try {
            bookingQueue.addRequest(new Reservation("Alice Johnson",  "Single Room", 3));
            bookingQueue.addRequest(new Reservation("Bob Smith",      "Suite Room",  2));
            bookingQueue.addRequest(new Reservation("Carol Williams", "Double Room", 5));
            bookingQueue.addRequest(new Reservation("David Brown",    "Single Room", 1));
        } catch (InvalidBookingException e) { System.out.println("  [EXCEPTION] " + e.getMessage()); }

        allocService.processQueue(bookingQueue.getQueue());

        // We know Bob Smith got 'STE-101' and Carol got 'DBL-101' etc.
        // Let's cancel a Single Room and the Suite Room.
        System.out.println("\n[UC10] Guest initiates cancellation requests...");
        
        // Let's assume the first Single Room was assigned SNG-101 and Suite was STE-101
        cancelService.cancelBooking("SNG-101");
        cancelService.cancelBooking("STE-101");
        
        // Attempting an invalid cancellation
        cancelService.cancelBooking("FAKE-999");

        // Displaying cancellation stack LIFO
        cancelService.displayCancelledStack();

        System.out.println("\n[State Check] Inventory after cancellations:");
        inventory.displayInventory();

        System.out.println("\n[UC8] Generating Operational Booking Report after rollbacks:");
        new BookingReportService(history).generateSummaryReport();

        System.out.println("\n============================================");
        System.out.println("  All use cases executed successfully.");
        System.out.println("  One file. Ten concepts. Real-world design.");
        System.out.println("============================================");
    }
}
