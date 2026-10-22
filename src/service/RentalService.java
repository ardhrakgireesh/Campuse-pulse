package service;

import model.Rental;
import model.RentalRepository;
import model.Resource;
import util.AppConstants;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class RentalService {

    public static final double DAILY_FINE_RATE = 2.0; // ₹2.00 per day
    public static final int MAX_RENTAL_DAYS = 14;     // Maximum 14 days rental

    private final RentalRepository rentalRepository;
    private final ResourceService resourceService;

    public RentalService(RentalRepository rentalRepository, ResourceService resourceService) {
        this.rentalRepository = rentalRepository;
        this.resourceService  = resourceService;
    }

    public RentalRepository getRepository() {
        return rentalRepository;
    }

    public Rental rentResource(String studentId, String resourceId, int durationDays) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID is required.");
        }
        if (resourceId == null || resourceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource ID is required.");
        }
        if (durationDays < 1 || durationDays > MAX_RENTAL_DAYS) {
            throw new IllegalArgumentException("Rental duration must be between 1 and " + MAX_RENTAL_DAYS + " days.");
        }

        Resource resource = resourceService.findById(resourceId);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceId);
        }
        if (!AppConstants.RESOURCE_STATUS_AVAILABLE.equals(resource.getStatus()) || resource.getQuantity() <= 0) {
            throw new IllegalStateException("Resource '" + resource.getResourceName() + "' is currently unavailable.");
        }

        LocalDate now = LocalDate.now();
        LocalDate expectedReturn = now.plusDays(durationDays);

        String rentalId = String.format("RNT-%03d", rentalRepository.getNextRentalSequence());

        // Decrement stock in catalog
        boolean borrowed = resource.borrowResource(studentId);
        if (!borrowed) {
            throw new IllegalStateException("Could not allocate resource. Inventory may be depleted.");
        }

        Rental rental = new Rental(
                rentalId,
                resourceId,
                studentId.trim(),
                now.toString(),
                expectedReturn.toString(),
                null,
                Rental.STATUS_ACTIVE,
                0.0
        );

        try {
            rentalRepository.addRental(rental);
        } catch (Exception e) {
            // Roll back in-memory inventory decrement on failure
            resource.returnResource(studentId);
            throw new RuntimeException("Failed to persist rental: " + e.getMessage(), e);
        }

        return rental;
    }

    public double calculateFine(Rental rental) {
        if (rental == null) {
            return 0.0;
        }

        // If already returned, use the locked fine amount
        if (Rental.STATUS_RETURNED.equals(rental.getStatus())) {
            return rental.getFineAmount();
        }

        long overdueDays = calculateOverdueDays(rental);
        return overdueDays > 0 ? overdueDays * DAILY_FINE_RATE : 0.0;
    }

    public long calculateOverdueDays(Rental rental) {
        if (rental == null || rental.getExpectedReturnDate() == null) {
            return 0;
        }

        LocalDate expected = LocalDate.parse(rental.getExpectedReturnDate());
        LocalDate effectiveReturn;

        if (rental.getActualReturnDate() != null) {
            effectiveReturn = LocalDate.parse(rental.getActualReturnDate());
        } else {
            effectiveReturn = LocalDate.now();
        }

        if (effectiveReturn.isAfter(expected)) {
            return ChronoUnit.DAYS.between(expected, effectiveReturn);
        }
        return 0;
    }

    public boolean isOverdue(Rental rental) {
        return calculateOverdueDays(rental) > 0;
    }

    public Rental processReturn(String rentalId) {
        if (rentalId == null || rentalId.trim().isEmpty()) {
            throw new IllegalArgumentException("Rental ID is required.");
        }

        Rental rental = rentalRepository.findById(rentalId.trim());
        if (rental == null) {
            throw new IllegalArgumentException("Rental record not found: " + rentalId);
        }
        if (Rental.STATUS_RETURNED.equals(rental.getStatus())) {
            throw new IllegalStateException("Rental " + rentalId + " has already been returned.");
        }

        LocalDate today = LocalDate.now();
        LocalDate expected = LocalDate.parse(rental.getExpectedReturnDate());

        long overdueDays = 0;
        if (today.isAfter(expected)) {
            overdueDays = ChronoUnit.DAYS.between(expected, today);
        }
        double fine = overdueDays * DAILY_FINE_RATE;

        boolean updated = rentalRepository.updateReturn(rentalId, today.toString(), fine);
        if (!updated) {
            throw new IllegalStateException("Failed to update rental status in database.");
        }

        // Restore quantity in catalog
        Resource resource = resourceService.findById(rental.getResourceId());
        if (resource != null) {
            resource.returnResource(rental.getStudentId());
        }

        // Update local object view
        rental.setStatus(Rental.STATUS_RETURNED);
        rental.setActualReturnDate(today.toString());
        rental.setFineAmount(fine);

        return rental;
    }

    public List<Rental> getRentalsByStudent(String studentId) {
        return rentalRepository.getRentalsByStudent(studentId);
    }

    public List<Rental> getAllRentals() {
        return rentalRepository.getAllRentals();
    }
}

