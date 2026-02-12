package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.Attendance;
import com.example.application.classes.model.Pet;
import com.example.application.classes.service.*;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.*;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@PageTitle("Atendimento")
@Route(value = "attendance/new", layout = MainLayout.class)
@RouteAlias(value = "attendance/:id/edit", layout = MainLayout.class)
public class AttendanceView extends Main implements BeforeEnterObserver {

    private final PetService petService;
    private final AttendanceService attendanceService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final DateTimePicker appointmentAtPicker = new DateTimePicker("Atendimento em");
    private final TextArea descriptionArea = new TextArea("Descrição");
    private final ComboBox<Pet> petComboBox = new ComboBox<>("Pet");
    private final Button saveBtn = new Button("Salvar");
    private final Button returnBtn = new Button("Voltar");

    private Long attendanceId;
    private Long preselectedPetId;
    private boolean editMode;

    private List<Pet> cachedPets = Collections.emptyList();

    public AttendanceView(PetService petService,
            AttendanceService attendanceService,
            CurrentUserService currentUserService,
            CurrentCompanyService currentCompanyService) {
        this.petService = petService;
        this.attendanceService = attendanceService;
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;

        appointmentAtPicker.setLocale(Locale.of("pt", "BR"));

        add(new ViewToolbar("Atendimento"));

        var content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.setWidthFull();
        add(content);

        var form = new FormLayout();
        form.setMaxWidth("600px");

        descriptionArea.setWidthFull();
        descriptionArea.setMinHeight("120px");
        descriptionArea.setMaxLength(2000);

        petComboBox.setRequiredIndicatorVisible(true);
        petComboBox.setHelperText("Obrigatório");
        petComboBox.setItemLabelGenerator(p -> p.getName() + " (ID: " + p.getId() + ")");
        petComboBox.setWidthFull();

        form.add(petComboBox, appointmentAtPicker, descriptionArea);
        content.add(form);

        saveBtn.addThemeNames("primary");
        saveBtn.addClickListener(e -> onSave());
        returnBtn.addThemeNames("tertiary");
        returnBtn.addClickListener(e -> {
            if (editMode && attendanceId != null) {
                Optional<Attendance> opt;
                try {
                    opt = attendanceService.findById(attendanceId);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                if (opt.isPresent()) {
                    long petId = opt.get().getAnimalId();
                    UI.getCurrent().navigate("pets/:id/attendances".replace(":id", String.valueOf(petId)));
                    return;
                }
            } else {
                if (preselectedPetId != null) {
                    UI.getCurrent().navigate("pets/:id/attendances".replace(":id", String.valueOf(preselectedPetId)));
                    return;
                }
            }
        });

        var buttons = new HorizontalLayout();
        buttons.add(saveBtn, returnBtn);
        content.add(buttons);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!currentUserService.isLoggedIn()) {
            Notification.show("Faça login para continuar.", 3000, Notification.Position.MIDDLE);
            event.rerouteTo("home");
            return;
        }

        if (!currentCompanyService.hasSelection()) {
            Notification.show("Selecione uma empresa para continuar.", 3000, Notification.Position.MIDDLE);
            event.rerouteTo("company/select");
            return;
        }

        Optional<String> idParam = event.getRouteParameters().get("id");
        if (idParam.isPresent()) {
            try {
                attendanceId = Long.parseLong(idParam.get());
                editMode = true;
            } catch (NumberFormatException ex) {
                Notification.show("ID de atendimento inválido.", 4000, Notification.Position.MIDDLE);
                event.rerouteTo("pets");
                return;
            }
        } else {
            editMode = false;
            attendanceId = null;
        }

        preselectedPetId = null;
        if (!editMode) {
            var params = event.getLocation().getQueryParameters().getParameters();
            var petList = params.get("pet");
            if (petList != null && !petList.isEmpty()) {
                try {
                    preselectedPetId = Long.parseLong(petList.get(0));
                } catch (NumberFormatException ignore) {
                    preselectedPetId = null;
                }
            }
        }
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        try {
            loadPetsOrDisable();

            if (editMode && attendanceId != null) {
                loadExistingAttendanceOrReroute(attendanceId);
            } else if (preselectedPetId != null) {
                selectPetIfExists(preselectedPetId);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erro ao preparar tela: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            UI.getCurrent().navigate("pets");
        }
    }

    private void loadPetsOrDisable() throws SQLException {
        cachedPets = petService.listAllForCompany();
        petComboBox.setItems(cachedPets);

        boolean hasPets = !cachedPets.isEmpty();
        saveBtn.setEnabled(hasPets);

        if (!hasPets) {
            Notification.show(
                    "Nenhum pet encontrado. Cadastre um pet antes de criar um atendimento.",
                    5000,
                    Notification.Position.MIDDLE).addThemeNames("warning");
        }
    }

    private void selectPetIfExists(long petId) {
        cachedPets.stream()
                .filter(p -> p.getId() == petId)
                .findFirst()
                .ifPresent(petComboBox::setValue);
    }

    private void loadExistingAttendanceOrReroute(long id) throws SQLException {
        var opt = attendanceService.findById(id);
        if (opt.isEmpty()) {
            Notification.show("Atendimento não encontrado.", 4000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            UI.getCurrent().navigate("pets");
            return;
        }

        Attendance attendance = opt.get();

        appointmentAtPicker.setValue(attendance.getAppointmentAt());
        descriptionArea.setValue(attendance.getDescription() != null ? attendance.getDescription() : "");

        selectPetIfExists(attendance.getAnimalId());
    }

    private void onSave() {
        try {
            currentCompanyService.activeCompanyIdOrThrow();

            Pet selectedPet = petComboBox.getValue();
            if (selectedPet == null) {
                Notification.show("Selecione um pet para o atendimento.", 3000, Notification.Position.MIDDLE)
                        .addThemeNames("warning");
                return;
            }

            var appointmentAt = appointmentAtPicker.getValue();
            var description = descriptionArea.getValue() != null ? descriptionArea.getValue().trim() : "";

            if (editMode && attendanceId != null) {
                var attendance = attendanceService.findById(attendanceId)
                        .orElseThrow(() -> new IllegalStateException("Atendimento não encontrado para edição."));

                attendance.setAnimalId(selectedPet.getId());
                attendance.setAppointmentAt(appointmentAt);
                attendance.setDescription(description);

                attendanceService.updateBasics(attendance);

                Notification.show("Atendimento atualizado com sucesso.", 3000, Notification.Position.MIDDLE)
                        .addThemeNames("success");
            } else {
                var attendance = new Attendance();
                attendance.setAnimalId(selectedPet.getId());
                attendance.setAppointmentAt(appointmentAt);
                attendance.setDescription(description);

                long id = attendanceService.create(attendance);

                Notification.show("Atendimento criado com sucesso. ID: " + id, 3000, Notification.Position.MIDDLE)
                        .addThemeNames("success");
            }

            UI.getCurrent().navigate("pets/" + selectedPet.getId() + "/attendances");

        } catch (AttendanceValidationException vex) {
            Notification.show(vex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao salvar atendimento: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification
                    .show("Erro inesperado ao salvar atendimento: " + ex.getMessage(), 5000,
                            Notification.Position.MIDDLE)
                    .addThemeNames("error");
        }
    }
}