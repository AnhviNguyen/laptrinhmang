package view;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Pair;
import model.Message;
import model.User;
import tray.animations.AnimationType;
import tray.notification.TrayNotification;
import utilitez.Constant;
import utilitez.Notification;
import java.text.Normalizer;
import java.util.regex.Pattern;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.control.Tooltip;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;

/**
 * FXML Controller class
 *
 * @author Merna
 */
public class ChatSceneController implements Initializable {

    @FXML
    private ImageView imgUser;
    @FXML
    private Label homeLabel;
    @FXML
    private ListView<String> requestsListview;
    @FXML
    private Tab requestsTab;
    @FXML
    private Tab homeBox;
    @FXML
    private TabPane tabPane;
    @FXML
    private ComboBox<String> comboBoxStatus;
    @FXML
    private VBox leftPane;
    // -----merna-----
    @FXML
    private TitledPane titlePaneFriends;

    @FXML
    private ListView<User> aListViewFriends;

    @FXML
    private TitledPane titlePaneFamily;

    @FXML
    private ListView<User> aListViewFamily;

    @FXML
    private Label serverStatusLabel;

    private static boolean falg = false;

    Map<String, Tab> tabsOpened = new HashMap<>();
    private Map<String, ChatBoxController> tabsControllers = new HashMap<>();

    ObservableList<String> statusList = FXCollections.observableArrayList("online", "offline", "busy");

    // Biến để theo dõi trạng thái hiện tại và ngăn chặn vòng lặp cập nhật
    private String currentSelectedStatus = null;
    private boolean isStatusBeingUpdated = false;

    private ClientView clinetView;

    @FXML
    private ListView<HBox> listViewContacts;
    @FXML
    private ListView<HBox> listViewFamily;

    @FXML
    private TextField txtFieldSearch;

    @FXML
    private ComboBox<String> statusComboBox;

