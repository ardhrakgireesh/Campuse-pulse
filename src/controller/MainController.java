package controller;

import model.Admin;
import model.Request;
import model.Resource;
import model.User;
import model.Volunteer;
import view.MainView;
import service.ResourceService;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

import util.AppConstants;

public class MainController {

    private MainView              view;
    private User                  currentUser;
    private model.UserRepository userRepository;
    private AuthController        authController;

    private ResourceService resourceService = new ResourceService();
    private service.RequestService requestService;
    private service.HandoverService handoverService;
    private model.RentalRepository rentalRepository;
    private service.RentalService rentalService;

    private List<Request> requests = new ArrayList<>();
    private List<Volunteer> volunteers = new ArrayList<>();

    private int requestCounter = 1;

    public MainController(MainView view) {
        this.view           = view;
        this.currentUser    = null;
        this.userRepository = new model.UserRepository();
        this.authController = new AuthController(userRepository);
        this.requestService = new service.RequestService(resourceService);
        this.handoverService = new service.HandoverService(requestService, resourceService);
        this.rentalRepository = new model.RentalRepository();
        this.rentalService = new service.RentalService(rentalRepository, resourceService);
        initializeSampleData();
    }

    private void initializeSampleData() {
        if (userRepository.getAllUsers().isEmpty()) {
            userRepository.addUser(new User("U001", "Test Student", "st@ex.com", "555-0101", "psw", AppConstants.ROLE_STUDENT));
            userRepository.addUser(new User("U002", "Test Admin", "ad@ex.com", "555-0102", "psw", AppConstants.ROLE_ADMIN));
            userRepository.addUser(new User("U003", "Test Volunteer", "vol@ex.com", "555-0103", "psw", AppConstants.ROLE_VOLUNTEER));
            userRepository.addUser(new User("U004", "Test Staff", "sf@ex.com", "555-0104", "psw", AppConstants.ROLE_STAFF));
            userRepository.addUser(new User("U005", "Volunteer 2", "vl@ex.com", "555-0105", "psw", AppConstants.ROLE_VOLUNTEER));
            userRepository.addUser(new User("U006", "Staff 2", "stf@ex.com", "555-0106", "psw", AppConstants.ROLE_STAFF));
            userRepository.deactivateUser("U006");
        }

        if (resourceService.getAllResources().isEmpty()) {
            resourceService.addResource(new Resource(
                "RES-001", "Scientific Calculator", "Casio fx-991EX ClassWiz",
                "Electronics", "U004", 5, AppConstants.RESOURCE_STATUS_AVAILABLE
            ));
            resourceService.addResource(new Resource(
                "RES-002", "Digital Multimeter", "Auto-ranging multimeter with probe leads",
                "Electronics", "U004", 2, AppConstants.RESOURCE_STATUS_AVAILABLE
            ));
            resourceService.addResource(new Resource(
                "RES-003", "Algorithms Textbook", "Introduction to Algorithms (CLRS 4th Edition)",
                "Books", "U004", 3, AppConstants.RESOURCE_STATUS_AVAILABLE
            ));
            resourceService.addResource(new Resource(
                "RES-004", "Basketball & Pump", "Spalding standard outdoor basketball with pump",
                "Sports", "U004", 4, AppConstants.RESOURCE_STATUS_AVAILABLE
            ));
            resourceService.addResource(new Resource(
                "RES-005", "Arduino Starter Kit", "Arduino Uno with sensor pack and breadboard",
                "Electronics", "U004", 2, AppConstants.RESOURCE_STATUS_AVAILABLE
            ));
        }

        User volUser = userRepository.findById("U003");
        if (volUser != null && volunteers.isEmpty()) {
            Volunteer v = registerVolunteer(volUser);
            if (v != null) {
                List<String> skills = new ArrayList<>();
                skills.add("First Aid");
                skills.add("Lab Assistant");
                skills.add("Tutoring");
                v.setSkills(skills);
            }
        }

        if (requests.isEmpty()) {
            createRequest(AppConstants.REQUEST_TYPE_GENERAL, "U001",
                "Need guidance with lab setup for physics experiment", "Lab Assistant");
            createRequest(AppConstants.REQUEST_TYPE_EMERGENCY, "U001",
                "First aid needed for minor sprain on sports ground", "First Aid");
        }

        // Reconcile catalog quantities with active persistent rentals across restarts
        if (rentalRepository != null) {
            List<model.Rental> activeRentals = rentalRepository.getActiveRentals();
            for (model.Rental rental : activeRentals) {
                Resource r = resourceService.findById(rental.getResourceId());
                if (r != null && r.getQuantity() > 0) {
                    r.borrowResource(rental.getStudentId());
                }
            }
        }
    }

    public ResourceService getResourceService() {
        return resourceService;
    }

    public service.RequestService getRequestService() {
        return requestService;
    }

    public service.HandoverService getHandoverService() {
        return handoverService;
    }

