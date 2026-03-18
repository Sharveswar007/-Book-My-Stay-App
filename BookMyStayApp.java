import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
 *
 * @author Sharveswar
 * @version 8.0
 */
public class BookMyStayApp {

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
    // UC3 — Centralized Room Inventory
    // =========================================================================

    static class RoomInventory {
        private final HashMap<String, Integer> inventory;

        public RoomInventory() {
            inventory = new HashMap<>();
            inventory.put("Single Room", 3);
            inventory.put("Double Room", 2);
            inventory.put("Suite Room",  1);
        }

        public int getAvailability(String t)   { return inventory.getOrDefault(t, 0); }
        public Iterable<String> getRoomTypes() { return inventory.keySet(); }

        public void decrementAvailability(String t) {
            int cur = inventory.getOrDefault(t, 0);
            if (cur > 0) inventory.put(t, cur - 1);
        }

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

        public Reservation(String guest, String room, int nights) {
            guestName=guest; roomType=room; numberOfNights=nights;
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
            System.out.println("  Total pending: " + queue.size());
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

        public void displayAllAddOns() {
            System.out.println("\n  --- Add-On Service Summary ---");
            if (serviceMap.isEmpty()) { System.out.println("  No add-on services selected."); return; }
            for (Map.Entry<String, List<AddOnService>> e : serviceMap.entrySet()) {
                String roomId = e.getKey();
                System.out.println("\n  Room ID: " + roomId);
                for (AddOnService s : e.getValue()) {
                    System.out.printf("    - %-20s : $%.2f%n", s.getServiceName(), s.getCost(), s.getDescription());
                }
                System.out.printf("    Total Add-On Cost   : $%.2f%n", getTotalAddOnCost(roomId));
            }
            System.out.println("\n  ------------------------------");
        }
    }

    // =========================================================================
    // UC8 — Booking History & Reporting (List)
    // =========================================================================

    /**
     * ConfirmedBooking — immutable record linking a generated roomId to
     * the original Reservation details.
     */
    static class ConfirmedBooking {
        private final String roomId;
        private final Reservation reservation;

        public ConfirmedBooking(String roomId, Reservation reservation) {
            this.roomId = roomId;
            this.reservation = reservation;
        }

        public String getRoomId() { return roomId; }
        public Reservation getReservation() { return reservation; }

        @Override
        public String toString() {
            return "ConfirmedBooking[ID=" + roomId + ", Guest=" + reservation.getGuestName()
                 + ", " + reservation.getRoomType() + " for " + reservation.getNumberOfNights() + " nights]";
        }
    }

    /**
     * BookingHistory — maintains chronological record of accepted bookings.
     * <p>Uses {@code List<ConfirmedBooking>} to preserve insertion order.
     */
    static class BookingHistory {
        private final List<ConfirmedBooking> history;

        public BookingHistory() {
            history = new ArrayList<>();
        }

        public void addRecord(ConfirmedBooking booking) {
            history.add(booking);
        }

        /** Returns an unmodifiable view for reporting to prevent accidental mutation. */
        public List<ConfirmedBooking> getHistory() {
            return Collections.unmodifiableList(history);
        }
    }

    /**
     * BookingReportService — generates operational summaries from history data.
     */
    static class BookingReportService {
        private final BookingHistory bookingHistory;

        public BookingReportService(BookingHistory bookingHistory) {
            this.bookingHistory = bookingHistory;
        }

        public void generateSummaryReport() {
            System.out.println("\n  --- Booking History Report ---");
            List<ConfirmedBooking> records = bookingHistory.getHistory();
            if (records.isEmpty()) {
                System.out.println("  No bookings recorded yet.");
                System.out.println("  ------------------------------");
                return;
            }

            int totalNights = 0;
            System.out.println("  Chronological Log:");
            for (int i = 0; i < records.size(); i++) {
                ConfirmedBooking cb = records.get(i);
                System.out.println("    " + (i + 1) + ". " + cb);
                totalNights += cb.getReservation().getNumberOfNights();
            }

            System.out.println("\n  --- Summary Statistics ---");
            System.out.println("  Total Bookings Confirmed : " + records.size());
            System.out.println("  Total Room-Nights Booked : " + totalNights);
            System.out.println("  ------------------------------");
        }
    }

