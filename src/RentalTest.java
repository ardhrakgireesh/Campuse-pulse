import model.Rental;
import model.RentalRepository;
import model.Resource;
import service.RentalService;
import service.ResourceService;
import util.AppConstants;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RentalTest {

    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> tempDbs = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("=== CampusPulse — Resource Rental Tests ===\n");

        try {
            // TC-1: Rental creation & persistence in SQLite
            {
                String dbPath = createTempDbPath("tc1");
                RentalRepository repo = new RentalRepository(dbPath);
                ResourceService resService = new ResourceService();
                resService.addResource(new Resource("R1", "Laptop", "Lenovo ThinkPad", "Electronics", "STAFF1", 2, AppConstants.RESOURCE_STATUS_AVAILABLE));

                RentalService rentalService = new RentalService(repo, resService);

                Rental rnt = rentalService.rentResource("STU1", "R1", 7);
                assertNotNull("TC-1a Rental created with non-null ID", rnt.getRentalId());
                assertEqual("TC-1b Initial status is ACTIVE", Rental.STATUS_ACTIVE, rnt.getStatus());
                assertEqual("TC-1c Rented resource quantity decremented from 2 to 1", 1, resService.findById("R1").getQuantity());

                // Verify persistence in SQLite
                Rental found = repo.findById(rnt.getRentalId());
                assertNotNull("TC-1d Rental found in database", found);
                assertEqual("TC-1e Student ID matches in DB", "STU1", found.getStudentId());
                assertEqual("TC-1f Resource ID matches in DB", "R1", found.getResourceId());
            }

            // TC-2: Unavailable resource rejection
            {
                String dbPath = createTempDbPath("tc2");
                RentalRepository repo = new RentalRepository(dbPath);
                ResourceService resService = new ResourceService();
                resService.addResource(new Resource("R2", "Calculator", "Casio", "Electronics", "STAFF1", 0, AppConstants.RESOURCE_STATUS_BORROWED));

                RentalService rentalService = new RentalService(repo, resService);

                boolean caught = false;
                try {
                    rentalService.rentResource("STU1", "R2", 5);
                } catch (IllegalStateException e) {
                    caught = true;
                }
                assertEqual("TC-2 Rental rejected for unavailable resource (qty 0)", true, caught);
            }

            // TC-3: Invalid duration rejection
            {
                String dbPath = createTempDbPath("tc3");
                RentalRepository repo = new RentalRepository(dbPath);
                ResourceService resService = new ResourceService();
                resService.addResource(new Resource("R3", "Multimeter", "Digital", "Electronics", "STAFF1", 1, AppConstants.RESOURCE_STATUS_AVAILABLE));

                RentalService rentalService = new RentalService(repo, resService);

                boolean caughtTooLong = false;
                try {
                    rentalService.rentResource("STU1", "R3", 15); // > 14 days
                } catch (IllegalArgumentException e) {
                    caughtTooLong = true;
                }
                assertEqual("TC-3a Duration > 14 days rejected", true, caughtTooLong);

                boolean caughtZero = false;
                try {
                    rentalService.rentResource("STU1", "R3", 0); // <= 0 days
                } catch (IllegalArgumentException e) {
                    caughtZero = true;
                }
                assertEqual("TC-3b Duration 0 days rejected", true, caughtZero);
            }

            // TC-4: Persistence across restart
            {
                String dbPath = createTempDbPath("tc4");
                RentalRepository repo1 = new RentalRepository(dbPath);
                ResourceService resService1 = new ResourceService();
                resService1.addResource(new Resource("R4", "Sports Kit", "Volleyball", "Sports", "STAFF1", 3, AppConstants.RESOURCE_STATUS_AVAILABLE));

                RentalService service1 = new RentalService(repo1, resService1);
                Rental rnt = service1.rentResource("STU2", "R4", 5);

                // Simulate app restart with fresh instance pointing to same DB
                RentalRepository repo2 = new RentalRepository(dbPath);
                Rental retrieved = repo2.findById(rnt.getRentalId());
                assertNotNull("TC-4a Rental retrieved from second repository instance", retrieved);
                assertEqual("TC-4b Status survives restart", Rental.STATUS_ACTIVE, retrieved.getStatus());
                assertEqual("TC-4c Due date survives restart", rnt.getExpectedReturnDate(), retrieved.getExpectedReturnDate());
            }

            // TC-5: On-time return (Zero fine)
            {
                String dbPath = createTempDbPath("tc5");
                RentalRepository repo = new RentalRepository(dbPath);
                ResourceService resService = new ResourceService();
                resService.addResource(new Resource("R5", "Textbook", "Algorithms", "Books", "STAFF1", 1, AppConstants.RESOURCE_STATUS_AVAILABLE));

                RentalService service = new RentalService(repo, resService);
                Rental rnt = service.rentResource("STU3", "R5", 7);

                Rental returned = service.processReturn(rnt.getRentalId());
                assertEqual("TC-5a Status transitions to RETURNED", Rental.STATUS_RETURNED, returned.getStatus());
                assertEqual("TC-5b On-time return has ₹0.00 fine", 0.0, returned.getFineAmount());
                assertEqual("TC-5c Catalog inventory restored to 1", 1, resService.findById("R5").getQuantity());
                assertEqual("TC-5d Actual return date is set", LocalDate.now().toString(), returned.getActualReturnDate());
            }

            // TC-6: Overdue return fine calculation (₹2.00 per day)
            {
                String dbPath = createTempDbPath("tc6");
                RentalRepository repo = new RentalRepository(dbPath);
                ResourceService resService = new ResourceService();
                resService.addResource(new Resource("R6", "Drill", "Power Drill", "Tools", "STAFF1", 1, AppConstants.RESOURCE_STATUS_AVAILABLE));

                RentalService service = new RentalService(repo, resService);

                // Simulate a past rental 3 days overdue
                String pastStart = LocalDate.now().minusDays(10).toString();
                String pastDue   = LocalDate.now().minusDays(3).toString();
                Rental overdueRental = new Rental("RNT-999", "R6", "STU4", pastStart, pastDue, null, Rental.STATUS_ACTIVE, 0.0);
                repo.addRental(overdueRental);

                assertEqual("TC-6a Detected as overdue", true, service.isOverdue(overdueRental));
                assertEqual("TC-6b Overdue days equals 3", 3L, service.calculateOverdueDays(overdueRental));
                assertEqual("TC-6c Calculated fine is ₹6.00 (3 days * ₹2.00)", 6.0, service.calculateFine(overdueRental));

                // Process return and verify locked fine
                Rental returned = service.processReturn("RNT-999");
                assertEqual("TC-6d Locked fine on return is ₹6.00", 6.0, returned.getFineAmount());
                assertEqual("TC-6e Status is RETURNED in DB", Rental.STATUS_RETURNED, repo.findById("RNT-999").getStatus());
            }

            // TC-7: Duplicate return prevention
            {
                String dbPath = createTempDbPath("tc7");
                RentalRepository repo = new RentalRepository(dbPath);
                ResourceService resService = new ResourceService();
                resService.addResource(new Resource("R7", "Keyboard", "Mechanical", "Electronics", "STAFF1", 1, AppConstants.RESOURCE_STATUS_AVAILABLE));

                RentalService service = new RentalService(repo, resService);
                Rental rnt = service.rentResource("STU5", "R7", 3);
                service.processReturn(rnt.getRentalId());

                boolean duplicateCaught = false;
                try {
                    service.processReturn(rnt.getRentalId()); // Second attempt
                } catch (IllegalStateException e) {
                    duplicateCaught = true;
                }
                assertEqual("TC-7 Duplicate return attempt rejected", true, duplicateCaught);
            }

            // TC-8: Student rentals query filtering
            {
                String dbPath = createTempDbPath("tc8");
                RentalRepository repo = new RentalRepository(dbPath);
                ResourceService resService = new ResourceService();
                resService.addResource(new Resource("R8A", "Item A", "Desc", "Cat", "STAFF1", 5, AppConstants.RESOURCE_STATUS_AVAILABLE));
                resService.addResource(new Resource("R8B", "Item B", "Desc", "Cat", "STAFF1", 5, AppConstants.RESOURCE_STATUS_AVAILABLE));

                RentalService service = new RentalService(repo, resService);
                service.rentResource("STU_X", "R8A", 3);
                service.rentResource("STU_X", "R8B", 5);
                service.rentResource("STU_Y", "R8A", 2);

                List<Rental> stuXRentals = service.getRentalsByStudent("STU_X");
                assertEqual("TC-8a STU_X has 2 rentals", 2, stuXRentals.size());

                List<Rental> stuYRentals = service.getRentalsByStudent("STU_Y");
                assertEqual("TC-8b STU_Y has 1 rental", 1, stuYRentals.size());

                List<Rental> allRentals = service.getAllRentals();
                assertEqual("TC-8c Total rentals count is 3", 3, allRentals.size());
            }

        } finally {
            cleanupTempDatabases();
        }

        System.out.println("\n--- Results ---");
        System.out.println("PASSED: " + passed);
        System.out.println("FAILED: " + failed);

        if (failed > 0) {
            System.err.println("\nSome tests FAILED. See above for details.");
            System.exit(1);
        } else {
            System.out.println("\nAll tests PASSED.");
        }
    }

    private static String createTempDbPath(String tag) {
        String path = "data/test_rental_" + tag + "_" + System.currentTimeMillis() + "_" + tempDbs.size() + ".db";
        tempDbs.add(path);
        return path;
    }

    private static void cleanupTempDatabases() {
        for (String path : tempDbs) {
            new File(path).delete();
            new File(path + "-wal").delete();
            new File(path + "-shm").delete();
            new File(path + "-journal").delete();
        }
    }

    private static void assertEqual(String label, Object expected, Object actual) {
        boolean ok = (expected == null && actual == null)
                || (expected != null && expected.equals(actual));
        if (ok) {
            System.out.println("  PASS  " + label);
            passed++;
        } else {
            System.out.println("  FAIL  " + label
                    + " | expected=[" + expected + "] got=[" + actual + "]");
            failed++;
        }
    }

    private static void assertNotNull(String label, Object value) {
        if (value != null) {
            System.out.println("  PASS  " + label);
            passed++;
        } else {
            System.out.println("  FAIL  " + label + " | expected non-null but got null");
            failed++;
        }
    }
}

