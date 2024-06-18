package lol.koblizek.bytelens.core.controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import lol.koblizek.bytelens.api.ui.PathField;
import lol.koblizek.bytelens.api.util.ProjectCreator;
import lol.koblizek.bytelens.core.ByteLens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NewProjectViewController extends Controller {

    private final Logger logger;

    @FXML public ListView<ProjectCreator> projectTypeListing;
    @FXML public AnchorPane userdata;

    public NewProjectViewController(ByteLens byteLens) {
        super(byteLens);
        logger = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void initialize() {
        getByteLens().getProjectTypes().forEach(projectTypeListing.getItems()::add);
        projectTypeListing.getSelectionModel().select(0);
        projectTypeListing.getSelectionModel().selectedItemProperty().subscribe(menu -> {
            if (menu == null) return;
            if (userdata.getChildren().size() > 1) {
                userdata.getChildren().remove(1, userdata.getChildren().size());
                userdata.getChildren().addAll(generateNode(menu));
            } else {
                userdata.getChildren().addAll(generateNode(menu));
            }
        });
    }

    private List<Node> generateNode(ProjectCreator creator) {
        List<Node> nodes = new ArrayList<>();
        AtomicInteger i = new AtomicInteger(10);
        creator.getFields().forEach((name, type) -> {
            Node node = generateNode(nodes, name, type);
            Label label = new Label(name);
            AnchorPane.setTopAnchor(label, (double) i.get());
            AnchorPane.setTopAnchor(node, (double) i.get());
            AnchorPane.setLeftAnchor(label, 20.0);
            label.setLabelFor(node);
            nodes.add(label);
            nodes.add(node);
            i.addAndGet(30);
        });
        return nodes;
    }

    private Node generateNode(List<Node> nodes, String name, Class<?> type) {
        Region r;
        if (type == String.class || type == int.class) {
            r = new TextField();
            r.getStyleClass().add("new-project-text-field");
        } else if (type == Path.class) {
            r = new PathField();
            ((PathField) r).directoryOnlyProperty().set(true);
        } else if (type == Boolean.class) {
            r = new CheckBox();
        } else {
            r = new Label("Unsupported type: " + type);
            r.setStyle("-fx-border-color: red");
        }
        AnchorPane.setRightAnchor(r, 20.0);
        r.prefWidth(200);
        return r;
    }

    @FXML
    public void closeWindow() {
        ((Stage) userdata.getScene().getWindow()).close();
    }

    @FXML
    public void attemptCreateProject(MouseEvent mouseEvent) {
        var creator = projectTypeListing.getSelectionModel().getSelectedItem();
        var list = userdata.getChildren();
        var data = IntStream.range(0, list.size() / 2)
                .mapToObj(i -> {
                    String text = ((Label) list.get(2 * i)).getText();
                    return Map.entry(text, list.get(2 * i + 1));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!validate(data)) {
            logger.warn("Data required to create project are not valid.");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Invalid data");
            alert.setContentText("Please fill in all required fields.");
        } else {
            getByteLens().getProjects()
                            .addLast(creator.createProject(getProjectData(data)));
            closeWindow();

        }
    }

    private boolean validate(Map<String, Node> data) {
        return data.values().stream().allMatch(node -> {
            if (node instanceof TextField) {
                System.out.println(((TextField) node).getText());
                return !((TextField) node).getText().isBlank();
            } else if (node instanceof PathField) {
                System.out.println(((PathField) node).getText());
                return !((PathField) node).getText().isBlank();
            } else return true;
        });
    }

    private Map<String, Object> getProjectData(Map<String, Node> data) {
        return data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            assert entry.getValue() != null;
            return switch (entry.getValue()) {
                case TextField textField -> textField.getText();
                case PathField pathField -> Paths.get(pathField.getText());
                case CheckBox checkBox -> checkBox.isSelected();
                default -> entry.getKey();
            };
        }));
    }
}
