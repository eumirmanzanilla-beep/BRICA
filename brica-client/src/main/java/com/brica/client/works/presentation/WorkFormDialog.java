package com.brica.client.works.presentation;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.brica.client.api.ClientDto;
import com.brica.client.works.api.WorkDto;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;
import javafx.util.StringConverter;

final class WorkFormDialog {

    private static final String CSS_PATH =
            "/com/brica/client/presentation/styles/app.css";

    private WorkFormDialog() {
    }

    static Optional<WorkInput> show(
            Window owner,
            WorkDto existing,
            List<ClientDto> availableClients,
            Function<Long, String> clientDisplay) {

        boolean editing = existing != null;

        Dialog<WorkInput> dialog = new Dialog<>();

        dialog.initOwner(owner);

        dialog.setTitle(
                editing
                        ? "Corregir obra"
                        : "Nueva obra"
        );

        dialog.getDialogPane().setHeaderText(
                editing
                        ? "Corregir información general"
                        : "Registrar nueva obra"
        );

        ComboBox<ClientDto> clientBox =
                new ComboBox<>(
                        FXCollections.observableArrayList(
                                availableClients
                        )
                );

        clientBox.setPromptText(
                "Selecciona un cliente"
        );

        clientBox.setMaxWidth(
                Double.MAX_VALUE
        );

        clientBox.setConverter(
                new StringConverter<>() {

                    @Override
                    public String toString(
                            ClientDto client) {

                        return client == null
                                ? ""
                                : client.name()
                                        + " (#"
                                        + client.id()
                                        + ")";
                    }

                    @Override
                    public ClientDto fromString(
                            String value) {

                        return null;
                    }
                }
        );

        TextField nameField =
                new TextField(
                        editing
                                ? existing.name()
                                : ""
                );

        TextField locationField =
                new TextField(
                        editing
                                ? existing.location()
                                : ""
                );

        TextArea descriptionField =
                new TextArea(
                        editing
                                ? safeText(
                                        existing.description()
                                )
                                : ""
                );

        descriptionField.setPrefRowCount(3);
        descriptionField.setWrapText(true);

        DatePicker startDatePicker =
                new DatePicker();

        DatePicker endDatePicker =
                new DatePicker();

        nameField.getStyleClass()
                .add("form-field");

        locationField.getStyleClass()
                .add("form-field");

        descriptionField.getStyleClass()
                .add("form-field");

        clientBox.getStyleClass()
                .add("form-field");

        startDatePicker.getStyleClass()
                .add("form-field");

        endDatePicker.getStyleClass()
                .add("form-field");

        GridPane form = new GridPane();

        form.setHgap(12);
        form.setVgap(12);
        form.setPadding(
                new Insets(12)
        );

        int row = 0;

        if (editing) {

            Label notice =
                    new Label(
                            "Cliente: "
                                    + clientDisplay.apply(
                                            existing.clientId()
                                    )
                                    + "\nInicio: "
                                    + dateText(
                                            existing.startDate()
                                    )
                                    + " | Terminación: "
                                    + dateText(
                                            existing.endDate()
                                    )
                                    + "\nEstos datos no pueden modificarse "
                                    + "desde esta operación."
                    );

            notice.setWrapText(true);

            notice.getStyleClass()
                    .add("content-note");

            form.add(
                    notice,
                    0,
                    row++,
                    2,
                    1
            );

        } else {

            form.add(
                    formLabel("Cliente"),
                    0,
                    row
            );

            form.add(
                    clientBox,
                    1,
                    row++
            );
        }

        form.add(
                formLabel("Nombre de la obra"),
                0,
                row
        );

        form.add(
                nameField,
                1,
                row++
        );

        form.add(
                formLabel("Ubicación"),
                0,
                row
        );

        form.add(
                locationField,
                1,
                row++
        );

        form.add(
                formLabel("Descripción"),
                0,
                row
        );

        form.add(
                descriptionField,
                1,
                row++
        );

        if (!editing) {

            form.add(
                    formLabel("Fecha de inicio"),
                    0,
                    row
            );

            form.add(
                    startDatePicker,
                    1,
                    row++
            );

            form.add(
                    formLabel("Fecha de terminación"),
                    0,
                    row
            );

            form.add(
                    endDatePicker,
                    1,
                    row++
            );
        }

        Label helpLabel =
                new Label(
                        "Nombre y ubicación: obligatorios, máximo "
                                + "255 caracteres.\nDescripción: máximo "
                                + "255 caracteres en este formulario."
                                + (editing
                                        ? ""
                                        : "\nLa fecha de inicio es obligatoria. "
                                                + "La terminación es opcional.")
                );

        helpLabel.setWrapText(true);

        helpLabel.getStyleClass()
                .add("content-note");

        form.add(
                helpLabel,
                0,
                row,
                2,
                1
        );

        GridPane.setHgrow(
                nameField,
                Priority.ALWAYS
        );

        GridPane.setHgrow(
                locationField,
                Priority.ALWAYS
        );

        GridPane.setHgrow(
                descriptionField,
                Priority.ALWAYS
        );

        dialog.getDialogPane()
                .setContent(form);

        ButtonType saveType =
                new ButtonType(
                        editing
                                ? "Guardar corrección"
                                : "Registrar",
                        ButtonBar.ButtonData.OK_DONE
                );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        saveType,
                        ButtonType.CANCEL
                );

