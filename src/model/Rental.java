package model;

public class Rental {

    public static final String STATUS_ACTIVE   = "ACTIVE";
    public static final String STATUS_RETURNED = "RETURNED";

    private String rentalId;
    private String resourceId;
    private String studentId;
    private String rentalDate;          // ISO-8601: "YYYY-MM-DD"
    private String expectedReturnDate;  // ISO-8601: "YYYY-MM-DD"
    private String actualReturnDate;    // ISO-8601: "YYYY-MM-DD", nullable while active
    private String status;              // ACTIVE, RETURNED
    private double fineAmount;          // Final calculated fine (0.0 if on-time)

    public Rental() {}

    public Rental(String rentalId, String resourceId, String studentId,
                  String rentalDate, String expectedReturnDate,
                  String actualReturnDate, String status, double fineAmount) {
        this.rentalId           = rentalId;
        this.resourceId         = resourceId;
        this.studentId          = studentId;
        this.rentalDate         = rentalDate;
        this.expectedReturnDate = expectedReturnDate;
        this.actualReturnDate   = actualReturnDate;
        this.status             = status;
        this.fineAmount         = fineAmount;
    }

    public String getRentalId() { return rentalId; }
    public void setRentalId(String rentalId) { this.rentalId = rentalId; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getRentalDate() { return rentalDate; }
    public void setRentalDate(String rentalDate) { this.rentalDate = rentalDate; }

    public String getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(String expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }

    public String getActualReturnDate() { return actualReturnDate; }
    public void setActualReturnDate(String actualReturnDate) { this.actualReturnDate = actualReturnDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getFineAmount() { return fineAmount; }
    public void setFineAmount(double fineAmount) { this.fineAmount = fineAmount; }

    @Override
    public String toString() {
        return "Rental{id='" + rentalId + "', resource='" + resourceId + "', student='" + studentId
                + "', due='" + expectedReturnDate + "', status='" + status + "', fine=" + fineAmount + "}";
    }
}

