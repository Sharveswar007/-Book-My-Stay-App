/**
 * UseCase2RoomInitialization - Basic Room Types and Static Availability.
 *
 * <p>
 * This file demonstrates object-oriented domain modeling by introducing
 * an abstract {@code Room} class and concrete room type implementations.
 * Room availability is tracked using simple static variables to intentionally
 * highlight the limitations of scattered state management.
 *
 * @author Sharveswar
 * @version 2.0
 */
public class UseCase2RoomInitialization {

    // ─────────────────────────────────────────────────────────────────────────
    // Abstract Room Domain Model
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Abstract base class representing a hotel room.
     *
     * <p>
     * Encapsulates common attributes shared by all room types and defines
     * an abstract method that concrete subclasses must implement.
     */
    static abstract class Room {
        protected String roomType;
        protected int numberOfBeds;
        protected double pricePerNight;
        protected String sizeDescription;

        /**
         * Constructs a Room with specified attributes.
         *
         * @param roomType        the category/type of the room
         * @param numberOfBeds    number of beds in the room
         * @param pricePerNight   nightly rate in currency units
         * @param sizeDescription textual description of room size
         */
        public Room(String roomType, int numberOfBeds, double pricePerNight, String sizeDescription) {
            this.roomType = roomType;
            this.numberOfBeds = numberOfBeds;
            this.pricePerNight = pricePerNight;
            this.sizeDescription = sizeDescription;
        }

        /** @return the room type label */
        public String getRoomType() {
            return roomType;
        }

        /** @return number of beds */
        public int getNumberOfBeds() {
            return numberOfBeds;
        }

        /** @return nightly price */
        public double getPricePerNight() {
            return pricePerNight;
        }

        /** @return size description string */
        public String getSizeDescription() {
            return sizeDescription;
        }

        /**
         * Displays the amenities specific to this room type.
         * Each concrete subclass must provide its own implementation.
         */
        public abstract void displayAmenities();

        /** Prints a formatted summary of this room's details. */
        public void displayRoomDetails() {
            System.out.println("  Room Type     : " + roomType);
            System.out.println("  Beds          : " + numberOfBeds);
            System.out.println("  Size          : " + sizeDescription);
            System.out.printf("  Price/Night   : $%.2f%n", pricePerNight);
            displayAmenities();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Concrete Room Classes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Represents a Single Room — a compact, budget-friendly option.
     */
    static class SingleRoom extends Room {
        public SingleRoom() {
            super("Single Room", 1, 80.00, "Small (200 sq ft)");
        }

        @Override
        public void displayAmenities() {
            System.out.println("  Amenities     : Wi-Fi, TV, Mini-Fridge");
        }
    }

    /**
     * Represents a Double Room — suitable for couples or small families.
     */
    static class DoubleRoom extends Room {
        public DoubleRoom() {
            super("Double Room", 2, 140.00, "Medium (350 sq ft)");
        }

        @Override
        public void displayAmenities() {
            System.out.println("  Amenities     : Wi-Fi, TV, Mini-Fridge, Work Desk");
        }
    }

    /**
     * Represents a Suite Room — a premium, spacious accommodation.
     */
    static class SuiteRoom extends Room {
        public SuiteRoom() {
            super("Suite Room", 3, 280.00, "Large (600 sq ft)");
        }

        @Override
        public void displayAmenities() {
            System.out.println("  Amenities     : Wi-Fi, TV, Mini-Bar, Jacuzzi, Living Area, Balcony");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Application Entry Point
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Main method — creates room objects and displays availability using
     * simple static variables (intentionally demonstrating their limitations).
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        // Static availability variables — scattered state (intentional limitation)
        int singleRoomAvailability = 5;
        int doubleRoomAvailability = 3;
        int suiteRoomAvailability = 2;

        // Polymorphic room references
        Room singleRoom = new SingleRoom();
        Room doubleRoom = new DoubleRoom();
        Room suiteRoom = new SuiteRoom();

        System.out.println("============================================");
        System.out.println("   Book My Stay App  — Room Type Catalog");
        System.out.println("   Hotel Booking Management System v2.0");
        System.out.println("============================================");

        System.out.println("\n[ " + singleRoom.getRoomType() + " ]");
        singleRoom.displayRoomDetails();
        System.out.println("  Available     : " + singleRoomAvailability + " rooms");

        System.out.println("\n[ " + doubleRoom.getRoomType() + " ]");
        doubleRoom.displayRoomDetails();
        System.out.println("  Available     : " + doubleRoomAvailability + " rooms");

        System.out.println("\n[ " + suiteRoom.getRoomType() + " ]");
        suiteRoom.displayRoomDetails();
        System.out.println("  Available     : " + suiteRoomAvailability + " rooms");

        System.out.println("\n============================================");
        System.out.println("  NOTE: Availability managed by individual");
        System.out.println("  variables — refactoring required for scale.");
        System.out.println("============================================");
    }
}