        applyStyles(dialog);

        Node saveButton =
                dialog.getDialogPane()
                        .lookupButton(saveType);

        saveButton.getStyleClass()
                .add("primary-button");

        saveButton.disableProperty()
                .bind(
                        Bindings.createBooleanBinding(
                                () -> isSaveDisabled(
                                        editing,
                                        nameField,
                                        locationField,
                                        descriptionField,
                                        clientBox,
                                        startDatePicker,
                                        endDatePicker
                                ),
                                nameField.textProperty(),
                                locationField.textProperty(),
                                descriptionField.textProperty(),
                                clientBox.valueProperty(),
                                startDatePicker.valueProperty(),
                                endDatePicker.valueProperty()
                        )
                );

        dialog.setResultConverter(
                button -> {

                    if (button != saveType) {
                        return null;
                    }

                    return new WorkInput(
                            editing
                                    ? null
                                    : clientBox
                                            .getValue()
                                            .id(),

                            nameField
                                    .getText()
                                    .trim(),

                            locationField
                                    .getText()
                                    .trim(),

                            descriptionField
                                    .getText()
                                    .trim(),

                            editing
                                    ? null
                                    : startDatePicker
                                            .getValue(),

                            editing
                                    ? null
                                    : endDatePicker
                                            .getValue()
                    );
                }
        );

        return dialog.showAndWait();
    }

    private static boolean isSaveDisabled(
            boolean editing,
            TextField nameField,
            TextField locationField,
            TextArea descriptionField,
            ComboBox<ClientDto> clientBox,
            DatePicker startDatePicker,
            DatePicker endDatePicker) {

        if (!validText(nameField.getText())
                || !validText(
                        locationField.getText()
                )) {

            return true;
        }

        if (descriptionField
                .getText()
                .length() > 255) {

            return true;
        }

        if (editing) {
            return false;
        }

        LocalDate start =
                startDatePicker.getValue();

        LocalDate end =
                endDatePicker.getValue();

        return clientBox.getValue() == null
                || start == null
                || (end != null
                        && end.isBefore(start));
    }

    private static boolean validText(
            String text) {

        return text != null
                && !text.isBlank()
                && text.trim().length() <= 255;
    }

    private static String safeText(
            String text) {

        return text == null
                ? ""
                : text;
    }

    private static String dateText(
            String date) {

        return date == null
                || date.isBlank()
                ? "—"
                : date;
    }

    private static Label formLabel(
            String text) {

        Label label =
                new Label(text);

        label.getStyleClass()
                .add("form-label");

        return label;
    }

    private static void applyStyles(
            Dialog<?> dialog) {

        URL stylesheet =
                WorkFormDialog.class
                        .getResource(
                                CSS_PATH
                        );

        if (stylesheet != null) {

            dialog.getDialogPane()
                    .getStylesheets()
                    .add(
                            stylesheet
                                    .toExternalForm()
                    );
        }

        dialog.getDialogPane()
                .getStyleClass()
                .add("client-dialog");
    }

    record WorkInput(
            Long clientId,
            String name,
            String location,
            String description,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}