    // =========================================================================
    // UC6 — Room Allocation Service (Updated for UC8 History)
    // =========================================================================

    static class RoomAllocationService {
        private final RoomInventory                inventory;
        private final BookingHistory               bookingHistory;
        private final Set<String>                  allAllocatedIds;
        private final HashMap<String,Set<String>>  allocatedByType;
        private int idCounter = 100;

        /**
         * @param inv     the centralized inventory
         * @param history the chronological booking history (UC8)
         */
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
                String      type = r.getRoomType();
                System.out.println("\n  [Request " + pos++ + "] " + r);

                if (inventory.getAvailability(type) <= 0) {
                    System.out.println("  [REJECTED] No availability for " + type);
                    continue;
                }

                String roomId = generateUniqueId(type);
                allAllocatedIds.add(roomId);
                allocatedByType.computeIfAbsent(type, k -> new HashSet<>()).add(roomId);
                inventory.decrementAvailability(type);

                // UC8: Added to chronological booking history
                bookingHistory.addRecord(new ConfirmedBooking(roomId, r));

                System.out.println("  [CONFIRMED] Room ID: " + roomId + " -> " + r.getGuestName()
                    + " | Remaining " + type + ": " + inventory.getAvailability(type));
            }
            System.out.println("\n  --------------------------------");
        }

        public void displaySummary() {
            System.out.println("\n  --- Allocation Summary ---");
            for (Map.Entry<String,Set<String>> e : allocatedByType.entrySet())
                System.out.println("  " + e.getKey() + " : " + e.getValue());
            System.out.println("  Total allocated: " + allAllocatedIds.size());
            System.out.println("  --------------------------");
        }
    }

    // =========================================================================
    // UC1 — Application Entry Point (main)
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("============================================");
        System.out.println("   Welcome to Book My Stay App");
        System.out.println("   Hotel Booking Management System v8.0");
        System.out.println("============================================");

        Room[] rooms = { new SingleRoom(), new DoubleRoom(), new SuiteRoom() };
        RoomInventory inventory = new RoomInventory();
        new SearchService(inventory).searchAvailableRooms();

        BookingRequestQueue bookingQueue = new BookingRequestQueue();
        bookingQueue.addRequest(new Reservation("Alice Johnson",  "Single Room", 3));
        bookingQueue.addRequest(new Reservation("Bob Smith",      "Suite Room",  2));
        bookingQueue.addRequest(new Reservation("Carol Williams", "Double Room", 5));
        bookingQueue.addRequest(new Reservation("David Brown",    "Single Room", 1));
        bookingQueue.addRequest(new Reservation("Eva Martinez",   "Suite Room",  4));
        bookingQueue.addRequest(new Reservation("Frank Lee",      "Single Room", 2));
        
        // UC8: History instance passed into AllocationService
        BookingHistory history = new BookingHistory();
        RoomAllocationService allocService = new RoomAllocationService(inventory, history);
        allocService.processQueue(bookingQueue.getQueue());

        AddOnServiceManager addOnManager = new AddOnServiceManager();
        addOnManager.addService("SNG-101", new AddOnService("Breakfast", "Daily buffet breakfast", 25.0));
        addOnManager.addService("STE-102", new AddOnService("Spa Access", "Full-day spa", 60.0));

        // UC8: Display Operational Booking Report
        System.out.println("\n[UC8] Generating Operational Booking Report:");
        BookingReportService reportService = new BookingReportService(history);
        reportService.generateSummaryReport();

        System.out.println("\n============================================");
        System.out.println("  All use cases executed successfully.");
        System.out.println("  One file. Eight concepts. Real-world design.");
        System.out.println("============================================");
    }
}
