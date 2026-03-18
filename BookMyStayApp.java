import java.util.ArrayList;
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
 *
 * @author Sharveswar
 * @version 7.0
 */
public class BookMyStayApp {

    // =========================================================================
    // UC2 — Room Domain Model (Abstract Class + Inheritance + Polymorphism)
    // =========================================================================

    /**
     * Abstract base class for all hotel room types.
     * Encapsulates shared attributes; {@code displayAmenities()} is
     * overridden by each concrete subclass.
     */
    static abstract class Room {
        protected String roomType, sizeDescription, amenities;
        protected int numberOfBeds;
        protected double pricePerNight;

        public Room(String rt, int beds, double price, String size, String am) {
            roomType = rt; numberOfBeds = beds; pricePerNight = price;
            sizeDescription = size; amenities = am;
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

    /** Single Room — compact, budget-friendly (1 bed). */
    static class SingleRoom extends Room {
        public SingleRoom() { super("Single Room", 1, 80.00, "Small (200 sq ft)", "Wi-Fi, TV, Mini-Fridge"); }
        @Override public void displayAmenities() { System.out.println("  Amenities   : " + amenities); }
    }

    /** Double Room — suitable for couples or small families (2 beds). */
    static class DoubleRoom extends Room {
        public DoubleRoom() { super("Double Room", 2, 140.00, "Medium (350 sq ft)", "Wi-Fi, TV, Mini-Fridge, Work Desk"); }
        @Override public void displayAmenities() { System.out.println("  Amenities   : " + amenities); }
    }

    /** Suite Room — premium, spacious accommodation (3 beds). */
    static class SuiteRoom extends Room {
        public SuiteRoom() { super("Suite Room", 3, 280.00, "Large (600 sq ft)", "Wi-Fi, TV, Mini-Bar, Jacuzzi, Living Area, Balcony"); }
        @Override public void displayAmenities() { System.out.println("  Amenities   : " + amenities); }
    }

    // =========================================================================
    // UC3 — Centralized Room Inventory (HashMap — O(1) access, single source)
    // =========================================================================

    /**
     * RoomInventory — replaces scattered UC2 variables with a
     * {@code HashMap<String, Integer>} as the single source of truth.
     */
    static class RoomInventory {
        private final HashMap<String, Integer> inventory;

        public RoomInventory() {
            inventory = new HashMap<>();
            inventory.put("Single Room", 3);
            inventory.put("Double Room", 2);
            inventory.put("Suite Room",  1);
        }

        public int getAvailability(String t)   { return inventory.getOrDefault(t, 0); }
        public Iterable<String> getRoomTypes()  { return inventory.keySet(); }

        /** Atomically decrements availability after confirmed allocation. */
        public void decrementAvailability(String t) {
            int cur = inventory.getOrDefault(t, 0);
            if (cur > 0) inventory.put(t, cur - 1);
        }

        public void displayInventory() {
            System.out.println("\n  --- Room Inventory ---");
            for (Map.Entry<String, Integer> e : inventory.entrySet())
                System.out.printf("  %-15s : %d available%n", e.getKey(), e.getValue());
            System.out.println("  ----------------------");
        }
    }

    // =========================================================================
    // UC4 — Search Service (Read-Only, No State Mutation)
    // =========================================================================

    /**
     * SearchService — read-only window into available rooms.
     * Inventory is never modified during search operations.
     */
    static class SearchService {
        private final RoomInventory    inventory;
        private final Map<String, Room> catalog;

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
    // UC5 — Reservation + Booking Request Queue (FIFO)
    // =========================================================================

    /** Reservation — immutable record of a guest's booking intent. */
    static class Reservation {
        private final String guestName, roomType;
        private final int numberOfNights;

        public Reservation(String guest, String room, int nights) {
            guestName = guest; roomType = room; numberOfNights = nights;
        }

        public String getGuestName()      { return guestName; }
        public String getRoomType()       { return roomType; }
        public int    getNumberOfNights() { return numberOfNights; }

        @Override
        public String toString() {
            return "Reservation[guest=" + guestName + ", room=" + roomType + ", nights=" + numberOfNights + "]";
        }
    }

    /**
     * BookingRequestQueue — FIFO intake for reservation requests.
     * LinkedList-backed Queue preserves insertion order automatically.
     * No inventory mutation occurs here.
     */
    static class BookingRequestQueue {
        private final Queue<Reservation> queue;
        public BookingRequestQueue()             { queue = new LinkedList<>(); }
        public void addRequest(Reservation r)    { queue.offer(r); System.out.println("  [QUEUED] " + r); }
        public Queue<Reservation> getQueue()     { return queue; }
        public int getPendingCount()             { return queue.size(); }
        public Reservation peekNextRequest()     { return queue.peek(); }

        public void displayQueue() {
            System.out.println("\n  --- Pending Requests (FIFO) ---");
            int p = 1; for (Reservation r : queue) System.out.println("  " + p++ + ". " + r);
            System.out.println("  Total pending: " + queue.size());
            System.out.println("  --------------------------------");
        }
    }

    // =========================================================================
    // UC6 — Room Allocation Service (Set + HashMap for Uniqueness)
    // =========================================================================

    /**
     * RoomAllocationService — processes queued requests and confirms reservations.
     *
     * <p>Key structures:
     * <ul>
     *   <li>{@code Set<String> allAllocatedIds} — global uniqueness guard</li>
     *   <li>{@code HashMap<String, Set<String>> allocatedByType} — per-type tracking</li>
     * </ul>
     * Inventory is decremented atomically with each confirmed booking.
     */
    static class RoomAllocationService {
        private final RoomInventory               inventory;
        private final Set<String>                  allAllocatedIds;
        private final HashMap<String, Set<String>> allocatedByType;
        private int idCounter = 100;

        public RoomAllocationService(RoomInventory inv) {
            inventory       = inv;
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

        /** Processes all queued requests in FIFO order. */
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

                System.out.println("  [CONFIRMED] Room ID: " + roomId + " -> " + r.getGuestName()
                    + " | Remaining " + type + ": " + inventory.getAvailability(type));
            }
            System.out.println("\n  --------------------------------");
        }

        public void displaySummary() {
            System.out.println("\n  --- Allocation Summary ---");
            for (Map.Entry<String, Set<String>> e : allocatedByType.entrySet())
                System.out.println("  " + e.getKey() + " : " + e.getValue());
            System.out.println("  Total allocated: " + allAllocatedIds.size());
            System.out.println("  --------------------------");
        }
    }

    // =========================================================================
    // UC7 — Add-On Service Selection (Map + List, Composition)
    // =========================================================================

    /**
     * AddOnService — represents an individual optional offering.
     *
     * <p>Each service has a name, description, and cost. Services are composed
     * with reservations rather than inherited, following composition over
     * inheritance to keep the design flexible and extensible.
     */
    static class AddOnService {
        private final String serviceName;
        private final String description;
        private final double cost;

        public AddOnService(String serviceName, String description, double cost) {
            this.serviceName = serviceName;
            this.description = description;
            this.cost        = cost;
        }

        public String getServiceName() { return serviceName; }
        public String getDescription() { return description; }
        public double getCost()        { return cost; }

        @Override
        public String toString() {
            return serviceName + " ($" + String.format("%.2f", cost) + ")";
        }
    }

    /**
     * AddOnServiceManager — manages the association between reservations
     * and their selected optional services.
     *
     * <p>Uses {@code Map<String, List<AddOnService>>} to model the one-to-many
     * relationship between a reservation (identified by room ID) and its
     * selected services. Lists preserve insertion order and allow duplicates.
     *
     * <p>This component operates independently of core booking and inventory
     * logic — it does NOT modify room availability or booking state.
     */
    static class AddOnServiceManager {

        /** Maps reservation room ID → list of selected add-on services. */
        private final Map<String, List<AddOnService>> serviceMap;

        public AddOnServiceManager() {
            serviceMap = new HashMap<>();
        }

        /**
         * Attaches an add-on service to the specified reservation.
         *
         * @param roomId  the allocated room ID (from UC6)
         * @param service the add-on service to attach
         */
        public void addService(String roomId, AddOnService service) {
            serviceMap.computeIfAbsent(roomId, k -> new ArrayList<>()).add(service);
            System.out.println("  [ADD-ON] " + service.getServiceName()
                + " added to reservation " + roomId);
        }

        /**
         * Calculates the total cost of all add-on services for a reservation.
         *
         * @param roomId the room ID to aggregate costs for
         * @return total additional cost, or 0.0 if no services selected
         */
        public double getTotalAddOnCost(String roomId) {
            List<AddOnService> services = serviceMap.get(roomId);
            if (services == null) return 0.0;
            double total = 0.0;
            for (AddOnService s : services) total += s.getCost();
            return total;
        }

        /**
         * Retrieves the list of services for a given reservation.
         *
         * @param roomId the room ID to look up
         * @return list of add-on services, or empty list if none
         */
        public List<AddOnService> getServices(String roomId) {
            return serviceMap.getOrDefault(roomId, new ArrayList<>());
        }

        /**
         * Displays all add-on service selections and their costs.
         */
        public void displayAllAddOns() {
            System.out.println("\n  --- Add-On Service Summary ---");
            if (serviceMap.isEmpty()) {
                System.out.println("  No add-on services selected.");
            } else {
                for (Map.Entry<String, List<AddOnService>> entry : serviceMap.entrySet()) {
                    String roomId = entry.getKey();
                    List<AddOnService> services = entry.getValue();
                    System.out.println("\n  Room ID: " + roomId);
                    for (AddOnService s : services) {
                        System.out.printf("    - %-20s : $%.2f  (%s)%n",
                            s.getServiceName(), s.getCost(), s.getDescription());
                    }
                    System.out.printf("    Total Add-On Cost   : $%.2f%n", getTotalAddOnCost(roomId));
                }
            }
            System.out.println("\n  ------------------------------");
        }
    }

    // =========================================================================
    // UC1 — Application Entry Point (main) — ties all use cases together
    // =========================================================================

    /**
     * Main method — runs all 7 use cases sequentially to demonstrate the full
     * Hotel Booking Management System lifecycle.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {

        // UC1: Welcome message
        System.out.println("============================================");
        System.out.println("   Welcome to Book My Stay App");
        System.out.println("   Hotel Booking Management System v7.0");
        System.out.println("============================================");

        // UC2: Room catalog
        System.out.println("\n[UC2] Room Type Catalog:");
        Room[] rooms = { new SingleRoom(), new DoubleRoom(), new SuiteRoom() };
        for (Room r : rooms) { System.out.println(); r.displayRoomDetails(); }

        // UC3: Centralized inventory
        System.out.println("\n[UC3] Centralized Inventory:");
        RoomInventory inventory = new RoomInventory();
        inventory.displayInventory();

        // UC4: Read-only search
        System.out.println("\n[UC4] Guest Room Search (Read-Only):");
        new SearchService(inventory).searchAvailableRooms();

        // UC5: FIFO booking queue
        System.out.println("\n[UC5] Booking Requests (FIFO Queue):");
        BookingRequestQueue bookingQueue = new BookingRequestQueue();
        bookingQueue.addRequest(new Reservation("Alice Johnson",  "Single Room", 3));
        bookingQueue.addRequest(new Reservation("Bob Smith",      "Suite Room",  2));
        bookingQueue.addRequest(new Reservation("Carol Williams", "Double Room", 5));
        bookingQueue.addRequest(new Reservation("David Brown",    "Single Room", 1));
        bookingQueue.addRequest(new Reservation("Eva Martinez",   "Suite Room",  4));
        bookingQueue.addRequest(new Reservation("Frank Lee",      "Single Room", 2));
        bookingQueue.displayQueue();

        // UC6: Room allocation
        System.out.println("\n[UC6] Room Allocation (Set + HashMap):");
        RoomAllocationService allocService = new RoomAllocationService(inventory);
        allocService.processQueue(bookingQueue.getQueue());
        allocService.displaySummary();

        // UC7: Add-on service selection
        System.out.println("\n[UC7] Add-On Service Selection:");
        AddOnServiceManager addOnManager = new AddOnServiceManager();

        // Predefined add-on service offerings
        AddOnService breakfast   = new AddOnService("Breakfast",      "Daily buffet breakfast",       25.00);
        AddOnService spa         = new AddOnService("Spa Access",     "Full-day spa and wellness",    60.00);
        AddOnService parking     = new AddOnService("Parking",        "Covered parking per night",    15.00);
        AddOnService airportTaxi = new AddOnService("Airport Taxi",   "Round-trip airport transfer",  45.00);
        AddOnService laundry     = new AddOnService("Laundry",        "Same-day laundry service",     20.00);

        // Guests select services for their confirmed reservations
        System.out.println("\n  Guests selecting add-on services...");
        addOnManager.addService("SNG-101", breakfast);
        addOnManager.addService("SNG-101", parking);
        addOnManager.addService("STE-102", spa);
        addOnManager.addService("STE-102", airportTaxi);
        addOnManager.addService("STE-102", breakfast);
        addOnManager.addService("DBL-103", laundry);

        // Display all add-on selections
        addOnManager.displayAllAddOns();

        // Final inventory
        System.out.println("\n[Final] Inventory after all bookings:");
        inventory.displayInventory();

        System.out.println("\n============================================");
        System.out.println("  All use cases executed successfully.");
        System.out.println("  One file. Seven concepts. Real-world design.");
        System.out.println("============================================");
    }
}
