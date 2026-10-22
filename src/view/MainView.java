package view;

import controller.MainController;
import model.Rental;
import model.Request;
import model.Resource;
import model.ResourceRequest;
import model.User;
import model.Volunteer;
import util.AppConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.List;

public class MainView {

    private final JFrame frame;
    private final JPanel mainPanel;
    private final CardLayout cardLayout;
    private MainController controller;

    private static final Color HEADER_BG = new Color(44, 52, 60);
    private static final Color HEADER_FG = Color.WHITE;
    private static final Color HEADER_SUB = new Color(180, 185, 190);

    public MainView() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        if (!GraphicsEnvironment.isHeadless()) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            frame = new JFrame(AppConstants.APP_NAME);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);
            frame.setMinimumSize(new Dimension(850, 600));
            frame.setLocationRelativeTo(null);
            frame.setContentPane(mainPanel);
        } else {
            frame = null;
        }
    }

    public void setController(MainController controller) {
        this.controller = controller;
    }

    private JPanel createHeader(String screenTitle) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setBorder(new EmptyBorder(10, 16, 10, 16));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 2, 2));
        titlePanel.setOpaque(false);

        JLabel appTitle = new JLabel(AppConstants.APP_NAME + " • " + screenTitle);
        appTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        appTitle.setForeground(HEADER_FG);
        titlePanel.add(appTitle);

        User user = controller != null ? controller.getCurrentUser() : null;
        String userDetails = user != null
                ? "User: " + user.getName() + " (" + user.getEmail() + ") | Role: " + user.getRole()
                : "Not Authenticated";
        JLabel subTitle = new JLabel(userDetails);
        subTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subTitle.setForeground(HEADER_SUB);
        titlePanel.add(subTitle);

        header.add(titlePanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);

        JButton logoutBtn = new JButton(AppConstants.LABEL_LOGOUT);
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> {
            if (controller != null) {
                controller.logout();
            }
            showLoginScreen();
        });

        rightPanel.add(logoutBtn);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(26);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
    }

    public void showLoginScreen() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(new Color(245, 245, 247));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 215, 220), 1),
                new EmptyBorder(25, 30, 25, 30)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel(AppConstants.APP_NAME, SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(HEADER_BG);

        JLabel subtitle = new JLabel("Campus Resource & Volunteer Management", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(Color.DARK_GRAY);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        card.add(title, gbc);

        gbc.gridy = 1;
        card.add(subtitle, gbc);

        JLabel emailLabel = new JLabel("Email Address:");
        emailLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        JTextField emailField = new JTextField(20);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        JPasswordField passField = new JPasswordField(20);

        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        card.add(emailLabel, gbc);
        gbc.gridx = 1;
        card.add(emailField, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        card.add(passLabel, gbc);
        gbc.gridx = 1;
        card.add(passField, gbc);

        JButton loginBtn = new JButton("Log In");
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        loginBtn.setFocusPainted(false);

        JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(Color.DARK_GRAY);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        card.add(loginBtn, gbc);

        gbc.gridy = 5;
        card.add(statusLabel, gbc);

        Runnable performLogin = () -> {
            if (controller == null) {
                showError("The controller has not been initialized.");
                return;
            }
            String email = emailField.getText().trim();
            String password = new String(passField.getPassword());

            if (email.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Please enter both email and password.");
                return;
            }

            boolean success = controller.login(email, password);
            if (success) {
                User user = controller.getCurrentUser();
                if (user == null) {
                    showError("Login succeeded, but no user record returned.");
                    return;
                }
                statusLabel.setText(" ");
                emailField.setText("");
                passField.setText("");

                String role = user.getRole();
                if (AppConstants.ROLE_STUDENT.equals(role)) {
                    showStudentDashboard();
                } else if (AppConstants.ROLE_VOLUNTEER.equals(role)) {
                    showVolunteerDashboard();
                } else if (AppConstants.ROLE_STAFF.equals(role)) {
                    showStaffDashboard();
                } else if (AppConstants.ROLE_ADMIN.equals(role)) {
                    showAdminPanel();
                } else {
                    showError("Unknown user role: " + role);
                }
            } else {
                statusLabel.setText("Login failed: invalid credentials or deactivated account.");
            }
        };

        loginBtn.addActionListener(e -> performLogin.run());
        passField.addActionListener(e -> performLogin.run());

        outer.add(card);
        mainPanel.add(outer, "LOGIN");
        cardLayout.show(mainPanel, "LOGIN");
        if (frame != null) {
            frame.setVisible(true);
        }
    }

    public void showStudentDashboard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(createHeader(AppConstants.TITLE_STUDENT_DASHBOARD), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));

        tabs.addTab("Resource Catalog", createStudentCatalogTab());
        tabs.addTab("My Borrowed Items & Handover", createStudentBorrowedTab());
        tabs.addTab("Assistance & Emergencies", createStudentAssistanceTab());
        tabs.addTab("Resource Rentals", createStudentRentalsTab());

        root.add(tabs, BorderLayout.CENTER);
        mainPanel.add(root, "STUDENT_DASHBOARD");
        cardLayout.show(mainPanel, "STUDENT_DASHBOARD");
    }

    private JPanel createStudentCatalogTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JTextField searchField = new JTextField(18);
        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{
                "All Categories", "Electronics", "Books", "Sports", "Lab Equipment"
        });
        JCheckBox availOnlyCheck = new JCheckBox("Available items only", false);
        JButton searchBtn = new JButton(AppConstants.LABEL_SEARCH);
        JButton resetBtn = new JButton("Reset");

        filterBar.add(new JLabel("Search:"));
        filterBar.add(searchField);
        filterBar.add(new JLabel("Category:"));
        filterBar.add(categoryCombo);
        filterBar.add(availOnlyCheck);
        filterBar.add(searchBtn);
        filterBar.add(resetBtn);

        panel.add(filterBar, BorderLayout.NORTH);

        String[] cols = {"Resource ID", "Name", "Category", "Available Qty", "Status", "Owner ID"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        Runnable refreshCatalog = () -> {
            model.setRowCount(0);
            if (controller == null) return;
            String kw = searchField.getText().trim();
            String cat = (String) categoryCombo.getSelectedItem();
            boolean availOnly = availOnlyCheck.isSelected();

            List<Resource> list;
            if (cat != null && !cat.equals("All Categories")) {
                list = controller.getResourceService().searchResources(kw, cat, availOnly);
            } else if (!kw.isEmpty()) {
                list = controller.searchResources(kw);
            } else {
                list = controller.getResourceCatalog();
            }

            for (Resource r : list) {
                if (availOnly && !AppConstants.RESOURCE_STATUS_AVAILABLE.equals(r.getStatus())) {
                    continue;
                }
                model.addRow(new Object[]{
                        r.getResourceId(),
                        r.getResourceName(),
                        r.getCategory(),
                        r.getQuantity(),
                        r.getStatus(),
                        r.getOwnerId()
                });
            }
        };

        searchBtn.addActionListener(e -> refreshCatalog.run());
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            categoryCombo.setSelectedIndex(0);
            availOnlyCheck.setSelected(false);
            refreshCatalog.run();
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton borrowBtn = new JButton("Request to Borrow Selected Item");
        borrowBtn.setFocusPainted(false);

        borrowBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                showMessage("Please select a resource from the catalog to borrow.");
                return;
            }
            String resId = (String) model.getValueAt(selectedRow, 0);
            String resName = (String) model.getValueAt(selectedRow, 1);
            int qty = (int) model.getValueAt(selectedRow, 3);

            if (qty <= 0) {
                showError("'" + resName + "' is currently out of stock.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    "Submit borrow request for: " + resName + " (" + resId + ")?",
                    "Confirm Borrow Request",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                User current = controller.getCurrentUser();
                ResourceRequest req = controller.getRequestService().submitRequest(current.getUserId(), resId);
                if (req != null) {
                    showMessage("Borrow request submitted successfully!\n\n"
                            + "Request ID: " + req.getRequestId() + "\n"
                            + "Resource: " + resName + " (" + resId + ")\n"
                            + "Status: PENDING Staff Approval\n\n"
                            + "Track status and complete handover under 'My Borrowed Items & Handover'.");
                    refreshCatalog.run();
                } else {
                    Resource r = controller.getResourceService().findById(resId);
                    if (r != null && current.getUserId().equals(r.getOwnerId())) {
                        showError("You cannot borrow a resource you own.");
                    } else {
                        showError("Could not submit borrow request. The item may no longer be available.");
                    }
                }
            }
        });

        actionPanel.add(borrowBtn);
        panel.add(actionPanel, BorderLayout.SOUTH);

        panel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing()) {
                refreshCatalog.run();
            }
        });

        refreshCatalog.run();
        return panel;
    }

    private JPanel createStudentBorrowedTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new BorderLayout());
        JLabel info = new JLabel("Your borrow requests and active items. When staff approves, enter the 4-digit OTP code below to collect.");
        info.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JButton refreshBtn = new JButton("Refresh List");
        topBar.add(info, BorderLayout.WEST);
        topBar.add(refreshBtn, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"Request ID", "Resource ID", "Item Name", "Category", "Status", "Handover / Next Step"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        Runnable refreshBorrowed = () -> {
            model.setRowCount(0);
            if (controller == null) return;
            User current = controller.getCurrentUser();
            if (current == null) return;

            List<ResourceRequest> all = controller.getRequestService().getAllRequests();
            for (ResourceRequest req : all) {
                if (current.getUserId().equals(req.getRequesterId())) {
                    Resource r = controller.getResourceService().findById(req.getResourceId());
                    String resName = (r != null) ? r.getResourceName() : "Unknown Item";
                    String category = (r != null) ? r.getCategory() : "-";
                    String status = req.getStatus();
                    String nextStep;
                    if (ResourceRequest.STATUS_PENDING.equals(status)) {
                        nextStep = "Pending Staff Approval";
                    } else if (ResourceRequest.STATUS_ACCEPTED.equals(status)) {
                        nextStep = "Approved! Enter OTP below to collect";
                    } else if (ResourceRequest.STATUS_COMPLETED.equals(status)) {
                        nextStep = "In Possession (Ready to return)";
                    } else if (ResourceRequest.STATUS_DECLINED.equals(status)) {
                        nextStep = "Declined by Staff";
                    } else if (ResourceRequest.STATUS_CANCELLED.equals(status)) {
                        nextStep = "Cancelled";
                    } else if ("RETURNED".equals(status)) {
                        nextStep = "Item Returned";
                    } else {
                        nextStep = status;
                    }

                    model.addRow(new Object[]{
                            req.getRequestId(),
                            req.getResourceId(),
                            resName,
                            category,
                            status,
                            nextStep
                    });
                }
            }
        };

        refreshBtn.addActionListener(e -> refreshBorrowed.run());

        panel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing()) {
                refreshBorrowed.run();
            }
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(2, 1, 6, 6));

        JPanel handoverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        handoverPanel.setBorder(BorderFactory.createTitledBorder("Handover Code Verification (OTP)"));
        JTextField reqIdField = new JTextField(8);
        JTextField codeField = new JTextField(8);
        JButton verifyCodeBtn = new JButton("Verify Handover Code");

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    reqIdField.setText((String) model.getValueAt(row, 0));
                }
            }
        });

        verifyCodeBtn.addActionListener(e -> {
            String qId = reqIdField.getText().trim();
            String code = codeField.getText().trim();
            if (qId.isEmpty() || code.isEmpty()) {
                showError("Please enter both Request ID (e.g. Q1) and the 4-digit code.");
                return;
            }
            User u = controller.getCurrentUser();
            ResourceRequest req = controller.getRequestService().findRequest(qId);
            if (req == null) {
                showError("Request '" + qId + "' not found.");
                return;
            }
            if (!u.getUserId().equals(req.getRequesterId())) {
                showError("Request '" + qId + "' does not belong to you.");
                return;
            }
            if (ResourceRequest.STATUS_COMPLETED.equals(req.getStatus())) {
                showMessage("This request has already been completed. Item is in your possession.");
                return;
            }
            if (!ResourceRequest.STATUS_ACCEPTED.equals(req.getStatus())) {
                showError("Request is not approved by staff yet. Current status: " + req.getStatus());
                return;
            }

            boolean verified = controller.getHandoverService().verify(qId, u.getUserId(), code);
            if (verified) {
                showMessage("Handover verified successfully!\nItem officially transferred. It is now in your possession.");
                codeField.setText("");
                reqIdField.setText("");
                refreshBorrowed.run();
            } else {
                showError("Handover verification failed.\nPlease ensure the 4-digit code provided by staff is correct.");
            }
        });

        handoverPanel.add(new JLabel("Request ID:"));
        handoverPanel.add(reqIdField);
        handoverPanel.add(new JLabel("4-Digit Code:"));
        handoverPanel.add(codeField);
        handoverPanel.add(verifyCodeBtn);

        JPanel returnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton cancelBtn = new JButton("Cancel Selected Request");
        JButton returnBtn = new JButton("Return Selected Resource");

        cancelBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                showMessage("Select a request to cancel.");
                return;
            }
            String qId = (String) model.getValueAt(row, 0);
            String status = (String) model.getValueAt(row, 4);
            if (!ResourceRequest.STATUS_PENDING.equals(status) && !ResourceRequest.STATUS_ACCEPTED.equals(status)) {
                showError("Cannot cancel request with status: " + status);
                return;
            }
            User u = controller.getCurrentUser();
            boolean ok = controller.getRequestService().cancel(qId, u.getUserId());
            if (ok) {
                showMessage("Request " + qId + " has been cancelled.");
                refreshBorrowed.run();
            } else {
                showError("Failed to cancel request " + qId + ".");
            }
        });

        returnBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                showMessage("Select a resource to return.");
                return;
            }
            String qId = (String) model.getValueAt(row, 0);
            String rId = (String) model.getValueAt(row, 1);
            String rName = (String) model.getValueAt(row, 2);
            String status = (String) model.getValueAt(row, 4);

            if (!ResourceRequest.STATUS_COMPLETED.equals(status)) {
                showError("Only items currently in your possession (Status: COMPLETED) can be returned.\nCurrent status: " + status);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    "Return item '" + rName + "' (" + rId + ")?",
                    "Confirm Return",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            User u = controller.getCurrentUser();
            boolean ok = controller.returnResource(rId, u.getUserId());
            if (ok) {
                ResourceRequest req = controller.getRequestService().findRequest(qId);
                if (req != null) {
                    req.setStatus("RETURNED");
                }
                showMessage("Successfully returned '" + rName + "'. Inventory restored.");
                refreshBorrowed.run();
            } else {
                showError("Failed to return resource.");
            }
        });

        returnPanel.add(cancelBtn);
        returnPanel.add(returnBtn);

        bottom.add(handoverPanel);
        bottom.add(returnPanel);
        panel.add(bottom, BorderLayout.SOUTH);

        refreshBorrowed.run();
        return panel;
    }

    private JPanel createStudentAssistanceTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Create New Assistance or Emergency Request"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> typeCombo = new JComboBox<>(new String[]{
                AppConstants.REQUEST_TYPE_GENERAL, AppConstants.REQUEST_TYPE_EMERGENCY
        });
        JComboBox<String> skillCombo = new JComboBox<>(new String[]{
                "None", "First Aid", "Lab Assistant", "Tutoring", "Heavy Lifting", "Technical Support"
        });
        JTextField descField = new JTextField(25);
        JButton submitBtn = new JButton("Submit Request");

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Request Type:"), gbc);
        gbc.gridx = 1;
        form.add(typeCombo, gbc);

        gbc.gridx = 2;
        form.add(new JLabel("Required Skill:"), gbc);
        gbc.gridx = 3;
        form.add(skillCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        form.add(descField, gbc);

        gbc.gridx = 3; gbc.gridwidth = 1;
        form.add(submitBtn, gbc);

        panel.add(form, BorderLayout.NORTH);

        String[] cols = {"Request ID", "Type", "Description", "Required Skill", "Volunteer Assigned", "Status", "Created At"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        Runnable refreshRequests = () -> {
            model.setRowCount(0);
            if (controller == null) return;
            User u = controller.getCurrentUser();
            for (Request req : controller.getRequests()) {
                if (u != null && u.getUserId().equals(req.getRequesterId())) {
                    model.addRow(new Object[]{
                            req.getRequestId(),
                            req.getRequestType(),
                            req.getDescription(),
                            req.getRequiredSkill() != null ? req.getRequiredSkill() : "None",
                            req.getAssignedVolunteerId() != null ? req.getAssignedVolunteerId() : "Unassigned",
                            req.getStatus(),
                            req.getCreatedAt()
                    });
                }
            }
        };

        submitBtn.addActionListener(e -> {
            String desc = descField.getText().trim();
            if (desc.isEmpty()) {
                showError("Please describe the assistance needed.");
                return;
            }
            String type = (String) typeCombo.getSelectedItem();
            String skill = (String) skillCombo.getSelectedItem();
            if ("None".equals(skill)) skill = null;

            User u = controller.getCurrentUser();
            Request req = controller.createRequest(type, u.getUserId(), desc, skill);
            if (req != null) {
                if (AppConstants.REQUEST_TYPE_EMERGENCY.equals(type)) {
                    showMessage("Emergency request created: " + req.getRequestId() + "\nSystem dispatched priority volunteer matching.");
                } else {
                    showMessage("Request submitted successfully: " + req.getRequestId());
                }
                descField.setText("");
                refreshRequests.run();
            } else {
                showError("Failed to submit request.");
            }
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelReqBtn = new JButton("Cancel Selected Request");
        cancelReqBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                showMessage("Select a request to cancel.");
                return;
            }
            String reqId = (String) model.getValueAt(row, 0);
            controller.updateRequestStatus(reqId, AppConstants.REQUEST_STATUS_CANCELLED);
            showMessage("Request " + reqId + " cancelled.");
            refreshRequests.run();
        });
        bottom.add(cancelReqBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        refreshRequests.run();
        return panel;
    }

    private JPanel createStudentRentalsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Panel: Rent New Resource
        JPanel rentForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        rentForm.setBorder(BorderFactory.createTitledBorder("Rent a Campus Resource"));

        JLabel resLabel = new JLabel("Available Resource:");
        JComboBox<String> resCombo = new JComboBox<>();

        JLabel daysLabel = new JLabel("Duration:");
        Integer[] daysOptions = new Integer[]{1, 2, 3, 5, 7, 10, 14};
        JComboBox<Integer> daysCombo = new JComboBox<>(daysOptions);
        daysCombo.setSelectedItem(7);

        JButton rentBtn = new JButton("Confirm Rental");
        rentBtn.setFont(new Font("SansSerif", Font.BOLD, 12));

        rentForm.add(resLabel);
        rentForm.add(resCombo);
        rentForm.add(daysLabel);
        rentForm.add(daysCombo);
        rentForm.add(new JLabel("days (Max 14)"));
        rentForm.add(rentBtn);

        // Center Panel: Student's Rentals Table
        String[] cols = {"Rental ID", "Resource ID", "Item Name", "Rented On", "Due Date", "Returned On", "Status", "Fine"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        Runnable refreshResources = () -> {
            resCombo.removeAllItems();
            if (controller == null) return;
            for (Resource r : controller.getResourceCatalog()) {
                if (AppConstants.RESOURCE_STATUS_AVAILABLE.equals(r.getStatus()) && r.getQuantity() > 0) {
                    resCombo.addItem(r.getResourceId() + " - " + r.getResourceName() + " (Qty: " + r.getQuantity() + ")");
                }
            }
        };

        Runnable refreshRentals = () -> {
            model.setRowCount(0);
            if (controller == null) return;
            List<model.Rental> rentals = controller.getRentalsForCurrentStudent();
            for (model.Rental rnt : rentals) {
                Resource res = controller.getResourceService().findById(rnt.getResourceId());
                String resName = res != null ? res.getResourceName() : rnt.getResourceId();
                String statusDisplay = rnt.getStatus();
                double fine = controller.getRentalService().calculateFine(rnt);

                if (Rental.STATUS_ACTIVE.equals(rnt.getStatus())) {
                    if (controller.getRentalService().isOverdue(rnt)) {
                        long overdueDays = controller.getRentalService().calculateOverdueDays(rnt);
                        statusDisplay = "OVERDUE (" + overdueDays + "d late)";
                    } else {
                        statusDisplay = "ACTIVE";
                    }
                }

                String fineDisplay = fine > 0 ? String.format("₹%.2f", fine) : "₹0.00";
                String returnDateDisplay = rnt.getActualReturnDate() != null ? rnt.getActualReturnDate() : "-";

                model.addRow(new Object[]{
                        rnt.getRentalId(),
                        rnt.getResourceId(),
                        resName,
                        rnt.getRentalDate(),
                        rnt.getExpectedReturnDate(),
                        returnDateDisplay,
                        statusDisplay,
                        fineDisplay
                });
            }
        };

        rentBtn.addActionListener(e -> {
            String selected = (String) resCombo.getSelectedItem();
            if (selected == null || selected.isEmpty()) {
                showError("Please select an available resource to rent.");
                return;
            }
            String resId = selected.split(" - ")[0].trim();
            int duration = (int) daysCombo.getSelectedItem();

            try {
                model.Rental rental = controller.rentResource(resId, duration);
                showMessage("Rental confirmed successfully!\n\n"
                        + "Rental ID: " + rental.getRentalId() + "\n"
                        + "Resource: " + resId + "\n"
                        + "Rental Date: " + rental.getRentalDate() + "\n"
                        + "Due Date: " + rental.getExpectedReturnDate() + "\n\n"
                        + "Please return on or before the due date to avoid overdue fines of ₹2.00/day.");
                refreshResources.run();
                refreshRentals.run();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(rentForm, BorderLayout.CENTER);

        JButton refreshListBtn = new JButton("Refresh List");
        refreshListBtn.addActionListener(e -> {
            refreshResources.run();
            refreshRentals.run();
        });
        JPanel refreshWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshWrapper.add(refreshListBtn);
        topBar.add(refreshWrapper, BorderLayout.SOUTH);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        panel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing()) {
                refreshResources.run();
                refreshRentals.run();
            }
        });

        refreshResources.run();
        refreshRentals.run();
        return panel;
    }

    public void showVolunteerDashboard() {
        JPanel root = new JPanel(new BorderLayout());

        User currentUser = controller != null ? controller.getCurrentUser() : null;
        Volunteer vol = controller != null && currentUser != null
                ? controller.getVolunteerByUserId(currentUser.getUserId())
                : null;

        JPanel statusBanner = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        statusBanner.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 225)));

        JLabel availLabel = new JLabel("Your Availability:");
        availLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        JComboBox<String> availCombo = new JComboBox<>(new String[]{
                AppConstants.VOLUNTEER_AVAILABLE, AppConstants.VOLUNTEER_BUSY, AppConstants.VOLUNTEER_UNAVAILABLE
        });
        if (vol != null && vol.getAvailabilityStatus() != null) {
            availCombo.setSelectedItem(vol.getAvailabilityStatus());
        }

        availCombo.addActionListener(e -> {
            if (vol != null) {
                vol.setAvailabilityStatus((String) availCombo.getSelectedItem());
                showMessage("Availability updated to " + vol.getAvailabilityStatus());
            }
        });

        JLabel skillsLabel = new JLabel(vol != null ? "Skills: " + vol.getSkills() : "Skills: None");
        skillsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        statusBanner.add(availLabel);
        statusBanner.add(availCombo);
        statusBanner.add(Box.createHorizontalStrut(20));
        statusBanner.add(skillsLabel);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(createHeader(AppConstants.TITLE_VOLUNTEER_DASHBOARD), BorderLayout.NORTH);
        topContainer.add(statusBanner, BorderLayout.SOUTH);
        root.add(topContainer, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));

        tabs.addTab("Assigned Tasks", createVolunteerAssignedTab(vol));
        tabs.addTab("Open Requests Queue", createVolunteerQueueTab(vol));
        tabs.addTab("Skills & Profile", createVolunteerSkillsTab(vol, skillsLabel));

        root.add(tabs, BorderLayout.CENTER);
        mainPanel.add(root, "VOLUNTEER_DASHBOARD");
        cardLayout.show(mainPanel, "VOLUNTEER_DASHBOARD");
    }

    private JPanel createVolunteerAssignedTab(Volunteer vol) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] cols = {"Request ID", "Type", "Requester ID", "Description", "Skill Required", "Status", "Created At"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        Runnable refreshAssigned = () -> {
            model.setRowCount(0);
            if (controller == null || vol == null) return;
            for (Request r : controller.getRequests()) {
                if (vol.getVolunteerId().equals(r.getAssignedVolunteerId())) {
                    model.addRow(new Object[]{
                            r.getRequestId(),
                            r.getRequestType(),
                            r.getRequesterId(),
                            r.getDescription(),
                            r.getRequiredSkill() != null ? r.getRequiredSkill() : "None",
                            r.getStatus(),
                            r.getCreatedAt()
                    });
                }
            }
        };

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton resolveBtn = new JButton("Mark Task as Resolved");
        resolveBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                showMessage("Select a task to resolve.");
                return;
            }
            String rId = (String) model.getValueAt(row, 0);
            controller.updateRequestStatus(rId, AppConstants.REQUEST_STATUS_RESOLVED);
            showMessage("Task " + rId + " marked as Resolved.");
            refreshAssigned.run();
        });

        actions.add(resolveBtn);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        refreshAssigned.run();
        return panel;
    }

    private JPanel createVolunteerQueueTab(Volunteer vol) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] cols = {"Request ID", "Priority / Type", "Requester ID", "Description", "Required Skill", "Status", "Created At"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        Runnable refreshQueue = () -> {
            model.setRowCount(0);
            if (controller == null) return;
            for (Request r : controller.getRequestsByPriority()) {
                if (AppConstants.REQUEST_STATUS_OPEN.equals(r.getStatus())) {
                    String typeDisplay = AppConstants.REQUEST_TYPE_EMERGENCY.equals(r.getRequestType())
                            ? "[EMERGENCY] " + r.getRequestType()
                            : r.getRequestType();
                    model.addRow(new Object[]{
                            r.getRequestId(),
                            typeDisplay,
                            r.getRequesterId(),
                            r.getDescription(),
                            r.getRequiredSkill() != null ? r.getRequiredSkill() : "None",
                            r.getStatus(),
                            r.getCreatedAt()
                    });
                }
            }
        };

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton assignMeBtn = new JButton("Self-Assign Selected Task");

        assignMeBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                showMessage("Select an open task from the queue to accept.");
                return;
            }
            if (vol == null) {
                showError("Volunteer profile not initialized.");
                return;
            }
            String rId = (String) model.getValueAt(row, 0);
            boolean ok = controller.assignVolunteer(rId, vol.getVolunteerId());
            if (ok) {
                showMessage("Assigned task " + rId + " to you.");
                refreshQueue.run();
            } else {
                showError("Could not assign task (you may be BUSY, or task was already assigned).");
            }
        });

        actions.add(assignMeBtn);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        refreshQueue.run();
        return panel;
    }

    private JPanel createVolunteerSkillsTab(Volunteer vol, JLabel skillsBadge) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Volunteer Qualifications & Skills");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        JTextArea skillDisplay = new JTextArea(4, 30);
        skillDisplay.setEditable(false);
        skillDisplay.setFont(new Font("SansSerif", Font.PLAIN, 13));
        if (vol != null) {
            skillDisplay.setText(String.join("\n - ", vol.getSkills()));
        }
        gbc.gridy = 1;
        panel.add(new JScrollPane(skillDisplay), gbc);

        JTextField addSkillField = new JTextField(15);
        JButton addSkillBtn = new JButton("Add Skill");

        addSkillBtn.addActionListener(e -> {
            String sk = addSkillField.getText().trim();
            if (!sk.isEmpty() && vol != null) {
                vol.addSkill(sk);
                addSkillField.setText("");
                skillDisplay.setText(" - " + String.join("\n - ", vol.getSkills()));
                skillsBadge.setText("Skills: " + vol.getSkills());
                showMessage("Skill '" + sk + "' added.");
            }
        });

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputRow.add(new JLabel("New Skill:"));
        inputRow.add(addSkillField);
        inputRow.add(addSkillBtn);

        gbc.gridy = 2;
        panel.add(inputRow, gbc);

        return panel;
    }

    public void showStaffDashboard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(createHeader(AppConstants.TITLE_STAFF_DASHBOARD), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));

        tabs.addTab("Resource Inventory", createStaffInventoryTab());
        tabs.addTab("Borrow Requests & Handovers", createStaffHandoversTab());
        tabs.addTab("Campus Activity", createStaffActivityTab());
        tabs.addTab("Rental Management", createStaffRentalsTab());

        root.add(tabs, BorderLayout.CENTER);
        mainPanel.add(root, "STAFF_DASHBOARD");
        cardLayout.show(mainPanel, "STAFF_DASHBOARD");
    }

    private JPanel createStaffInventoryTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID", "Name", "Category", "Quantity", "Status", "Owner ID"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        Runnable refreshInventory = () -> {
            model.setRowCount(0);
            if (controller == null) return;
            for (Resource r : controller.getResourceCatalog()) {
                model.addRow(new Object[]{
                        r.getResourceId(),
                        r.getResourceName(),
                        r.getCategory(),
                        r.getQuantity(),
                        r.getStatus(),
                        r.getOwnerId()
                });
            }
        };

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addBtn = new JButton("+ Add New Resource");

        addBtn.addActionListener(e -> {
            JTextField idField = new JTextField("RES-" + (controller.getResourceCatalog().size() + 1));
            JTextField nameField = new JTextField(15);
            JTextField catField = new JTextField("Electronics");
            JTextField descField = new JTextField(15);
            JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));

            JPanel form = new JPanel(new GridLayout(5, 2, 4, 4));
            form.add(new JLabel("Resource ID:")); form.add(idField);
            form.add(new JLabel("Name:")); form.add(nameField);
            form.add(new JLabel("Category:")); form.add(catField);
            form.add(new JLabel("Description:")); form.add(descField);
            form.add(new JLabel("Quantity:")); form.add(qtySpinner);

            int result = JOptionPane.showConfirmDialog(frame, form, "Add New Resource", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String id = idField.getText().trim();
                String name = nameField.getText().trim();
                String cat = catField.getText().trim();
                int qty = (int) qtySpinner.getValue();
                if (id.isEmpty() || name.isEmpty()) {
                    showError("ID and Name are required.");
                    return;
                }
                User current = controller.getCurrentUser();
                Resource res = new Resource(id, name, descField.getText().trim(), cat,
                        current.getUserId(), qty, AppConstants.RESOURCE_STATUS_AVAILABLE);
                controller.addResource(res);
                showMessage("Resource '" + name + "' added successfully.");
                refreshInventory.run();
            }
        });

        actions.add(addBtn);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        panel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing()) {
                refreshInventory.run();
            }
        });

        refreshInventory.run();
        return panel;
    }

    private JPanel createStaffHandoversTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new BorderLayout());
        JLabel info = new JLabel("Approve borrow requests and generate 4-digit handover codes for students.");
        info.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JButton refreshBtn = new JButton("Refresh Requests");
        topBar.add(info, BorderLayout.WEST);
        topBar.add(refreshBtn, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"Request ID", "Resource ID", "Item Name", "Requester ID", "Owner ID", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        Runnable refreshRequests = () -> {
            model.setRowCount(0);
            if (controller == null) return;
            for (ResourceRequest req : controller.getRequestService().getAllRequests()) {
                Resource r = controller.getResourceService().findById(req.getResourceId());
                String resName = (r != null) ? r.getResourceName() : "Unknown Item";
                String ownerId = (r != null) ? r.getOwnerId() : "-";
                model.addRow(new Object[]{
                        req.getRequestId(),
                        req.getResourceId(),
                        resName,
                        req.getRequesterId(),
                        ownerId,
                        req.getStatus()
                });
            }
        };

        refreshBtn.addActionListener(e -> refreshRequests.run());

        panel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing()) {
                refreshRequests.run();
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton declineBtn = new JButton("Decline Request");
        JButton approveBtn = new JButton("Approve & Generate Handover Code");

        approveBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                showMessage("Select a borrow request from the table.");
                return;
            }
            String qId = (String) model.getValueAt(row, 0);
            User current = controller.getCurrentUser();
            ResourceRequest req = controller.getRequestService().findRequest(qId);
            if (req == null) {
                showError("Request not found.");
                return;
            }
            Resource r = controller.getResourceService().findById(req.getResourceId());
            if (r == null || !r.getOwnerId().equals(current.getUserId())) {
                showError("Only the resource owner (" + (r != null ? r.getOwnerId() : "N/A") + ") can approve this request.");
                return;
            }

            if (ResourceRequest.STATUS_PENDING.equals(req.getStatus())) {
                boolean accepted = controller.getRequestService().accept(qId, current.getUserId());
                if (!accepted) {
                    showError("Could not accept request. The resource may no longer have available units.");
                    return;
                }
            } else if (!ResourceRequest.STATUS_ACCEPTED.equals(req.getStatus())) {
                showError("Cannot generate handover code for request with status: " + req.getStatus());
                return;
            }

            String code = controller.getHandoverService().generateCode(qId, current.getUserId());
            if (code != null) {
                JOptionPane.showMessageDialog(
                        frame,
                        "Request " + qId + " is APPROVED.\n\n4-Digit Handover Code for Student: " + code + "\n\nGive this code to the student to verify and collect the item.",
                        "Handover Code Generated",
                        JOptionPane.INFORMATION_MESSAGE
                );
                refreshRequests.run();
            } else {
                showError("Failed to generate handover code.");
            }
        });

        declineBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                showMessage("Select a borrow request from the table to decline.");
                return;
            }
            String qId = (String) model.getValueAt(row, 0);
            User current = controller.getCurrentUser();
            boolean declined = controller.getRequestService().decline(qId, current.getUserId());
            if (declined) {
                showMessage("Request " + qId + " has been declined.");
                refreshRequests.run();
            } else {
                showError("Could not decline request (must be owner and status must be PENDING).");
            }
        });

        actions.add(declineBtn);
        actions.add(approveBtn);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        refreshRequests.run();
        return panel;
    }

    private JPanel createStaffActivityTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] cols = {"Request ID", "Type", "Requester ID", "Description", "Assigned Volunteer", "Status", "Date"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        if (controller != null) {
            for (Request r : controller.getRequests()) {
                model.addRow(new Object[]{
                        r.getRequestId(),
                        r.getRequestType(),
                        r.getRequesterId(),
                        r.getDescription(),
                        r.getAssignedVolunteerId() != null ? r.getAssignedVolunteerId() : "None",
                        r.getStatus(),
                        r.getCreatedAt()
                });
            }
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStaffRentalsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new BorderLayout());
        JLabel info = new JLabel("All active and completed resource rentals. Daily fine rate: ₹2.00 per overdue day.");
        info.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JButton refreshBtn = new JButton("Refresh Rentals");
        topBar.add(info, BorderLayout.WEST);
        topBar.add(refreshBtn, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"Rental ID", "Student ID", "Resource ID", "Item Name", "Rented On", "Due Date", "Returned On", "Status", "Fine"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        Runnable refreshRentals = () -> {
            model.setRowCount(0);
            if (controller == null) return;
            List<model.Rental> rentals = controller.getAllRentals();
            for (model.Rental rnt : rentals) {
                Resource res = controller.getResourceService().findById(rnt.getResourceId());
                String resName = res != null ? res.getResourceName() : rnt.getResourceId();
                String statusDisplay = rnt.getStatus();
                double fine = controller.getRentalService().calculateFine(rnt);

                if (Rental.STATUS_ACTIVE.equals(rnt.getStatus())) {
                    if (controller.getRentalService().isOverdue(rnt)) {
                        long overdueDays = controller.getRentalService().calculateOverdueDays(rnt);
                        statusDisplay = "OVERDUE (" + overdueDays + "d late)";
                    } else {
                        statusDisplay = "ACTIVE";
                    }
                }

                String fineDisplay = fine > 0 ? String.format("₹%.2f", fine) : "₹0.00";
                String returnDateDisplay = rnt.getActualReturnDate() != null ? rnt.getActualReturnDate() : "-";

                model.addRow(new Object[]{
                        rnt.getRentalId(),
                        rnt.getStudentId(),
                        rnt.getResourceId(),
                        resName,
                        rnt.getRentalDate(),
                        rnt.getExpectedReturnDate(),
                        returnDateDisplay,
                        statusDisplay,
                        fineDisplay
                });
            }
        };

        refreshBtn.addActionListener(e -> refreshRentals.run());

        panel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing()) {
                refreshRentals.run();
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton returnBtn = new JButton("Process Resource Return");
        returnBtn.setFont(new Font("SansSerif", Font.BOLD, 12));

        returnBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                showMessage("Please select an active rental from the table to process its return.");
                return;
            }
            String rentalId = (String) model.getValueAt(row, 0);
            String status = (String) model.getValueAt(row, 7);
            String fineStr = (String) model.getValueAt(row, 8);

            if (status.startsWith("RETURNED")) {
                showError("Rental " + rentalId + " is already returned.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    "Process return for Rental: " + rentalId + "?\nCalculated Fine: " + fineStr + "\n\nThis will restore the resource to the catalog.",
                    "Confirm Resource Return",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    model.Rental returned = controller.processRentalReturn(rentalId);
                    String message = "Resource returned successfully!\n\nRental ID: " + returned.getRentalId()
                            + "\nActual Return Date: " + returned.getActualReturnDate()
                            + "\nFinal Overdue Fine: ₹" + String.format("%.2f", returned.getFineAmount())
                            + "\n\nInventory has been restored in the catalog.";
                    showMessage(message);
                    refreshRentals.run();
                } catch (Exception ex) {
                    showError(ex.getMessage());
                }
            }
        });

        actions.add(returnBtn);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        refreshRentals.run();
        return panel;
    }

    public void showAdminPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(createHeader(AppConstants.TITLE_ADMIN_PANEL), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));

        tabs.addTab("User Management", createAdminUserManagementTab());
        tabs.addTab("System Statistics", createAdminStatsTab());
        tabs.addTab("System Oversight", createAdminOversightTab());

        root.add(tabs, BorderLayout.CENTER);
        mainPanel.add(root, "ADMIN_PANEL");
        cardLayout.show(mainPanel, "ADMIN_PANEL");
    }

    private JPanel createAdminUserManagementTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JComboBox<String> roleFilter = new JComboBox<>(new String[]{"ALL ROLES", "STUDENT", "STAFF", "VOLUNTEER", "ADMIN"});
        JButton refreshBtn = new JButton("Refresh Users");

        filterBar.add(new JLabel("Filter by Role:"));
        filterBar.add(roleFilter);
        filterBar.add(refreshBtn);

        panel.add(filterBar, BorderLayout.NORTH);

        String[] cols = {"User ID", "Name", "Email", "Phone", "Role", "Active Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        Runnable refreshUsers = () -> {
            model.setRowCount(0);
            if (controller == null) return;
            String selRole = (String) roleFilter.getSelectedItem();
            List<User> users = controller.getAllUsers();
            for (User u : users) {
                if (selRole != null && !selRole.equals("ALL ROLES") && !selRole.equalsIgnoreCase(u.getRole())) {
                    continue;
                }
                boolean active = controller.isUserActive(u.getUserId());
                model.addRow(new Object[]{
                        u.getUserId(),
                        u.getName(),
                        u.getEmail(),
                        u.getPhone(),
                        u.getRole(),
                        active ? "ACTIVE" : "DEACTIVATED"
                });
            }
        };

        refreshBtn.addActionListener(e -> refreshUsers.run());
        roleFilter.addActionListener(e -> refreshUsers.run());

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton activateBtn = new JButton("Activate Selected User");
        activateBtn.setFocusPainted(false);

        activateBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                showMessage("Please select a user to activate.");
                return;
            }
            String userId = (String) model.getValueAt(row, 0);
            String name = (String) model.getValueAt(row, 1);
            String status = (String) model.getValueAt(row, 5);

            if ("ACTIVE".equalsIgnoreCase(status)) {
                showMessage("User " + name + " (" + userId + ") is already active.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    "Activate account for: " + name + " (" + userId + ")?\nThis will allow them to log in again.",
                    "Confirm Activation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                controller.activateUser(userId);
                showMessage("User " + name + " (" + userId + ") has been activated successfully.");
                refreshUsers.run();
            }
        });

        JButton deactBtn = new JButton("Deactivate Selected User");
        deactBtn.setFocusPainted(false);

        deactBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                showMessage("Please select a user to deactivate.");
                return;
            }
            String userId = (String) model.getValueAt(row, 0);
            String name = (String) model.getValueAt(row, 1);
            String role = (String) model.getValueAt(row, 4);
            String status = (String) model.getValueAt(row, 5);

            if ("DEACTIVATED".equalsIgnoreCase(status)) {
                showMessage("User " + name + " (" + userId + ") is already deactivated.");
                return;
            }

            if ("ADMIN".equalsIgnoreCase(role)) {
                showError("Admin accounts cannot be deactivated directly.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    "Are you sure you want to deactivate user: " + name + " (" + userId + ")?\nThis will prevent them from logging in.",
                    "Confirm Deactivation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                controller.deactivateUser(userId);
                showMessage("User " + name + " (" + userId + ") has been deactivated.");
                refreshUsers.run();
            }
        });

        actions.add(activateBtn);
        actions.add(deactBtn);
        panel.add(actions, BorderLayout.SOUTH);

        refreshUsers.run();
        return panel;
    }

    private JPanel createAdminStatsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] cols = {"Metric Category", "Current Value / Count", "Details & Scope", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        Runnable refreshStats = () -> {
            model.setRowCount(0);
            if (controller == null) return;

            int totalUsers = controller.getAllUsers().size();
            int totalVolunteers = controller.getVolunteers().size();
            int activeVolunteers = 0;
            for (Volunteer v : controller.getVolunteers()) {
                if (AppConstants.VOLUNTEER_AVAILABLE.equals(v.getAvailabilityStatus())) {
                    activeVolunteers++;
                }
            }

            int totalResources = controller.getResourceCatalog().size();
            int availableResources = 0;
            for (Resource r : controller.getResourceCatalog()) {
                if (AppConstants.RESOURCE_STATUS_AVAILABLE.equals(r.getStatus())) {
                    availableResources++;
                }
            }

            int totalRequests = controller.getRequests().size();
            int openEmergency = 0;
            int openGeneral = 0;
            int resolved = 0;
            for (Request req : controller.getRequests()) {
                if (AppConstants.REQUEST_STATUS_OPEN.equals(req.getStatus())) {
                    if (AppConstants.REQUEST_TYPE_EMERGENCY.equals(req.getRequestType())) {
                        openEmergency++;
                    } else {
                        openGeneral++;
                    }
                } else if (AppConstants.REQUEST_STATUS_RESOLVED.equals(req.getStatus())) {
                    resolved++;
                }
            }

            model.addRow(new Object[]{
                    "Registered Users",
                    totalUsers + " users",
                    "Persisted in local SQLite database (campuspulse.db)",
                    "Active"
            });
            model.addRow(new Object[]{
                    "Registered Volunteers",
                    totalVolunteers + " total (" + activeVolunteers + " available)",
                    "Volunteers ready for assignment and emergency response",
                    activeVolunteers > 0 ? "Ready" : "Standby"
            });
            model.addRow(new Object[]{
                    "Catalog Resources",
                    totalResources + " items (" + availableResources + " available)",
                    "Equipment, books, and materials in active catalog",
                    "In Stock"
            });
            model.addRow(new Object[]{
                    "Total Service Requests",
                    totalRequests + " requests",
                    "General and emergency assistance requests recorded",
                    resolved + " Resolved"
            });
            model.addRow(new Object[]{
                    "Open Emergency Requests",
                    openEmergency + " open emergencies",
                    "High priority requests awaiting volunteer assignment",
                    openEmergency > 0 ? "Requires Attention" : "Normal"
            });
            model.addRow(new Object[]{
                    "Open General Requests",
                    openGeneral + " open general requests",
                    "Routine assistance requests in open status",
                    "Normal"
            });
        };

        refreshStats.run();

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton refreshBtn = new JButton("Refresh Statistics");
        refreshBtn.addActionListener(e -> refreshStats.run());
        top.add(refreshBtn);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAdminOversightTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setFont(new Font("Monospaced", Font.PLAIN, 12));

        StringBuilder sb = new StringBuilder();
        sb.append("=== CAMPUSPULSE SYSTEM OVERSIGHT ===\n\n");
        sb.append("DATABASE STATUS: SQLite Active (campuspulse.db)\n");
        sb.append("REGISTERED USERS COUNT: ").append(controller != null ? controller.getAllUsers().size() : 0).append("\n");
        sb.append("RESOURCES IN CATALOG: ").append(controller != null ? controller.getResourceCatalog().size() : 0).append("\n");
        sb.append("TOTAL REQUESTS CREATED: ").append(controller != null ? controller.getRequests().size() : 0).append("\n\n");
        sb.append("ACTIVE EMERGENCY AUTO-DISPATCH RULES:\n");
        sb.append(" - 100 pt score for matching volunteer skill\n");
        sb.append(" - Workload balancing penalty per assigned request\n");
        sb.append(" - Fallback to available volunteer during emergencies\n");

        info.setText(sb.toString());
        panel.add(new JScrollPane(info), BorderLayout.CENTER);
        return panel;
    }

    public void showResourceCatalog() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(createHeader(AppConstants.TITLE_RESOURCE_CATALOG), BorderLayout.NORTH);
        root.add(createStudentCatalogTab(), BorderLayout.CENTER);

        mainPanel.add(root, "RESOURCE_CATALOG");
        cardLayout.show(mainPanel, "RESOURCE_CATALOG");
    }

    public void showRequestList() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(createHeader(AppConstants.TITLE_REQUEST_LIST), BorderLayout.NORTH);
        root.add(createStudentAssistanceTab(), BorderLayout.CENTER);

        mainPanel.add(root, "REQUEST_LIST");
        cardLayout.show(mainPanel, "REQUEST_LIST");
    }

    public void showMessage(String message) {
        if (!GraphicsEnvironment.isHeadless() && frame != null) {
            JOptionPane.showMessageDialog(
                    frame,
                    message,
                    AppConstants.APP_NAME,
                    JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            System.out.println("[CampusPulse] Message: " + message);
        }
    }

    public void showError(String error) {
        if (!GraphicsEnvironment.isHeadless() && frame != null) {
            JOptionPane.showMessageDialog(
                    frame,
                    error,
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } else {
            System.err.println("[CampusPulse] Error: " + error);
        }
    }
}