    public service.RentalService getRentalService() {
        return rentalService;
    }

    public model.RentalRepository getRentalRepository() {
        return rentalRepository;
    }

    public Volunteer getVolunteerByUserId(String userId) {
        if (userId == null) {
            return null;
        }
        for (Volunteer v : volunteers) {
            if (userId.equals(v.getUserId())) {
                return v;
            }
        }
        return null;
    }

    public boolean login(String email, String password) {
        boolean success = authController.login(email, password);
        if (success) {
            this.currentUser = authController.getCurrentUser();
        } else {
            this.currentUser = null;
        }
        return success;
    }

    public void logout() {
        authController.logout();
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void addResource(Resource resource) {
        resourceService.addResource(resource);
    }

    public List<Resource> searchResources(String keyword) {
        return resourceService.searchResources(keyword);
    }

    public boolean borrowResource(String resourceId, String borrowerId) {
        Resource resource = resourceService.findById(resourceId);

        if (resource == null) {
            return false;
        }

        return resource.borrowResource(borrowerId);
    }

    public boolean returnResource(String resourceId, String returnerId) {
        Resource resource = resourceService.findById(resourceId);

        if (resource == null) {
            return false;
        }

        return resource.returnResource(returnerId);
    }

    public List<Resource> getResourceCatalog() {
        return resourceService.getAllResources();
    }

    public Request createRequest(
            String requestType,
            String requesterId,
            String description) {

        return createRequest(
                requestType,
                requesterId,
                description,
                null
        );
    }

    public Request createRequest(
            String requestType,
            String requesterId,
            String description,
            String requiredSkill) {

        if (requestType == null
                || requesterId == null
                || description == null
                || description.trim().isEmpty()) {

            return null;
        }

        String requestId =
                String.format("REQ-%03d", requestCounter++);

        String createdAt =
                java.time.LocalDateTime.now()
                        .withNano(0)
                        .toString();

        Request request = new Request(
                requestId,
                requestType,
                requesterId,
                description,
                AppConstants.REQUEST_STATUS_OPEN,
                null,
                createdAt
        );

        request.setRequiredSkill(requiredSkill);

        requests.add(request);

        if (AppConstants.REQUEST_TYPE_EMERGENCY.equals(requestType)) {
            assignWaitingRequests();
        }

        return request;
    }

    public boolean assignVolunteer(
            String requestId,
            String volunteerId) {

        Request request = findRequest(requestId);
        Volunteer volunteer = findVolunteer(volunteerId);

        if (request == null || volunteer == null) {
            return false;
        }

        if (!AppConstants.REQUEST_STATUS_OPEN.equals(
                request.getStatus())) {
            return false;
        }

        if (!AppConstants.VOLUNTEER_AVAILABLE.equals(
                volunteer.getAvailabilityStatus())) {
            return false;
        }

        request.assignVolunteer(volunteerId);

        request.updateRequestStatus(
                AppConstants.REQUEST_STATUS_ASSIGNED
        );

        volunteer.addAssignedRequest(requestId);

        volunteer.setAvailabilityStatus(
                AppConstants.VOLUNTEER_BUSY
        );

        return true;
    }

    public void updateRequestStatus(
            String requestId,
            String newStatus) {

        Request request = findRequest(requestId);

        if (request == null || newStatus == null) {
            return;
        }

        String currentStatus = request.getStatus();

        boolean allowed =
                (
                    AppConstants.REQUEST_STATUS_OPEN.equals(currentStatus)
                    &&
                    (
                        AppConstants.REQUEST_STATUS_ASSIGNED.equals(newStatus)
                        || AppConstants.REQUEST_STATUS_CANCELLED.equals(newStatus)
                    )
                )
                ||
                (
                    AppConstants.REQUEST_STATUS_ASSIGNED.equals(currentStatus)
                    &&
                    (
                        AppConstants.REQUEST_STATUS_RESOLVED.equals(newStatus)
                        || AppConstants.REQUEST_STATUS_CANCELLED.equals(newStatus)
                    )
                );

        if (!allowed) {
            return;
        }

        request.updateRequestStatus(newStatus);

        if (AppConstants.REQUEST_STATUS_RESOLVED.equals(newStatus)
                || AppConstants.REQUEST_STATUS_CANCELLED.equals(newStatus)) {

            Volunteer volunteer =
                    findVolunteer(request.getAssignedVolunteerId());

            if (volunteer != null) {

                volunteer.removeAssignedRequest(requestId);

                if (volunteer.getAssignedRequestIds().isEmpty()) {
                    volunteer.setAvailabilityStatus(
                            AppConstants.VOLUNTEER_AVAILABLE
                    );
                }
            }

            assignWaitingRequests();
        }
    }

    /*
     * Heuristic volunteer matching:
     * - Only available volunteers are considered
     * - Required skill match awards +100 points
     * - Workload penalty: -1 point per assigned request
     * - Fallback: emergencies can match without required skill if no specialist is available
     */
    public Volunteer matchVolunteer(Request request) {

        if (request == null) {
            return null;
        }

        String skill = request.getRequiredSkill();

        boolean needsSkill =
                skill != null && !skill.trim().isEmpty();

        boolean emergency =
                AppConstants.REQUEST_TYPE_EMERGENCY.equals(
                        request.getRequestType()
                );

        Volunteer bestVolunteer = null;
        int bestScore = Integer.MIN_VALUE;

        for (Volunteer volunteer : volunteers) {

            if (!AppConstants.VOLUNTEER_AVAILABLE.equals(
                    volunteer.getAvailabilityStatus())) {
                continue;
            }

            boolean skillMatch =
                    needsSkill && volunteer.hasSkill(skill);

            if (needsSkill && !skillMatch && !emergency) {
                continue;
            }

            int currentScore = score(volunteer, skill);

            if (bestVolunteer == null
                    || currentScore > bestScore) {

                bestVolunteer = volunteer;
                bestScore = currentScore;
            }
        }

        return bestVolunteer;
    }

    private int score(Volunteer volunteer, String skill) {

        int score = 0;

        if (skill != null
                && !skill.trim().isEmpty()
                && volunteer.hasSkill(skill)) {

            score += 100;
        }

        if (volunteer.getAssignedRequestIds() != null) {
            score -= volunteer.getAssignedRequestIds().size();
        }

        return score;
    }

    public List<Request> getRequestsByPriority() {

        List<Request> sortedRequests = new ArrayList<>(requests);

        sortedRequests.sort(
                Comparator
                    .comparingInt(
                        (Request request) ->
                            AppConstants.REQUEST_TYPE_EMERGENCY.equals(
                                    request.getRequestType()
                            ) ? 0 : 1
                    )
                    .thenComparing(
                        Request::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                    )
        );

        return sortedRequests;
    }

    public void assignWaitingRequests() {

        for (Request request : getRequestsByPriority()) {

            if (!AppConstants.REQUEST_STATUS_OPEN.equals(
                    request.getStatus())) {
                continue;
            }

            if (!AppConstants.REQUEST_TYPE_EMERGENCY.equals(
                    request.getRequestType())) {
                continue;
            }

            Volunteer match = matchVolunteer(request);

            if (match != null) {
                assignVolunteer(
                        request.getRequestId(),
                        match.getVolunteerId()
                );
            }
        }
    }

    public Volunteer registerVolunteer(User user) {

        if (user == null) {
            return null;
        }

        for (Volunteer volunteer : volunteers) {

            if (volunteer.getUserId().equals(user.getUserId())) {
                return volunteer;
            }
        }

        String volunteerId =
                String.format("VOL-%03d", volunteers.size() + 1);

        Volunteer volunteer = new Volunteer(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getPassword(),
                volunteerId,
                AppConstants.VOLUNTEER_AVAILABLE
        );

        volunteers.add(volunteer);

        assignWaitingRequests();

        return volunteer;
    }

    public boolean updateVolunteerSkills(
            String volunteerId,
            List<String> skills) {

        Volunteer volunteer = findVolunteer(volunteerId);

        if (volunteer == null || skills == null) {
            return false;
        }

        volunteer.setSkills(skills);

        assignWaitingRequests();

        return true;
    }

    public List<Request> getRequests() {
        return requests;
    }

    public List<Volunteer> getVolunteers() {
        return volunteers;
    }

    private Request findRequest(String id) {

        if (id == null) {
            return null;
        }

        for (Request request : requests) {

            if (id.equals(request.getRequestId())) {
                return request;
            }
        }

        return null;
    }

    private Volunteer findVolunteer(String id) {

        if (id == null) {
            return null;
        }

        for (Volunteer volunteer : volunteers) {

            if (id.equals(volunteer.getVolunteerId())) {
                return volunteer;
            }
        }

        return null;
    }

    public List<User> getAllUsers() {
        return userRepository.getAllUsers();
    }

    public void deactivateUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        userRepository.deactivateUser(userId);
    }

    public void activateUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        userRepository.activateUser(userId);
    }

    public boolean isUserActive(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        return userRepository.isUserActive(userId);
    }

    public model.Rental rentResource(String resourceId, int durationDays) {
        if (currentUser == null) {
            throw new IllegalStateException("You must be logged in to rent resources.");
        }
        return rentalService.rentResource(currentUser.getUserId(), resourceId, durationDays);
    }

    public model.Rental processRentalReturn(String rentalId) {
        return rentalService.processReturn(rentalId);
    }

    public List<model.Rental> getRentalsForCurrentStudent() {
        if (currentUser == null) {
            return java.util.Collections.emptyList();
        }
        return rentalService.getRentalsByStudent(currentUser.getUserId());
    }

    public List<model.Rental> getAllRentals() {
        return rentalService.getAllRentals();
    }

    public void start() {
        view.showLoginScreen();
    }
}