    public ChatSceneController() {
        // get instance form view
        clinetView = ClientView.getInstance();
        clinetView.setChatSceneController(this);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            System.out.println("=== ChatSceneController initializing ===");

            // Kiểm tra trạng thái ClientView
            if (clinetView == null) {
                System.out.println("ERROR: clinetView is null");
                clinetView = ClientView.getInstance();
                if (clinetView == null) {
                    System.out.println("CRITICAL ERROR: Cannot get ClientView instance");
                    return;
                } else {
                    System.out.println("ClientView instance obtained successfully");
                }
            }

            // In thông tin về đăng nhập hiện tại
            User currentUser = clinetView.getUserInformation();
            if (currentUser != null) {
                System.out.println("Current logged in user: " + currentUser.getUsername());
            } else {
                System.out.println("WARNING: currentUser is null - user might not be logged in");
            }

            // Kiểm tra kết nối đến server
            boolean isConnected = clinetView.isConnected();
            System.out.println("Connection to server: " + (isConnected ? "CONNECTED" : "DISCONNECTED"));

            // Debug: Kiểm tra danh sách liên hệ
            ArrayList<utilitez.Pair> contacts = clinetView.getContactsWithType();
            if (contacts != null) {
                System.out.println("Initial contacts list - Size: " + contacts.size());
                for (utilitez.Pair contact : contacts) {
                    User user = (User) contact.getFirst();
                    String type = (String) contact.getSecond();
                    System.out.println("   - " + user.getUsername() + " (" + type + ")");
                }
            } else {
                System.out.println("WARNING: Initial contacts list is null");
            }

            // Tiếp tục với quá trình khởi tạo
            updatePageInfo();
            comboBoxStatus.setItems(statusList);

            try {
                homeBox.setContent(FXMLLoader.load(getClass().getResource("HomeBox.fxml")));
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            updateFriendsRequests();

            // Don't create new ListViews - they are already injected from FXML
            loadAccordionData();

            // Initialize server status label
            ClientView.getInstance().serverStatusLabel = serverStatusLabel;

            // Thiết lập chức năng tìm kiếm cho chỉ khi ListView đã được khởi tạo
            setupSearchField();

            try {
                // Check if ListView fields are null, if so, initialize them with the accordion
                // ones
                if (listViewContacts == null || listViewFamily == null) {
                    System.out.println("ListView contacts or family is null, initializing using alternative methods");
                    // Wait until loadAccordionData finishes before refreshing contacts
                    loadAccordionData();
                    // Don't call refreshContacts here to avoid the NullPointerException
                    return;
                }

                // Only refresh contacts if the ListView fields are not null
                refreshContacts();
            } catch (Exception e) {
                System.out.println("Error during initialization: " + e.getMessage());
                e.printStackTrace();
            }

            // Initialize status combo box
            comboBoxStatus.setItems(FXCollections.observableArrayList("online", "offline", "busy"));

            // Set initial status from user info
            if (clinetView != null && clinetView.getUserInformation() != null) {
                String currentStatus = clinetView.getUserInformation().getStatus();
                if (currentStatus != null) {
                    currentSelectedStatus = currentStatus;
                    comboBoxStatus.setValue(currentStatus);
                    updateStatusStyle(currentStatus);
                }
            }

            // Add listener for status changes
            comboBoxStatus.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.equals(oldValue) && !isStatusBeingUpdated) {
                    // Lưu trạng thái đã chọn
                    currentSelectedStatus = newValue;

                    // Update the style immediately
                    updateStatusStyle(newValue);

                    // Notify server about status change
                    if (clinetView != null) {
                        // Bật cờ để ngăn vòng lặp cập nhật
                        isStatusBeingUpdated = true;
                        clinetView.changeStatus(newValue);
                        // Tắt cờ sau khi hoàn thành
                        isStatusBeingUpdated = false;
                    }

                    // Force the ComboBox to lose focus to close the dropdown
                    Platform.runLater(() -> {
                        comboBoxStatus.hide();
                        // Shift focus away from the combobox
                        comboBoxStatus.getParent().requestFocus();
                    });
                }
            });
        } catch (Exception e) {
            System.out.println("EXCEPTION in initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Kiểm tra trạng thái kết nối và dữ liệu
     * Phương thức này có thể được gọi để chẩn đoán vấn đề
     */
    public void debugConnectionAndData() {
        System.out.println("\n=== DEBUG CONNECTION AND DATA ===");

        try {
            // Kiểm tra ClientView
            if (clinetView == null) {
                System.out.println("ERROR: clinetView is null");
                return;
            }

            // Kiểm tra kết nối đến server
            boolean isConnected = clinetView.isConnected();
            System.out.println("Connection to server: " + (isConnected ? "CONNECTED" : "DISCONNECTED"));

            // Kiểm tra thông tin người dùng hiện tại
            User currentUser = clinetView.getUserInformation();
            if (currentUser != null) {
                System.out.println("Current user: " + currentUser.getUsername());
                System.out.println("Full name: " + currentUser.getFname() + " " + currentUser.getLname());
                System.out.println("Status: " + currentUser.getStatus());
            } else {
                System.out.println("WARNING: currentUser is null - user might not be logged in");
            }

            // Kiểm tra danh sách liên hệ
            ArrayList<utilitez.Pair> contacts = clinetView.getContactsWithType();
            if (contacts != null) {
                System.out.println("Contacts list - Size: " + contacts.size());
                for (utilitez.Pair contact : contacts) {
                    User user = (User) contact.getFirst();
                    String type = (String) contact.getSecond();
                    System.out.println("   - " + user.getUsername() + " (" + type + ")");
                }

                // Kiểm tra ListView
                if (listViewContacts != null && listViewFamily != null) {
                    System.out.println("ListView states:");
                    System.out.println("   - listViewContacts items: " + listViewContacts.getItems().size());
                    System.out.println("   - listViewFamily items: " + listViewFamily.getItems().size());
                } else {
                    System.out.println("ERROR: One or both ListView fields are null");
                }
            } else {
                System.out.println("WARNING: Contacts list is null - might be a connection issue");
            }

            System.out.println("=== DEBUG COMPLETED ===\n");

            // Thử làm mới danh sách liên hệ
            refreshContacts();

        } catch (Exception e) {
            System.out.println("EXCEPTION during debug: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lọc danh sách liên hệ theo từ khóa tìm kiếm
     * 
     * @param searchText Từ khóa tìm kiếm
     */
    private void filterContacts(String searchText) {
        try {
            System.out.println("--- filterContacts started with keyword: '" + searchText + "' ---");

            // Hiển thị thông báo đang tìm kiếm
            Platform.runLater(() -> {
                // Xóa tất cả các mục hiện tại
                if (listViewContacts != null) {
                    listViewContacts.getItems().clear();

                    // Hiển thị thông báo đang tìm kiếm
                    HBox searchingBox = new HBox();
                    searchingBox.setAlignment(Pos.CENTER);
                    searchingBox.setPadding(new Insets(10));
                    searchingBox.setPrefWidth(225);
                    searchingBox.setStyle("-fx-background-color: transparent;");

                    Label searchingLabel = new Label("Đang tìm kiếm...");
                    searchingLabel.setStyle(
                            "-fx-text-fill: #1877f2;" +
                                    "-fx-font-size: 13px;");

                    searchingBox.getChildren().add(searchingLabel);
                    listViewContacts.getItems().add(searchingBox);
                }

                if (listViewFamily != null) {
                    listViewFamily.getItems().clear();
                }
            });

            // Chạy chẩn đoán nếu có vẻ như có vấn đề
            if (searchText.equalsIgnoreCase("debug")) {
                System.out.println("DEBUG COMMAND detected - running diagnostics");
                debugConnectionAndData();

                // Hiển thị thông báo debug trên giao diện
                Platform.runLater(() -> {
                    if (listViewContacts != null) {
                        listViewContacts.getItems().clear();

                        HBox debugBox = new HBox();
                        debugBox.setAlignment(Pos.CENTER);
                        debugBox.setPadding(new Insets(10));
                        debugBox.setPrefWidth(225);
                        debugBox.setStyle("-fx-background-color: transparent;");

                        Label debugLabel = new Label("Debug info đã được in ra trong terminal");
                        debugLabel.setStyle(
                                "-fx-text-fill: #1877f2;" +
                                        "-fx-font-size: 13px;");

                        debugBox.getChildren().add(debugLabel);
                        listViewContacts.getItems().add(debugBox);
                    }
                });

                return;
            }

            // Lấy danh sách liên hệ
            ArrayList<utilitez.Pair> contacts = clinetView.getContactsWithType();

            if (contacts == null || contacts.isEmpty()) {
                System.out.println("WARNING: Contact list is null or empty");

                // Hiển thị thông báo không có liên hệ
                Platform.runLater(() -> {
                    if (listViewContacts != null) {
                        listViewContacts.getItems().clear();

                        HBox noContactsBox = new HBox();
                        noContactsBox.setAlignment(Pos.CENTER);
                        noContactsBox.setPadding(new Insets(10));
                        noContactsBox.setPrefWidth(225);
                        noContactsBox.setStyle("-fx-background-color: transparent;");

                        Label noContactsLabel = new Label("Không có liên hệ nào trong danh sách");
                        noContactsLabel.setStyle(
                                "-fx-text-fill: #65676b;" +
                                        "-fx-font-size: 13px;");

                        noContactsBox.getChildren().add(noContactsLabel);
                        listViewContacts.getItems().add(noContactsBox);
                    }
                });

                return;
            }

            System.out.println("Total contacts to search through: " + contacts.size());

            // Thực hiện tìm kiếm trên một thread riêng để không làm chậm UI
            new Thread(() -> {
                // Đếm số kết quả tìm thấy
                int resultsCount = 0;

                // Danh sách lưu trữ kết quả tìm thấy
                ArrayList<HBox> contactResults = new ArrayList<>();
                ArrayList<HBox> familyResults = new ArrayList<>();

                // Lọc và hiển thị các liên hệ phù hợp
                for (utilitez.Pair contact : contacts) {
                    User user = (User) contact.getFirst();
                    String type = (String) contact.getSecond();

                    // Tìm kiếm theo nhiều trường khác nhau
                    String fullName = (user.getFname() + " " + user.getLname()).toLowerCase();
                    String username = user.getUsername().toLowerCase();
                    String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";

                    System.out.println("Checking user: " + username + " (" + fullName + ")");

                    // Loại bỏ dấu tiếng Việt để tìm kiếm không phân biệt dấu
                    String normalizedFullName = removeAccents(fullName);
                    String normalizedSearchText = removeAccents(searchText.toLowerCase());

                    // Kiểm tra nếu bất kỳ trường nào khớp với từ khóa tìm kiếm
                    boolean matches = false;

                    // Tìm kiếm chính xác với dấu
                    if (fullName.contains(searchText) ||
                            username.contains(searchText) ||
                            email.contains(searchText)) {
                        matches = true;
                        System.out.println("MATCH found (exact match): " + username);
                    }

                    // Tìm kiếm không phân biệt dấu
                    if (!matches && (normalizedFullName.contains(normalizedSearchText) ||
                            removeAccents(username).contains(normalizedSearchText) ||
                            removeAccents(email).contains(normalizedSearchText))) {
                        matches = true;
                        System.out.println("MATCH found (accent-insensitive): " + username);
                    }

                    // Tìm kiếm theo từng từ (cho phép tìm kiếm không theo thứ tự)
                    if (!matches && searchText.contains(" ")) {
                        String[] searchWords = normalizedSearchText.split("\\s+");
                        boolean allWordsMatch = true;

                        for (String word : searchWords) {
                            if (word.trim().isEmpty())
                                continue;

                            if (!normalizedFullName.contains(word) &&
                                    !removeAccents(username).contains(word) &&
                                    !removeAccents(email).contains(word)) {
                                allWordsMatch = false;
                                break;
                            }
                        }

                        if (allWordsMatch) {
                            matches = true;
                            System.out.println("MATCH found (multi-word): " + username);
                        }
                    }

                    if (matches) {
                        final HBox contactBox = createContactBox(user, type);

                        // Thêm vào danh sách kết quả thích hợp
                        if (type.equals("Family")) {
                            familyResults.add(contactBox);
                        } else {
                            contactResults.add(contactBox);
                        }

                        resultsCount++;
                    }
                }

                // Cập nhật UI trên thread chính
                final int finalResultsCount = resultsCount;
                Platform.runLater(() -> {
                    // Xóa tất cả các mục hiện tại
                    if (listViewContacts != null) {
                        listViewContacts.getItems().clear();
                    }

                    if (listViewFamily != null) {
                        listViewFamily.getItems().clear();
                    }

                    // Hiển thị thông báo nếu không tìm thấy kết quả
                    if (finalResultsCount == 0) {
                        // Tạo box hiển thị không tìm thấy kết quả
                        HBox noResultsBox = new HBox();
                        noResultsBox.setAlignment(Pos.CENTER);
                        noResultsBox.setPadding(new Insets(20));
                        noResultsBox.setPrefWidth(225);
                        noResultsBox.setStyle("-fx-background-color: transparent;");

                        Label noResultsLabel = new Label("Không tìm thấy kết quả phù hợp");
                        noResultsLabel.setStyle(
                                "-fx-text-fill: #65676b;" +
                                        "-fx-font-size: 13px");

                        // Thêm nút để làm mới danh sách liên hệ
                        Button refreshButton = new Button("Hiển thị tất cả liên hệ");
                        refreshButton.setStyle(
                                "-fx-background-color: #1877f2;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-padding: 5 10;" +
                                        "-fx-background-radius: 5;" +
                                        "-fx-cursor: hand;");
                        refreshButton.setOnAction(e -> refreshContacts());

                        VBox noResultsContainer = new VBox(10);
                        noResultsContainer.setAlignment(Pos.CENTER);
                        noResultsContainer.getChildren().addAll(noResultsLabel, refreshButton);

                        noResultsBox.getChildren().clear();
                        noResultsBox.getChildren().add(noResultsContainer);

                        // Hiển thị thông báo trong cả hai danh sách
                        if (listViewContacts != null) {
                            listViewContacts.getItems().add(noResultsBox);
                        }

                        if (listViewFamily != null) {
                            HBox noResultsBoxFamily = new HBox();
                            noResultsBoxFamily.setAlignment(Pos.CENTER);
                            noResultsBoxFamily.setPadding(new Insets(20));
                            noResultsBoxFamily.setPrefWidth(225);
                            noResultsBoxFamily.setStyle("-fx-background-color: transparent;");

                            VBox noResultsContainerFamily = new VBox(10);
                            noResultsContainerFamily.setAlignment(Pos.CENTER);
                            noResultsContainerFamily.getChildren().add(new Label("Không tìm thấy kết quả"));

                            noResultsBoxFamily.getChildren().add(noResultsContainerFamily);
                            listViewFamily.getItems().add(noResultsBoxFamily);
                        }

                        // Mở cả hai TitledPane
                        if (titlePaneFriends != null) {
                            titlePaneFriends.setExpanded(true);
                        }
                        if (titlePaneFamily != null) {
                            titlePaneFamily.setExpanded(true);
                        }
                    } else {
                        // Hiển thị số lượng kết quả tìm thấy ở cả hai vị trí
                        HBox resultsCountBox = new HBox();
                        resultsCountBox.setAlignment(Pos.CENTER);
                        resultsCountBox.setPadding(new Insets(5, 0, 10, 0));
                        resultsCountBox.setPrefWidth(225);
                        resultsCountBox.setStyle("-fx-background-color: transparent;");

                        Label resultsCountLabel = new Label("Tìm thấy " + finalResultsCount + " kết quả");
                        resultsCountLabel.setStyle(
                                "-fx-text-fill: #1877f2;" +
                                        "-fx-font-size: 12px;" +
                                        "-fx-font-weight: bold;");

                        resultsCountBox.getChildren().add(resultsCountLabel);

                        // Thêm các kết quả tìm kiếm
                        if (listViewContacts != null) {
                            // Thêm vào đầu danh sách
                            listViewContacts.getItems().add(0, resultsCountBox);
                            // Thêm các kết quả liên hệ thường
                            listViewContacts.getItems().addAll(contactResults);
                        }

                        // Thêm các kết quả Family và mở TitledPane nếu có kết quả
                        if (listViewFamily != null) {
                            if (!familyResults.isEmpty()) {
                                // Thêm tiêu đề số lượng kết quả trong Family
                                HBox familyResultsCountBox = new HBox();
                                familyResultsCountBox.setAlignment(Pos.CENTER);
                                familyResultsCountBox.setPadding(new Insets(5, 0, 10, 0));
                                familyResultsCountBox.setPrefWidth(225);
                                familyResultsCountBox.setStyle("-fx-background-color: transparent;");

                                Label familyResultsLabel = new Label(
                                        "Tìm thấy " + familyResults.size() + " kết quả Family");
                                familyResultsLabel.setStyle(
                                        "-fx-text-fill: #1877f2;" +
                                                "-fx-font-size: 12px;" +
                                                "-fx-font-weight: bold;");

                                familyResultsCountBox.getChildren().add(familyResultsLabel);
                                listViewFamily.getItems().add(0, familyResultsCountBox);

                                // Thêm các kết quả Family
                                listViewFamily.getItems().addAll(familyResults);

                                // Mở TitledPane Family nếu có kết quả
                                if (titlePaneFamily != null) {
                                    titlePaneFamily.setExpanded(true);
                                }
                            }
                        }

                        // Mở TitledPane Friends
                        if (titlePaneFriends != null) {
                            titlePaneFriends.setExpanded(true);
                        }
                    }
                });

                System.out.println("Total matches found: " + resultsCount);
                System.out.println("--- filterContacts completed ---");
            }).start();

        } catch (Exception e) {
            System.out.println("ERROR in filterContacts: " + e.getMessage());
            e.printStackTrace();

            // Hiển thị thông báo lỗi
            Platform.runLater(() -> {
                if (listViewContacts != null) {
                    listViewContacts.getItems().clear();

                    HBox errorBox = new HBox();
                    errorBox.setAlignment(Pos.CENTER);
                    errorBox.setPadding(new Insets(10));
                    errorBox.setPrefWidth(225);
                    errorBox.setStyle("-fx-background-color: transparent;");

                    Label errorLabel = new Label("Đã xảy ra lỗi khi tìm kiếm");
                    errorLabel.setStyle(
                            "-fx-text-fill: #e41e3f;" +
                                    "-fx-font-size: 13px;");

                    errorBox.getChildren().add(errorLabel);
                    listViewContacts.getItems().add(errorBox);
                }
            });
        }
    }

    /**
     * Tạo box hiển thị thông tin liên hệ
     */
    private HBox createContactBox(User user, String type) {
        HBox contactBox = new HBox(10);
        contactBox.setAlignment(Pos.CENTER_LEFT);
        contactBox.setPadding(new Insets(8, 12, 8, 12));
        contactBox.setPrefWidth(225);
        contactBox.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8");

        // Thêm hiệu ứng hover
        contactBox.setOnMouseEntered(e -> {
            contactBox.setStyle(
                    "-fx-background-color: #f0f2f5;" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-radius: 8;" +
                            "-fx-cursor: hand");
        });
        contactBox.setOnMouseExited(e -> {
            contactBox.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-radius: 8");
        });

        // User info container
        VBox userInfo = new VBox(2);
        userInfo.setAlignment(Pos.CENTER_LEFT);
        userInfo.setPrefWidth(180);

        // Hiển thị tên đầy đủ
        Label nameLabel = new Label(user.getFname() + " " + user.getLname());
        nameLabel.setStyle(
                "-fx-font-weight: bold;" +
                        "-fx-text-fill: #050505;" +
                        "-fx-font-size: 14px");

        // Username display
        Label usernameLabel = new Label("@" + user.getUsername());
        usernameLabel.setStyle(
                "-fx-text-fill: #65676b;" +
                        "-fx-font-size: 12px");

        userInfo.getChildren().addAll(nameLabel, usernameLabel);

        // Status indicator (small colored circle)
        Circle statusCircle = new Circle(4);
        switch (user.getStatus().toLowerCase()) {
            case "online":
                statusCircle.setFill(Color.web("#31a24c"));
                break;
            case "busy":
                statusCircle.setFill(Color.web("#e41e3f"));
                break;
            default:
                statusCircle.setFill(Color.web("#65676b"));
        }

        // Add all components to contact box
        contactBox.getChildren().addAll(userInfo, statusCircle);

        // Add click handler to open chat
        contactBox.setOnMouseClicked(event -> cellClickAction(event, user.getUsername()));

        return contactBox;
    }

    @FXML
    private void btnLogoutAction(ActionEvent event) {
        ((Node) (event.getSource())).getScene().getWindow().hide();
        clinetView.getMainStage().show();
        clinetView.logout();
        clinetView.changeStatus("offline");
    }

    @FXML
    private void iconLogoutAction(MouseEvent event) {
        ((Node) (event.getSource())).getScene().getWindow().hide();
        clinetView.getMainStage().show();
        clinetView.logout();
        clinetView.changeStatus("offline");
    }

    @FXML
    private void iconCreateGroupAction(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GroupScene.fxml"));
            Parent parent = loader.load();
            Stage stage = new Stage();
            Scene scene = new Scene(parent);
            stage.setScene(scene);
            stage.setTitle("Create New Group");
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void btnCreateGroupAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GroupScene.fxml"));
            Parent parent = loader.load();
            Stage stage = new Stage();
            Scene scene = new Scene(parent);
            stage.setScene(scene);
            stage.setTitle("Create New Group");
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void btnNewFriendAction(ActionEvent event) {
        // Call the existing implementation with a null MouseEvent
        iconAddNewFriendAction(null);
    }

    public void updatePageInfo() {
        User user = clinetView.getUserInformation();
        homeLabel.setText(user.getUsername());
        comboBoxStatus.setValue("online");

        // Update user avatar in the header
        refreshUserProfileImage();
    }

    /**
     * update Friends request from Database
     */
    public void updateFriendsRequests() {
        Platform.runLater(() -> {

            ArrayList<String> requestsArrayList = clinetView.checkRequest();

            if (requestsArrayList != null) {
                requestsTab.setDisable(false);
                ObservableList<String> requestsList = FXCollections.observableArrayList(requestsArrayList);
                requestsListview.setItems(requestsList);
                requestsListview.setCellFactory(listView -> new ListCell<String>() {

                    Button btnAccept = new Button();
                    Button btnIgnore = new Button();

                    @Override
                    public void updateItem(String name, boolean empty) {
                        super.updateItem(name, empty);

                        if (empty || name == null) {
                            setText(null);
                            setGraphic(null);
                        } else {

                            BorderPane pane = new BorderPane();

                            Label labelRequestFrom = new Label();
                            labelRequestFrom.setText(name);

                            btnAccept.setGraphic(new ImageView(new Image("/resouces/accept.png", 9, 9, false, false)));
                            btnAccept.setOnAction(new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent event) {
                                    if (clinetView.acceptRequest(getItem())) {
                                        clinetView.showSuccess("Operation Sccuess",
                                                "Friend Added Successfuly",
                                                "you now become friend with " + getItem());

                                        // update requests
                                        updateFriendsRequests();

                                        // update list of friends
                                        loadAccordionData();
                                    } else {
                                        clinetView.showError("Error", "you can't add friend right now \n"
                                                + "please try again later ..", "");
                                    }
                                }
                            });
                            btnIgnore.setGraphic(new ImageView(new Image("/resouces/ignore.png", 9, 9, false, false)));
                            btnIgnore.setOnAction(new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent event) {
                                    clinetView.ignoreRequest(getItem());
                                    updateFriendsRequests();
                                }
                            });

                            HBox btnHbox = new HBox();

                            btnHbox.getChildren().addAll(btnIgnore, btnAccept);
                            btnHbox.setSpacing(3);
                            pane.setRight(btnHbox);
                            pane.setLeft(labelRequestFrom);
                            setGraphic(pane);

                        }
                    }
                });
            } else {
                requestsTab.setDisable(true);
            }
        });
    }

    public void notify(String message, int type) {
        try {

            switch (type) {
                case Notification.FRIEND_REQUSET:
                    showNotifaction("Friend Request", message,
                            new Image(getClass().getResource("/resouces/add-contact.png").openStream()));
                    updateFriendsRequests();
                    break;
                case Notification.FRIEND_OFFLINE:
                    showNotifaction("Friend Become offline", message,
                            new Image(getClass().getResource("/resouces/closed.png").openStream()));
                    // updateContactsList();
                    loadAccordionData();
                    break;
                case Notification.FRIEND_ONLINE:
                    showNotifaction("Friend Become online", message,
                            new Image(getClass().getResource("/resouces/open.png").openStream()));
                    // updateContactsList();
                    loadAccordionData();
                    break;
                case Notification.ACCEPT_FRIEND_REQUEST:
                    showNotifaction("Accept Request", message,
                            new Image(getClass().getResource("/resouces/accept.png").openStream()));
                    // updateContactsList();
                    loadAccordionData();
                    break;
                case Notification.SERVER_MESSAGE:
                    showNotifaction("New Announcement", message,
                            new Image(getClass().getResource("/resouces/megaphone.png").openStream()));
                    break;
                case Notification.FRIEND_BUSY:
                    // showNotifaction("Friend Become busy", message, new
                    // Image(getClass().getResource("../resouces/add-contact.png").openStream()));
                    // updateContactsList();
                    loadAccordionData();

            }

            // TODO change image to require image
        } catch (IOException ex) {
            Logger.getLogger(ChatSceneController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void showNotifaction(String title, String message, Image image) {
        Platform.runLater(() -> {
            TrayNotification tray = new TrayNotification();
            tray.setTitle(title);
            tray.setMessage(message);
            tray.setRectangleFill(Paint.valueOf("#bdc3c7"));
            tray.setAnimationType(AnimationType.POPUP);
            tray.setImage(image);
            tray.showAndWait();
        });
    }

    public void changeStatus() {
        clinetView.changeStatus(comboBoxStatus.getValue().toString());
    }

    /**
     * get message from clientView and open existing tab or create new tab and
     * load new chatBoxScene on it
     *
     * @param message
     * @throws java.io.IOException
     */
    public void reciveMsg(Message message) throws IOException {

        String tabName;
        String[] groupName = message.getTo().split("##");

        // message sent to group? open tab (group name) : open tab(sender name)
        if (message.getTo().contains("##")) {
            tabName = message.getTo();
        } else {
            tabName = message.getFrom();
        }

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!tabsOpened.containsKey(tabName)) {

                        // create new tab
                        Tab newTab = new Tab();
                        newTab.setId(tabName);

                        if (message.getTo().contains("##")) {
                            newTab.setText(groupName[2]);
                        } else {
                            newTab.setText(tabName);
                        }

                        // Add icon for tab
                        try {
                            ImageView iv = new ImageView(
                                    new Image(getClass().getResource("/resouces/user.png").openStream()));
                            iv.setFitHeight(20);
                            iv.setFitWidth(20);
                            newTab.setGraphic(iv);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                        newTab.setOnCloseRequest(new EventHandler<Event>() {
                            @Override
                            public void handle(Event event) {
                                tabsOpened.remove(newTab.getId());
                                tabsControllers.remove(newTab.getId());
                            }
                        });

                        tabPane.getTabs().add(newTab);
                        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

                        // load new chat box
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("ChatBox.fxml"));
                        ChatBoxController chatBoxController = new ChatBoxController(message);
                        loader.setController(chatBoxController);
                        newTab.setContent(loader.load());
                        chatBoxController.reciveMsg(message);

                        // put the new tab and controller in the map
                        tabsOpened.put(tabName, newTab);
                        tabsControllers.put(tabName, chatBoxController);

                    } else {
                        // tab already exist so open it and pass msg to its controller
                        tabPane.getSelectionModel().select(tabsOpened.get(tabName));
                        tabsControllers.get(tabName).reciveMsg(message);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void createGroup(String groupName) {
        String[] splitString = groupName.split("##");

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!tabsOpened.containsKey(groupName)) {
                        // Create new tab
                        Tab newTab = new Tab();
                        newTab.setId(groupName);
                        // Use the last part of the split string as group name
                        newTab.setText(splitString[splitString.length - 1]);
                        newTab.setClosable(true);

                        // Add icon for tab
                        try {
                            ImageView iv = new ImageView(
                                    new Image(getClass().getResource("/resouces/group.png").openStream()));
                            iv.setFitHeight(20);
                            iv.setFitWidth(20);
                            newTab.setGraphic(iv);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                        // Add close handler
                        newTab.setOnCloseRequest(new EventHandler<Event>() {
                            @Override
                            public void handle(Event event) {
                                tabsOpened.remove(newTab.getId());
                                tabsControllers.remove(newTab.getId());
                            }
                        });

                        // Load ChatBox for this group
                        Message message = new Message();
                        message.setTo(groupName);
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("ChatBox.fxml"));
                        Parent root = loader.load();
                        ChatBoxController chatBoxController = loader.getController();
                        chatBoxController.initializeWithMessage(message);

                        // Set content and store references
                        newTab.setContent(root);
                        tabsOpened.put(groupName, newTab);
                        tabsControllers.put(groupName, chatBoxController);

                        // Add to tabPane and select
                        tabPane.getTabs().add(newTab);
                        tabPane.getSelectionModel().select(newTab);
                    } else {
                        // Tab already exists, just select it
                        tabPane.getSelectionModel().select(tabsOpened.get(groupName));
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public String getSaveLocation(String sender, String filename) {
        try {

            FutureTask saveLocation = new FutureTask(() -> {

                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Recieve File ");
                alert.setHeaderText(sender + " send file to you .. ");
                alert.setContentText("Do you want to download file ?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setInitialFileName(filename);
                    // Show save file dialog
                    File file = fileChooser.showSaveDialog(null);

                    return (file != null) ? file.getAbsolutePath() : null;
                } else {
                    return null;
                }

            });

            Platform.runLater(saveLocation);

            return (String) saveLocation.get();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void loadErrorServer() {
        // ----- close this scene -----
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                // requestsListview.getScene().getWindow().hide();
                if (!falg) {
                    homeLabel.getScene().getWindow().hide();

                    try {
                        Parent parent = FXMLLoader.load(getClass().getResource("OutOfServiceScene.fxml"));
                        Stage stage = new Stage();
                        Scene scene = new Scene(parent);
                        stage.setScene(scene);
                        stage.setResizable(false);
                        stage.setTitle(" ");
                        stage.show();
                        stage.setOnCloseRequest((WindowEvent ew) -> {
                            Platform.exit();
                            // TODO : why not close
                            System.exit(0);
                        });
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    falg = true;
                }
            }
        });

    }

    @FXML
    void iconAddNewFriendAction(MouseEvent event) {

        ObservableList<String> options = FXCollections.observableArrayList(
                "Family",
                "Friends",
                "Block");

        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Add New Friend");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField txtFieldUserName = new TextField();
        txtFieldUserName.setPromptText("username");

        ComboBox comboBox = new ComboBox(options);
        comboBox.getSelectionModel().selectFirst();

        grid.add(new Label("User Name :"), 0, 0);
        grid.add(txtFieldUserName, 1, 0);

        grid.add(new Label("Category:"), 0, 1);
        grid.add(comboBox, 1, 1);

        // dialog.getDialogPane().setStyle(" -fx-background-color: #535f85;");
        dialog.getDialogPane().setContent(grid);

        // Request focus on the txtFieldEmail field by default.
        Platform.runLater(() -> txtFieldUserName.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new Pair<>(txtFieldUserName.getText(),
                        comboBox.getSelectionModel().getSelectedItem().toString());
            }
            return null;
        });

        // Hna H3mal Insert LL Code Bta3e (Send Request)
        Optional<Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(emailCategory -> {

            if (emailCategory.getValue().equals("Block")) {
                clinetView.sendRequest(emailCategory.getKey(), emailCategory.getValue());
                clinetView.showSuccess("Sccuess", "Blocked", "You block user " + emailCategory.getKey());
                return;
            }

            switch (clinetView.sendRequest(emailCategory.getKey(), emailCategory.getValue())) {
                case Constant.ALREADY_FRIENDS:
                    clinetView.showError("Error", "Can't  Send Requset", "User Already Friend to you..");
                    break;
                case Constant.REQUEST_ALREADY_EXIST:
                    clinetView.showError("Error", "Can't  Send Requset", "you Already send request before "
                            + "\nor have request from this person\nor there is a block relation :( ");
                    break;
                case Constant.USER_NOT_EXIST:
                    clinetView.showError("Error", "Can't  Send Requset", "User Not Exsist in our System");
                    break;
                case Constant.EXCEPTION:
                    clinetView.showError("Error", "Can't  Send Requset", "An error Occure please Contact Admin");
                    break;
                case Constant.SENDED:
                    clinetView.showSuccess("Sccuess", "Requset Sended",
                            "You send request to " + emailCategory.getKey());
                    break;
                case Constant.SAME_NAME:
                    clinetView.showError("Error", "Can't  Send Requset", "you can't add your self");
                    break;
            }

        });
    }

    // --- merna ---
    /**
     * update friends contact list
     */
    void loadAccordionData() {
        Platform.runLater(() -> {
            ArrayList<utilitez.Pair> allContact = clinetView.getContactsWithType();
            ArrayList<String> groups = new ArrayList<>(); // Initialize empty groups list

            if (allContact != null) {
                ObservableList<User> friendsList = FXCollections.observableArrayList();
                ObservableList<User> familyList = FXCollections.observableArrayList();
                ObservableList<String> groupsList = FXCollections.observableArrayList();

                // Load contacts
                for (utilitez.Pair contact : allContact) {
                    User user = (User) contact.getFirst();
                    String type = (String) contact.getSecond();

                    if ("Family".equals(type)) {
                        familyList.add(user);
                    } else {
                        friendsList.add(user);
                    }
                }

                // Load groups
                if (groups != null) {
                    groupsList.addAll(groups);
                }

                // Handle Friends list
                if (friendsList.isEmpty()) {
                    try {
                        Node node = FXMLLoader.load(getClass().getResource("EmptyList.fxml"));
                        titlePaneFriends.setContent(node);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    titlePaneFriends.setContent(aListViewFriends);
                    aListViewFriends.setItems(friendsList);
                }

                // Handle Family list
                if (familyList.isEmpty()) {
                    try {
                        Node node = FXMLLoader.load(getClass().getResource("EmptyList.fxml"));
                        titlePaneFamily.setContent(node);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    titlePaneFamily.setContent(aListViewFamily);
                    aListViewFamily.setItems(familyList);
                }

                // Handle Groups list

                // Set cell factories
                aListViewFriends.setCellFactory(listView -> new ListCell<User>() {
                    @Override
                    protected void updateItem(User friend, boolean empty) {
                        super.updateItem(friend, empty);
                        if (empty || friend == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setGraphic(loadCell(friend));
                        }
                    }
                });

                aListViewFamily.setCellFactory(listView -> new ListCell<User>() {
                    @Override
                    protected void updateItem(User friend, boolean empty) {
                        super.updateItem(friend, empty);
                        if (empty || friend == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setGraphic(loadCell(friend));
                        }
                    }
                });

                // Add click handlers
                aListViewFriends.setOnMouseClicked(event -> {
                    User selectedUser = aListViewFriends.getSelectionModel().getSelectedItem();
                    if (selectedUser != null) {
                        cellClickAction(event, selectedUser.getUsername());
                    }
                });

                aListViewFamily.setOnMouseClicked(event -> {
                    User selectedUser = aListViewFamily.getSelectionModel().getSelectedItem();
                    if (selectedUser != null) {
                        cellClickAction(event, selectedUser.getUsername());
                    }
                });

            }
        });
    }

    private HBox loadCell(User friend) {
        HBox contactBox = new HBox(10);
        contactBox.setAlignment(Pos.CENTER_LEFT);
        contactBox.setPadding(new Insets(5, 5, 5, 5));
        contactBox.setPrefHeight(40);
        contactBox.setStyle("-fx-background-color: #f2f2f2; -fx-background-radius: 5;");

        // Create container for avatar
        StackPane avatarContainer = new StackPane();
        avatarContainer.setPrefSize(35, 35);
        avatarContainer.setMaxSize(35, 35);
        avatarContainer.setMinSize(35, 35);

        // Add avatar with proper sizing
        ImageView imageView = new ImageView();
        imageView.setFitWidth(35);
        imageView.setFitHeight(35);
        imageView.setPreserveRatio(false); // Disable preserve ratio to allow cover
        imageView.setSmooth(true); // Enable smooth scaling
        imageView.setCache(false); // Disable caching to ensure fresh image

        try {
            // Check if user has a custom avatar
            if (friend.getImage() != null && !friend.getImage().trim().isEmpty()) {
                String imagePath = friend.getImage();
                if (imagePath.startsWith("http")) {
                    String separator = imagePath.contains("?") ? "&" : "?";
                    imagePath += separator + "t=" + System.currentTimeMillis();
                }
                Image avatar = new Image(imagePath, 35, 35, true, false);
                imageView.setImage(avatar);
            } else {
                // Load default avatar based on gender
                String defaultPath = friend.getGender() != null &&
                        friend.getGender().equalsIgnoreCase("Female") ? "/resouces/female.png" : "/resouces/male.png";

                String fullPath = getClass().getResource(defaultPath).toExternalForm() +
                        "?t=" + System.currentTimeMillis();

                imageView.setImage(new Image(fullPath, true));
            }

            // Create circular clip
            Circle clip = new Circle(17.5);
            clip.setCenterX(17.5);
            clip.setCenterY(17.5);
            imageView.setClip(clip);

            // Create circular border
            Circle border = new Circle(17.5);
            border.setCenterX(17.5);
            border.setCenterY(17.5);
            border.setFill(null);
            border.setStroke(Color.web("#e4e4e4"));
            border.setStrokeWidth(1);

            // Add image and border to container
            avatarContainer.getChildren().addAll(imageView, border);

        } catch (Exception e) {
            System.out.println("Error loading avatar for " + friend.getUsername() + ": " + e.getMessage());
            try {
                imageView.setImage(new Image(getClass().getResource("/resouces/male.png").toExternalForm() +
                        "?t=" + System.currentTimeMillis()));

                // Create circular clip for default image
                Circle clip = new Circle(17.5);
                clip.setCenterX(17.5);
                clip.setCenterY(17.5);
                imageView.setClip(clip);

                // Create circular border for default image
                Circle border = new Circle(17.5);
                border.setCenterX(17.5);
                border.setCenterY(17.5);
                border.setFill(null);
                border.setStroke(Color.web("#e4e4e4"));
                border.setStrokeWidth(1);

                avatarContainer.getChildren().addAll(imageView, border);
            } catch (Exception ex) {
                avatarContainer.getChildren().add(imageView);
            }
        }

        // Add name and status text
        VBox infoBox = new VBox(2);
        Label nameLabel = new Label(friend.getUsername());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");

        Label statusLabel = new Label(friend.getStatus());
        // Adjust style based on status
        if (friend.getStatus().equalsIgnoreCase("online")) {
            statusLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 11;");
        } else if (friend.getStatus().equalsIgnoreCase("busy")) {
            statusLabel.setStyle("-fx-text-fill: #c62828; -fx-font-size: 11;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #616161; -fx-font-size: 11;");
        }

        infoBox.getChildren().addAll(nameLabel, statusLabel);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        // Add all elements to the contact box
        contactBox.getChildren().addAll(avatarContainer, infoBox);

        return contactBox;
    }

    /**
     * Action for clicking on a friend in the contacts list
     * Opens a new chat tab for the selected friend
     * 
     * @param event  - Mouse click event
     * @param friend - Selected friend name
     */
    @FXML
    private void cellClickAction(MouseEvent event, String friend) {
        System.out.println(">> ChatSceneController: cell click action for " + friend);
        try {
            if (tabsOpened.containsKey(friend)) {
                tabPane.getSelectionModel().select(tabsOpened.get(friend));
                return;
            }

            FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("ChatBox.fxml"));

            Tab tab = new Tab();
            tab.setId(friend);

            tab.setOnCloseRequest(new EventHandler<Event>() {
                @Override
                public void handle(Event event) {
                    tabsOpened.remove(friend);
                    tabsControllers.remove(friend);
                }
            });

            tab.setContent(fXMLLoader.load());

            // Create tab header with avatar and username
            HBox tabHeader = new HBox(5);
            tabHeader.setAlignment(Pos.CENTER_LEFT);
            tabHeader.setPadding(new Insets(2, 5, 2, 5));

            // Create and configure avatar
            ImageView avatar = new ImageView();
            avatar.setFitWidth(20);
            avatar.setFitHeight(20);
            avatar.setPreserveRatio(false);
            avatar.setSmooth(true);
            avatar.setCache(false);

            try {
                User user = clinetView.getUser(friend);
                if (user != null && user.getImage() != null && !user.getImage().trim().isEmpty()) {
                    String imagePath = user.getImage();
                    if (imagePath.startsWith("http")) {
                        String separator = imagePath.contains("?") ? "&" : "?";
                        imagePath += separator + "t=" + System.currentTimeMillis();
                    }
                    avatar.setImage(new Image(imagePath));
                } else {
                    String genderImage = (user.getGender() != null &&
                            user.getGender().equalsIgnoreCase("Female")) ? "/resouces/female.png"
                                    : "/resouces/male.png";

                    avatar.setImage(new Image(getClass().getResource(genderImage).toExternalForm() +
                            "?t=" + System.currentTimeMillis(), 20, 20, true, true));
                }

                // Make avatar circular
                Circle clip = new Circle(10);
                clip.setCenterX(10);
                clip.setCenterY(10);
                avatar.setClip(clip);

            } catch (Exception e) {
                System.out.println("Error loading avatar: " + e.getMessage());
                try {
                    avatar.setImage(new Image(getClass().getResource("/resouces/male.png").toExternalForm() +
                            "?t=" + System.currentTimeMillis()));
                    Circle clip = new Circle(10);
                    clip.setCenterX(10);
                    clip.setCenterY(10);
                    avatar.setClip(clip);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            // Create username label
            Label usernameLabel = new Label(friend);
            usernameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333333;");

            // Add components to header
            tabHeader.getChildren().addAll(avatar, usernameLabel);
            tab.setGraphic(tabHeader);

            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
            tabsOpened.put(friend, tab);

            ChatBoxController controller = fXMLLoader.getController();
            controller.setReceiver(friend);
            tabsControllers.put(friend, controller);

            tab.setOnSelectionChanged(e -> {
                if (tab.isSelected()) {
                    Platform.runLater(() -> {
                        controller.enforceConsistentMessageLayout();
                    });
                }
            });

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    // -- end merna ---

    // friendsPane.getChildren().clear();
    // friendsPane.getChildren().add(FXMLLoader.load(getClass().getResource("EmptyList.fxml")));

    @FXML
    private void btnProfileAction(ActionEvent event) {
        try {
            // Load the update profile content into the home tab
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab != homeBox) {
                tabPane.getSelectionModel().select(homeBox);
            }

            // Load the UpdateProfileBox.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UpdateProfileBox.fxml"));
            Parent updateProfileRoot = loader.load();
            homeBox.setContent(updateProfileRoot);
        } catch (IOException ex) {
            ex.printStackTrace();
            clinetView.showError("Error", "Profile Load Error", "Failed to load profile update screen.");
        }
    }

    @FXML
    private Button btnProfile;

    @FXML
    private Button btnNewChat;

    /**
     * Load the home scene content
     */
    public void loadHomeScene() {
        try {
            // Select the home tab
            tabPane.getSelectionModel().select(homeBox);

            // Load the HomeBox.fxml content
            homeBox.setContent(FXMLLoader.load(getClass().getResource("HomeBox.fxml")));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void createChatBoxFXML(String friendName) {
        System.out.println(">> ChatSceneController: create chat box for friend: " + friendName);
        try {
            // if this friend is already opened return
            if (tabsOpened.containsKey(friendName)) {
                tabPane.getSelectionModel().select(tabsOpened.get(friendName));
                return;
            }

            // create the tab
            Tab newTab = new Tab(friendName);

            // load chat box - không chỉ định controller ở đây vì đã chỉ định trong FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ChatBox.fxml"));
            final Parent parentChatBox = loader.load();

            // controller for chat box - lấy controller đã được tạo tự động từ FXML
            ChatBoxController chatBoxController = loader.getController();
            chatBoxController.setChatSceneController(this);
            chatBoxController.init(friendName); // initialize chat box controller

            tabsControllers.put(friendName, chatBoxController);

            // add the loaded chat box to the tab
            newTab.setContent(parentChatBox);

            // prevent tab from being closed
            newTab.setClosable(true);

            // set icon image for tab
            ImageView img = new ImageView(new Image(getClass().getResource("/resouces/user.png").openStream()));
            img.setFitWidth(14);
            img.setFitHeight(14);
            newTab.setGraphic(img);

            // add the new tab
            tabPane.getTabs().add(newTab);
            tabsOpened.put(friendName, newTab);

            // select the new tab
            tabPane.getSelectionModel().select(newTab);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Làm mới danh sách liên hệ, hiển thị tất cả
     */
    public void refreshContacts() {
        try {
            System.out.println("--- refreshContacts() started ---");

            // Lấy danh sách liên hệ từ client view
            ArrayList<utilitez.Pair> contacts = clinetView.getContactsWithType();

            if (contacts == null) {
                System.out.println("ERROR: contacts list is null");
                return;
            }

            if (contacts.isEmpty()) {
                System.out.println("WARNING: contacts list is empty");
            } else {
                System.out.println("Total contacts retrieved: " + contacts.size());
            }

            // Kiểm tra xem ListView đã được khởi tạo chưa
            if (listViewContacts == null || listViewFamily == null) {
                System.out.println("ERROR: ListView controls are null");
                return;
            }

            // Xóa các mục hiện tại
            listViewContacts.getItems().clear();
            listViewFamily.getItems().clear();
            System.out.println("Cleared existing items from ListViews");

            // Đếm số lượng contact đã thêm vào từng danh sách
            int regularCount = 0;
            int familyCount = 0;

            // Hiển thị tất cả người dùng
            for (utilitez.Pair contact : contacts) {
                User user = (User) contact.getFirst();
                String type = (String) contact.getSecond();

                System.out.println("Processing contact: " + user.getUsername() + " | Type: " + type);

                // Tạo mục liên hệ với thông tin người dùng
                HBox contactBox = new HBox(10);
                contactBox.setAlignment(Pos.CENTER_LEFT);
                contactBox.setPadding(new Insets(8, 12, 8, 12));
                contactBox.setPrefWidth(225);
                contactBox.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-radius: 8");

                // Thêm hiệu ứng hover
                contactBox.setOnMouseEntered(e -> {
                    contactBox.setStyle(
                            "-fx-background-color: #f0f2f5;" +
                                    "-fx-background-radius: 8;" +
                                    "-fx-border-radius: 8;" +
                                    "-fx-cursor: hand");
                });
                contactBox.setOnMouseExited(e -> {
                    contactBox.setStyle(
                            "-fx-background-color: transparent;" +
                                    "-fx-background-radius: 8;" +
                                    "-fx-border-radius: 8");
                });

                // User info container
                VBox userInfo = new VBox(2);
                userInfo.setAlignment(Pos.CENTER_LEFT);
                userInfo.setPrefWidth(180);

                // Hiển thị tên đầy đủ
                Label nameLabel = new Label(user.getFname() + " " + user.getLname());
                nameLabel.setStyle(
                        "-fx-font-weight: bold;" +
                                "-fx-text-fill: #050505;" +
                                "-fx-font-size: 14px");

                // Username
                Label usernameLabel = new Label("@" + user.getUsername());
                usernameLabel.setStyle(
                        "-fx-text-fill: #65676b;" +
                                "-fx-font-size: 12px");

                userInfo.getChildren().addAll(nameLabel, usernameLabel);

                // Status indicator
                Circle statusCircle = new Circle(4);
                switch (user.getStatus().toLowerCase()) {
                    case "online":
                        statusCircle.setFill(Color.web("#31a24c"));
                        break;
                    case "busy":
                        statusCircle.setFill(Color.web("#e41e3f"));
                        break;
                    default:
                        statusCircle.setFill(Color.web("#65676b"));
                }

                // Add components to contact box
                contactBox.getChildren().addAll(userInfo, statusCircle);

                // Add click handler to open chat
                contactBox.setOnMouseClicked(event -> cellClickAction(event, user.getUsername()));

                // Add to appropriate list
                if (type.equals("Family")) {
                    listViewFamily.getItems().add(contactBox);
                    familyCount++;
                } else {
                    listViewContacts.getItems().add(contactBox);
                    regularCount++;
                }
            }

            System.out.println("Added contacts - Regular: " + regularCount + ", Family: " + familyCount);
            System.out.println("ListView counts - Regular: " + listViewContacts.getItems().size() +
                    ", Family: " + listViewFamily.getItems().size());
            System.out.println("--- refreshContacts() completed ---");

        } catch (Exception e) {
            System.out.println("ERROR in refreshContacts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void openChatBox(String username) {
        try {
            if (!tabsOpened.containsKey(username)) {
                Tab newTab = new Tab(username);
                newTab.setClosable(true);

                // Add icon
                ImageView iv = new ImageView(new Image(getClass().getResource("/resouces/user.png").openStream()));
                iv.setFitHeight(20);
                iv.setFitWidth(20);
                newTab.setGraphic(iv);

                // Add close handler
                newTab.setOnCloseRequest(event -> {
                    tabsOpened.remove(newTab.getId());
                    tabsControllers.remove(newTab.getId());
                });

                // Load chat box
                FXMLLoader loader = new FXMLLoader(getClass().getResource("ChatBox.fxml"));
                ChatBoxController chatBoxController = new ChatBoxController(username);
                loader.setController(chatBoxController);

                newTab.setContent(loader.load());
                tabsOpened.put(username, newTab);
                tabsControllers.put(username, chatBoxController);

                tabPane.getTabs().add(newTab);
                tabPane.getSelectionModel().select(newTab);
            } else {
                tabPane.getSelectionModel().select(tabsOpened.get(username));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Refresh avatar for a specific user in all contact lists
     * 
     * @param username The username whose avatar needs updating
     */
    public void refreshContactAvatar(String username) {
        Platform.runLater(() -> {
            try {
                System.out.println("Refreshing avatar for user: " + username);

                // Get fresh user data
                User updatedUser = clinetView.getUser(username);
                if (updatedUser == null) {
                    System.out.println("Could not get updated user data for: " + username);
                    return;
                }

                // Clear any cached images for this user
                if (updatedUser.getImage() != null && !updatedUser.getImage().trim().isEmpty()) {
                    String imagePath = updatedUser.getImage();
                    if (imagePath.startsWith("http")) {
                        String separator = imagePath.contains("?") ? "&" : "?";
                        imagePath += separator + "t=" + System.currentTimeMillis();
                    }
                    // Force image reload by creating new Image object
                    new Image(imagePath, true);
                }

                // Refresh in open tabs if available
                if (tabsControllers.containsKey(username)) {
                    ChatBoxController controller = tabsControllers.get(username);
                    controller.refreshUserAvatar(username);
                }

                // Refresh tab icons if this user has an open tab
                refreshTabAvatar(username);

                // Force contact list to reload with new avatars
                refreshContacts();

                // Refresh user profile image if it's the current user
                if (username.equals(clinetView.getUserInformation().getUsername())) {
                    updateUserProfileImage(updatedUser);
                }

                // Force UI redraw to make changes visible
                Stage stage = (Stage) tabPane.getScene().getWindow();
                if (stage != null) {
                    double width = stage.getWidth();
                    stage.setWidth(width + 0.1);
                    stage.setWidth(width);
                }

                System.out.println("Successfully refreshed avatar for user: " + username);
            } catch (Exception ex) {
                System.out.println("Error refreshing contact avatar: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    /**
     * Refreshes the avatar shown in tab headers for a specific user
     * 
     * @param username The username whose tab avatar needs updating
     */
    public void refreshTabAvatar(String username) {
        try {
            System.out.println("Refreshing tab avatars for: " + username);

            // Update all tabs that need the user's avatar
            for (Tab tab : tabPane.getTabs()) {
                // Skip home and requests tabs
                if (tab == homeBox || tab == requestsTab) {
                    continue;
                }

                String tabId = tab.getId();
                if (tabId == null) {
                    tabId = tab.getText();
                }

                // Check if this tab belongs to a friend chat
                boolean isUserTab = (tabId != null && tabId.equals(username));

                // If it's the user's tab, update the avatar
                if (isUserTab) {
                    System.out.println("Found tab for user: " + username);

                    // Get updated user info
                    User user = clinetView.getUser(username);
                    if (user == null) {
                        System.out.println("User info not found for: " + username);
                        continue;
                    }

                    // Load the appropriate image
                    Image avatarImage = null;

                    // Try to use user's custom image first
                    if (user.getImage() != null && !user.getImage().trim().isEmpty()) {
                        try {
                            // Add cache busting parameter
                            String imagePath = user.getImage();
                            if (imagePath.startsWith("http")) {
                                String separator = imagePath.contains("?") ? "&" : "?";
                                imagePath = imagePath + separator + "t=" + System.currentTimeMillis();
                            }
                            avatarImage = new Image(imagePath, 20, 20, true, true);
                            System.out.println("Loaded custom image for tab");
                        } catch (Exception e) {
                            System.out.println("Error loading user avatar for tab: " + e.getMessage());
                        }
                    }

                    // Fallback to default if user image failed or wasn't set
                    if (avatarImage == null) {
                        try {
                            String genderImage = (user.getGender() != null &&
                                    user.getGender().equalsIgnoreCase("Female")) ? "/resouces/female.png"
                                            : "/resouces/male.png";

                            avatarImage = new Image(getClass().getResource(genderImage).toExternalForm() +
                                    "?t=" + System.currentTimeMillis(), 20, 20, true, true);
                            System.out.println("Loaded default image for tab");
                        } catch (Exception e) {
                            System.out.println("Error loading default avatar for tab: " + e.getMessage());
                        }
                    }

                    // Apply the image to the tab
                    if (avatarImage != null) {
                        ImageView iv = new ImageView(avatarImage);
                        iv.setFitHeight(20);
                        iv.setFitWidth(20);
                        tab.setGraphic(iv);
                        System.out.println("Updated tab avatar for: " + username);
                    }
                }
            }

            // Special handling for current user's tabs in other people's lists
            // This ensures avatars are properly refreshed even when the tab belongs to
            // someone else
            // but needs to show the current user's avatar
            String currentUsername = clinetView.getUserInformation().getUsername();
            if (username.equals(currentUsername)) {
                System.out.println("Current user updated their avatar, refreshing all tabs");

                // Update the user's avatar in all tabs
                for (String tabName : tabsOpened.keySet()) {
                    // Skip if this is a group tab
                    if (tabName.contains("##")) {
                        continue;
                    }

                    ChatBoxController chatController = tabsControllers.get(tabName);
                    if (chatController != null) {
                        chatController.refreshCurrentUserAvatar();
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Error refreshing tab avatar: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Updates the current user's profile image in the UI
     * 
     * @param user The user whose image needs updating
     */
    private void updateUserProfileImage(User user) {
        if (imgUser != null && user != null) {
            // Configure ImageView
            imgUser.setFitWidth(35);
            imgUser.setFitHeight(35);
            imgUser.setPreserveRatio(false);
            imgUser.setSmooth(true);
            imgUser.setCache(false);

            // Load with timestamp to force refresh
            if (user.getImage() != null && !user.getImage().trim().isEmpty()) {
                String imagePath = user.getImage();
                if (imagePath.startsWith("http")) {
                    String separator = imagePath.contains("?") ? "&" : "?";
                    imagePath = imagePath + separator + "t=" + System.currentTimeMillis();
                }
                Image img = new Image(imagePath, 35, 35, true, false);
                imgUser.setImage(img);
            } else {
                // Use default image based on gender
                String defaultPath = "/resouces/" +
                        (user.getGender() != null &&
                                user.getGender().equalsIgnoreCase("Female") ? "female.png" : "male.png");
                imgUser.setImage(new Image(getClass().getResource(defaultPath).toExternalForm() +
                        "?t=" + System.currentTimeMillis(), 35, 35, true, false));
            }

            // Create perfect circle clip
            Circle clip = new Circle(17.5);
            clip.setCenterX(17.5);
            clip.setCenterY(17.5);
            imgUser.setClip(clip);

            // Create circular border
            Circle border = new Circle(17.5);
            border.setCenterX(17.5);
            border.setCenterY(17.5);
            border.setFill(null);
            border.setStroke(Color.web("#ffffff"));
            border.setStrokeWidth(2);

            // Create container for avatar
            StackPane avatarContainer = new StackPane();
            avatarContainer.setPrefSize(35, 35);
            avatarContainer.setMaxSize(35, 35);
            avatarContainer.setMinSize(35, 35);

            // Replace the old ImageView with the new container
            if (imgUser.getParent() instanceof StackPane) {
                StackPane parent = (StackPane) imgUser.getParent();
                parent.getChildren().setAll(imgUser, border);
            }

            System.out.println("User profile image updated in header");
        }
    }

    /**
     * Updates the tab icon for the specified friend, ensuring avatar is fresh
     * 
     * @param friendName The username of the friend whose tab icon needs updating
     */
    public void updateFriendTabIcon(String friendName) {
        Platform.runLater(() -> {
            try {
                // First check if we have a direct reference to the tab
                if (tabsOpened.containsKey(friendName)) {
                    Tab tab = tabsOpened.get(friendName);
                    if (tab != null) {
                        // Fetch user info
                        User friend = clinetView.getUser(friendName);
                        if (friend != null) {
                            try {
                                // Create a horizontal box for the tab header
                                HBox tabHeader = new HBox(5);
                                tabHeader.setAlignment(Pos.CENTER_LEFT);

                                // Create and configure the avatar image view
                                ImageView iv = new ImageView();
                                iv.setFitHeight(16);
                                iv.setFitWidth(16);
                                iv.setPreserveRatio(true);
                                iv.setCache(false);

                                // Get appropriate avatar image
                                if (friend.getImage() != null && !friend.getImage().trim().isEmpty()) {
                                    String imagePath = friend.getImage() + "?t=" + System.currentTimeMillis();
                                    iv.setImage(new Image(imagePath, 16, 16, true, false));
                                } else {
                                    String genderPath = friend.getGender() != null &&
                                            friend.getGender().equalsIgnoreCase("Female") ? "/resouces/female.png"
                                                    : "/resouces/male.png";

                                    String fullPath = getClass().getResource(genderPath).toExternalForm() +
                                            "?t=" + System.currentTimeMillis();

                                    iv.setImage(new Image(fullPath, 16, 16, true, false));
                                }

                                // Make the avatar circular
                                Circle clip = new Circle(8, 8, 8);
                                iv.setClip(clip);

                                // Create label for username
                                Label nameLabel = new Label(friendName);
                                nameLabel.setStyle("-fx-font-size: 12;");

                                // Add components to tab header
                                tabHeader.getChildren().addAll(iv, nameLabel);

                                // Set the tab header
                                tab.setGraphic(tabHeader);

                                System.out.println("Updated tab icon via tabsOpened map");
                            } catch (Exception ex) {
                                System.out.println("Error updating tab icon from map: " + ex.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error in updateFriendTabIcon: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Refreshes a friend's avatar in any open chat windows
     * This is a convenience wrapper method for ClientView to use instead of
     * directly accessing maps
     * 
     * @param username The username of the friend whose avatar needs refreshing
     */
    public void refreshFriendAvatar(String username) {
        try {
            // 1. Check if we have this user's controller in our map
            if (tabsControllers.containsKey(username)) {
                System.out.println("Found chat controller for " + username + ", refreshing avatar");
                ChatBoxController controller = tabsControllers.get(username);
                // Refresh the user's avatar in chat
                controller.refreshUserAvatar(username);
            }

            // 2. Refresh all message bubbles in all chats that might contain this user's
            // avatar
            for (ChatBoxController controller : tabsControllers.values()) {
                controller.refreshMessageAvatars(username);
            }

            System.out.println("Completed friend avatar refresh for: " + username);
        } catch (Exception ex) {
            System.out.println("Error in refreshFriendAvatar: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Forces update of all avatars in the accordion panes (TitledPane components)
     * specifically targeting the titlePaneFamily and titlePaneFriends components
     */
    public void refreshAccordionAvatars() {
        Platform.runLater(() -> {
            try {
                System.out.println("Refreshing avatars in accordion panes (TitledPane)");

                // Clear any image cache by removing and re-adding items
                if (aListViewFriends != null) {
                    // Store current items
                    ObservableList<User> friendItems = aListViewFriends.getItems();
                    if (friendItems != null && !friendItems.isEmpty()) {
                        // Force refresh by resetting the items
                        aListViewFriends.setItems(null);
                        aListViewFriends.setItems(friendItems);
                        aListViewFriends.refresh();
                    }
                }

                if (aListViewFamily != null) {
                    // Store current items
                    ObservableList<User> familyItems = aListViewFamily.getItems();
                    if (familyItems != null && !familyItems.isEmpty()) {
                        // Force refresh by resetting the items
                        aListViewFamily.setItems(null);
                        aListViewFamily.setItems(familyItems);
                        aListViewFamily.refresh();
                    }
                }

                // Complete reload data in case the above refreshes aren't enough
                loadAccordionData();

                System.out.println("Accordion pane avatars refresh complete");
            } catch (Exception e) {
                System.out.println("Error refreshing accordion avatars: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Comprehensive avatar refresh method that updates avatars everywhere in the UI
     * This ensures that all instances of the given user's avatar are updated
     * 
     * @param username The username whose avatar needs updating
     */
    public void refreshAllAvatars(String username) {
        Platform.runLater(() -> {
            try {
                System.out.println("Starting comprehensive avatar refresh for: " + username);

                // 1. Update tab icons (highest priority)
                updateFriendTabIcon(username);

                // 2. Update avatars in chat boxes
                if (tabsControllers.containsKey(username)) {
                    ChatBoxController controller = tabsControllers.get(username);
                    controller.refreshUserAvatar(username);
                }

                // Refresh all message bubbles that might contain this user's avatar
                for (ChatBoxController controller : tabsControllers.values()) {
                    controller.refreshMessageAvatars(username);
                }

                // 3. Update contact lists in the sidebar
                refreshContacts();

                // 4. Update the TitledPane lists (accordion)
                refreshAccordionAvatars();

                // 5. Update the user's profile image if it's the current user
                if (username.equals(clinetView.getUserInformation().getUsername())) {
                    refreshUserProfileImage();
                }

                // 6. Ensure tab headers are updated
                refreshTabAvatar(username);

                // 7. Force layout refresh to ensure changes are visible
                if (tabPane != null) {
                    double width = tabPane.getWidth();
                    tabPane.setPrefWidth(width + 0.1);
                    Platform.runLater(() -> tabPane.setPrefWidth(width));
                }

                System.out.println("Comprehensive avatar refresh complete for: " + username);
            } catch (Exception e) {
                System.out.println("Error in refreshAllAvatars: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Thiết lập chức năng tìm kiếm cho ô tìm kiếm
     */
    private void setupSearchField() {
        if (txtFieldSearch == null) {
            System.out.println("Warning: txtFieldSearch is null, cannot setup search field");
            return;
        }

        // Thêm style cho ô tìm kiếm với border và màu nền
        txtFieldSearch.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 8 12 8 12;" +
                        "-fx-font-size: 13px;" +
                        "-fx-prompt-text-fill: #65676b;" +
                        "-fx-text-fill: #050505;" +
                        "-fx-border-color: #65676b;"+
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 4, 0, 0, 1);");

        // Tạo container chứa ô tìm kiếm và nút xóa
        HBox searchContainer = (HBox) txtFieldSearch.getParent();
        if (searchContainer != null) {
            // Xóa ô tìm kiếm hiện tại từ container
            searchContainer.getChildren().remove(txtFieldSearch);

            // Tạo StackPane để đặt ô tìm kiếm và nút xóa lên nhau
            StackPane searchPane = new StackPane();
            searchPane.setAlignment(Pos.CENTER_RIGHT);

            // Biểu tượng xóa tìm kiếm
            Button clearButton = new Button("✕");
            clearButton.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: #65676b;" +
                            "-fx-font-size: 12px;" +
                            "-fx-cursor: hand;");
            clearButton.setVisible(false);
            clearButton.setOnAction(e -> {
                txtFieldSearch.clear();
                clearButton.setVisible(false);
                refreshContacts(); // Hiển thị lại toàn bộ danh sách
            });

            // Đặt vị trí nút xóa
            StackPane.setAlignment(clearButton, Pos.CENTER_RIGHT);
            StackPane.setMargin(clearButton, new Insets(0, 12, 0, 0));

            // Thêm txtFieldSearch vào StackPane
            searchPane.getChildren().addAll(txtFieldSearch, clearButton);

            // Thêm StackPane vào container
            searchContainer.getChildren().add(searchPane);
            searchContainer.setAlignment(Pos.CENTER);

            // Sự kiện khi nội dung thay đổi
            txtFieldSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                // Hiển thị/ẩn nút xóa dựa trên nội dung ô tìm kiếm
                clearButton.setVisible(!newValue.isEmpty());

                // Khi có thay đổi trong ô tìm kiếm, thực hiện tìm kiếm
                String searchText = newValue.trim();

                if (searchText.isEmpty()) {
                    // Nếu ô tìm kiếm trống, hiển thị lại tất cả liên hệ
                    refreshContacts();
                } else {
                    // Lọc danh sách liên hệ theo từ khóa tìm kiếm
                    filterContacts(searchText);
                }
            });
        } else {
            // Fallback nếu không tìm thấy container
            // Thêm listener để bắt sự kiện khi người dùng gõ vào ô tìm kiếm
            txtFieldSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                // Khi có thay đổi trong ô tìm kiếm, thực hiện tìm kiếm
                String searchText = newValue.trim();

                if (searchText.isEmpty()) {
                    // Nếu ô tìm kiếm trống, hiển thị lại tất cả liên hệ
                    refreshContacts();
                } else {
                    // Lọc danh sách liên hệ theo từ khóa tìm kiếm
                    filterContacts(searchText);
                }
            });
        }

        // Đặt gợi ý cho ô tìm kiếm
        txtFieldSearch.setPromptText("Tìm kiếm bạn bè...");

        // Thêm xử lý sự kiện khi nhấn Enter
        txtFieldSearch.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String searchText = txtFieldSearch.getText().trim();
                System.out.println("ENTER key pressed. Search text: '" + searchText + "'");
                if (searchText.isEmpty()) {
                    System.out.println("Search text is empty, refreshing all contacts");
                    refreshContacts();
                } else {
                    System.out.println("Searching for contacts with keyword: " + searchText);
                    filterContacts(searchText);
                }
            }
        });

        // Thêm hiệu ứng focus
        txtFieldSearch.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) { // Khi focus
                txtFieldSearch.setStyle(
                        "-fx-background-color: #ffffff;" +
                                "-fx-background-radius: 20;" +
                                "-fx-padding: 8 12 8 12;" +
                                "-fx-font-size: 13px;" +
                                "-fx-prompt-text-fill: #1877f2;" +
                                "-fx-text-fill: #050505;" +
                                "-fx-border-color: #1877f2;" +
                                "-fx-border-radius: 20;" +
                                "-fx-border-width: 2;" +
                                "-fx-effect: dropshadow(gaussian, rgba(24, 119, 242, 0.25), 8, 0, 0, 2);");
            } else { // Khi mất focus
                txtFieldSearch.setStyle(
                        "-fx-background-color: #ffffff;" +
                                "-fx-background-radius: 20;" +
                                "-fx-padding: 8 12 8 12;" +
                                "-fx-font-size: 13px;" +
                                "-fx-prompt-text-fill: #65676b;" +
                                "-fx-text-fill: #050505;" +
                                "-fx-border-color: #e4e6eb;" +
                                "-fx-border-radius: 20;" +
                                "-fx-border-width: 1;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 4, 0, 0, 1);");
            }
        });

        // Thêm tooltip gợi ý cách tìm kiếm
        final Tooltip searchTip = new Tooltip(
                "Gợi ý tìm kiếm:\n" +
                        "- Tìm kiếm theo tên, username hoặc email\n" +
                        "- Không phân biệt chữ hoa/thường, có dấu/không dấu\n" +
                        "- Có thể tìm theo nhiều từ khóa cùng lúc");
        searchTip.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-padding: 8 12;" +
                        "-fx-background-radius: 6;" +
                        "-fx-background-color: rgba(0, 0, 0, 0.75);" +
                        "-fx-text-fill: white;");
        txtFieldSearch.setTooltip(searchTip);

        // Tự động hiển thị tooltip khi focus
        txtFieldSearch.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Point2D point = txtFieldSearch.localToScreen(
                        txtFieldSearch.getLayoutBounds().getMaxX(),
                        txtFieldSearch.getLayoutBounds().getMaxY());
                searchTip.show(txtFieldSearch, point.getX(), point.getY());

                // Tự động ẩn tooltip sau 3 giây
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        Platform.runLater(() -> searchTip.hide());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                searchTip.hide();
            }
        });
    }

    /**
     * Chuyển đổi chuỗi có dấu tiếng Việt sang không dấu
     * để thực hiện tìm kiếm không phân biệt dấu
     * 
     * @param text Chuỗi cần chuyển đổi
     * @return Chuỗi không dấu
     */
    private String removeAccents(String text) {
        if (text == null) {
            return "";
        }

        String temp = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").toLowerCase()
                .replace('đ', 'd')
                .replace('Đ', 'd');
    }

    /**
     * Refreshes the user's profile image in the header
     */
    public void refreshUserProfileImage() {
        Platform.runLater(() -> {
            try {
                User currentUser = clinetView.getUserInformation();
                if (currentUser != null && imgUser != null) {
                    // Configure ImageView
                    imgUser.setFitWidth(35);
                    imgUser.setFitHeight(35);
                    imgUser.setPreserveRatio(false);
                    imgUser.setSmooth(true);
                    imgUser.setCache(false);

                    // Load with timestamp to force refresh
                    if (currentUser.getImage() != null && !currentUser.getImage().trim().isEmpty()) {
                        String imagePath = currentUser.getImage();
                        if (imagePath.startsWith("http")) {
                            String separator = imagePath.contains("?") ? "&" : "?";
                            imagePath = imagePath + separator + "t=" + System.currentTimeMillis();
                        }
                        Image img = new Image(imagePath, 35, 35, true, false);
                        imgUser.setImage(img);
                    } else {
                        // Use default image based on gender
                        String defaultPath = "/resouces/" +
                                (currentUser.getGender() != null &&
                                        currentUser.getGender().equalsIgnoreCase("Female") ? "female.png" : "male.png");
                        imgUser.setImage(new Image(getClass().getResource(defaultPath).toExternalForm() +
                                "?t=" + System.currentTimeMillis(), 35, 35, true, false));
                    }

                    // Create perfect circle clip
                    Circle clip = new Circle(17.5);
                    clip.setCenterX(17.5);
                    clip.setCenterY(17.5);
                    imgUser.setClip(clip);

                    // Create circular border
                    Circle border = new Circle(17.5);
                    border.setCenterX(17.5);
                    border.setCenterY(17.5);
                    border.setFill(null);
                    border.setStroke(Color.web("#ffffff"));
                    border.setStrokeWidth(2);

                    // Create container for avatar
                    StackPane avatarContainer = new StackPane();
                    avatarContainer.setPrefSize(35, 35);
                    avatarContainer.setMaxSize(35, 35);
                    avatarContainer.setMinSize(35, 35);

                    // Replace the old ImageView with the new container
                    if (imgUser.getParent() instanceof StackPane) {
                        StackPane parent = (StackPane) imgUser.getParent();
                        parent.getChildren().setAll(imgUser, border);
                    }

                    System.out.println("User profile image updated in header");
                }
            } catch (Exception ex) {
                System.out.println("Error updating user profile image: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    public void handleAvatarUpdate(String username) {
        Platform.runLater(() -> {
            try {
                // Get fresh user data
                User updatedUser = clinetView.getUser(username);
                if (updatedUser == null) {
                    return;
                }

                // Refresh in open tabs if available
                if (tabsControllers.containsKey(username)) {
                    ChatBoxController controller = tabsControllers.get(username);
                    controller.refreshUserAvatar(username);
                }

                // Refresh tab icons if this user has an open tab
                refreshTabAvatar(username);

                // Force contact list to reload with new avatars
                refreshContacts();

                // Refresh user profile image if it's the current user
                if (username.equals(clinetView.getUserInformation().getUsername())) {
                    updateUserProfileImage(updatedUser);
                }
            } catch (Exception ex) {
                System.out.println("Error handling avatar update: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    /**
     * Get the map of chat box controllers for all open tabs
     * 
     * @return Map of tab IDs to their corresponding ChatBoxController instances
     */
    public Map<String, ChatBoxController> getTabsControllers() {
        return tabsControllers;
    }

    public void updateContactStatus(String username, String status) {
        Platform.runLater(() -> {
            // Update in contacts list
            for (HBox contactBox : listViewContacts.getItems()) {
                Label usernameLabel = (Label) contactBox.lookup(".friend-name");
                if (usernameLabel != null && usernameLabel.getText().equals(username)) {
                    Label statusLabel = (Label) contactBox.lookup(".friend-status");
                    Circle statusIndicator = (Circle) contactBox.lookup(".status-indicator");

                    if (statusLabel != null) {
                        statusLabel.setText(status);
                    }

                    if (statusIndicator != null) {
                        statusIndicator.getStyleClass().removeAll("status-indicator-online", "status-indicator-offline",
                                "status-indicator-busy");
                        statusIndicator.getStyleClass().add("status-indicator-" + status.toLowerCase());
                    }
                    break;
                }
            }

            // Update in family list
            for (HBox contactBox : listViewFamily.getItems()) {
                Label usernameLabel = (Label) contactBox.lookup(".friend-name");
                if (usernameLabel != null && usernameLabel.getText().equals(username)) {
                    Label statusLabel = (Label) contactBox.lookup(".friend-status");
                    Circle statusIndicator = (Circle) contactBox.lookup(".status-indicator");

                    if (statusLabel != null) {
                        statusLabel.setText(status);
                    }

                    if (statusIndicator != null) {
                        statusIndicator.getStyleClass().removeAll("status-indicator-online", "status-indicator-offline",
                                "status-indicator-busy");
                        statusIndicator.getStyleClass().add("status-indicator-" + status.toLowerCase());
                    }
                    break;
                }
            }

            // Also update any open chat boxes
            if (clinetView != null) {
                clinetView.updateUserStatus(username, status);
            }
        });
    }

    private void updateStatusStyle(String status) {
        if (comboBoxStatus != null) {
            // Remove all existing status styles
            comboBoxStatus.getStyleClass().removeAll("status-online", "status-offline", "status-busy");

            // Add appropriate style class
            switch (status.toLowerCase()) {
                case "online":
                    comboBoxStatus.getStyleClass().add("status-online");
                    break;
                case "offline":
                    comboBoxStatus.getStyleClass().add("status-offline");
                    break;
                case "busy":
                    comboBoxStatus.getStyleClass().add("status-busy");
                    break;
            }
        }
    }

    public void updateStatusDropdown(String status) {
        Platform.runLater(() -> {
            if (comboBoxStatus != null && status != null) {
                // Chỉ cập nhật nếu trạng thái khác với trạng thái hiện tại
                // và dropdown không đang hiển thị và không đang trong quá trình cập nhật
                if (!status.equals(comboBoxStatus.getValue()) &&
                        !comboBoxStatus.isShowing() &&
                        !isStatusBeingUpdated) {

                    // Chỉ cập nhật nếu người dùng không đang chọn trạng thái khác
                    if (currentSelectedStatus == null || status.equals(currentSelectedStatus)) {
                        isStatusBeingUpdated = true;
                        comboBoxStatus.setValue(status);
                        updateStatusStyle(status);
                        currentSelectedStatus = status;
                        isStatusBeingUpdated = false;
                    }
                }
            }
        });
    }
}
