package model;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RentalRepository {

    private static final String DEFAULT_DB_PATH = "data/campuspulse.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
        }
    }

    private final String dbPath;

    public RentalRepository() {
        this(System.getProperty("campuspulse.db.path", DEFAULT_DB_PATH));
    }

    public RentalRepository(String dbPath) {
        this.dbPath = (dbPath == null || dbPath.trim().isEmpty())
                ? System.getProperty("campuspulse.db.path", DEFAULT_DB_PATH)
                : dbPath.trim();
        initSchema();
    }

    public String getDbPath() {
        return dbPath;
    }

    private Connection getConnection() throws SQLException {
        if (!":memory:".equals(dbPath)) {
            File dbFile = new File(dbPath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs() && !parentDir.exists()) {
                    throw new SQLException("Failed to create database directory: " + parentDir.getAbsolutePath());
                }
            }
        }
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    public final void initSchema() {
        String createTableSql = "CREATE TABLE IF NOT EXISTS rentals ("
                + "rental_id TEXT PRIMARY KEY, "
                + "resource_id TEXT NOT NULL, "
                + "student_id TEXT NOT NULL, "
                + "rental_date TEXT NOT NULL, "
                + "expected_return_date TEXT NOT NULL, "
                + "actual_return_date TEXT, "
                + "status TEXT NOT NULL, "
                + "fine_amount REAL NOT NULL DEFAULT 0.0"
                + ");";

        String createIndexSql1 = "CREATE INDEX IF NOT EXISTS idx_rentals_student ON rentals(student_id);";
        String createIndexSql2 = "CREATE INDEX IF NOT EXISTS idx_rentals_status ON rentals(status);";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableSql);
            stmt.executeUpdate(createIndexSql1);
            stmt.executeUpdate(createIndexSql2);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize rentals schema: " + e.getMessage(), e);
        }
    }

    public void addRental(Rental rental) {
        if (rental == null) {
            throw new IllegalArgumentException("Rental cannot be null");
        }
        if (rental.getRentalId() == null || rental.getRentalId().trim().isEmpty()) {
            throw new IllegalArgumentException("Rental ID cannot be null or empty");
        }

        String sql = "INSERT INTO rentals (rental_id, resource_id, student_id, rental_date, "
                + "expected_return_date, actual_return_date, status, fine_amount) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, rental.getRentalId().trim());
            pstmt.setString(2, rental.getResourceId().trim());
            pstmt.setString(3, rental.getStudentId().trim());
            pstmt.setString(4, rental.getRentalDate());
            pstmt.setString(5, rental.getExpectedReturnDate());
            pstmt.setString(6, rental.getActualReturnDate());
            pstmt.setString(7, rental.getStatus());
            pstmt.setDouble(8, rental.getFineAmount());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist rental: " + e.getMessage(), e);
        }
    }

    public Rental findById(String rentalId) {
        if (rentalId == null || rentalId.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT rental_id, resource_id, student_id, rental_date, "
                + "expected_return_date, actual_return_date, status, fine_amount "
                + "FROM rentals WHERE rental_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, rentalId.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRental(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query rental by id: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Rental> getRentalsByStudent(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String sql = "SELECT rental_id, resource_id, student_id, rental_date, "
                + "expected_return_date, actual_return_date, status, fine_amount "
                + "FROM rentals WHERE student_id = ? ORDER BY rowid DESC";

        List<Rental> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRental(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query rentals by student: " + e.getMessage(), e);
        }
        return Collections.unmodifiableList(list);
    }

    public List<Rental> getAllRentals() {
        String sql = "SELECT rental_id, resource_id, student_id, rental_date, "
                + "expected_return_date, actual_return_date, status, fine_amount "
                + "FROM rentals ORDER BY rowid DESC";

        List<Rental> list = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRental(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query all rentals: " + e.getMessage(), e);
        }
        return Collections.unmodifiableList(list);
    }

    public List<Rental> getActiveRentals() {
        String sql = "SELECT rental_id, resource_id, student_id, rental_date, "
                + "expected_return_date, actual_return_date, status, fine_amount "
                + "FROM rentals WHERE status = 'ACTIVE' ORDER BY rowid ASC";

        List<Rental> list = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRental(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query active rentals: " + e.getMessage(), e);
        }
        return Collections.unmodifiableList(list);
    }

    public boolean updateReturn(String rentalId, String actualReturnDate, double fineAmount) {
        if (rentalId == null || rentalId.trim().isEmpty()) {
            return false;
        }

        String sql = "UPDATE rentals SET actual_return_date = ?, status = 'RETURNED', fine_amount = ? "
                + "WHERE rental_id = ? AND status = 'ACTIVE'";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, actualReturnDate);
            pstmt.setDouble(2, fineAmount);
            pstmt.setString(3, rentalId.trim());
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update rental return: " + e.getMessage(), e);
        }
    }

    public int getNextRentalSequence() {
        String sql = "SELECT COUNT(*) AS total FROM rentals";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("total") + 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count rentals: " + e.getMessage(), e);
        }
        return 1;
    }

    private Rental mapRental(ResultSet rs) throws SQLException {
        return new Rental(
                rs.getString("rental_id"),
                rs.getString("resource_id"),
                rs.getString("student_id"),
                rs.getString("rental_date"),
                rs.getString("expected_return_date"),
                rs.getString("actual_return_date"),
                rs.getString("status"),
                rs.getDouble("fine_amount")
        );
    }
}

