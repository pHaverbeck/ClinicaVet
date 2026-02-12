package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.CenteredBody;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.service.CompanyChoice;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;

import java.sql.SQLException;
import java.util.List;

@PageTitle("Selecionar Empresa")
@Route(value = "company/select", layout = MainLayout.class)
@Menu(title = "Selecionar Empresa", icon = "la la-building", order = 2)
public class CompanySelectView extends VerticalLayout implements BeforeEnterObserver {

    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final Grid<CompanyChoice> grid = new Grid<>(CompanyChoice.class, false);
    private final Button confirmBtn = new Button("Usar empresa");
    private final Button refreshBtn = new Button("Atualizar lista");
    private final Button createBtn = new Button("Criar empresa");

    public CompanySelectView(CurrentUserService currentUserService,
            CurrentCompanyService currentCompanyService) {
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSpacing(true);

        add(new ViewToolbar("Selecionar Empresa"));

        var body = new CenteredBody();
        add(body);
        setFlexGrow(1, body);

        var content = body.wrapper();

        H1 title = new H1("Escolha sua empresa");

        grid.addColumn(c -> c.name).setHeader("Nome").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(c -> c.admin ? "Sim" : "Não").setHeader("Admin").setAutoWidth(true);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setHeight("420px");

        confirmBtn.addThemeNames("primary");
        confirmBtn.setEnabled(false);
        confirmBtn.addClickListener(e -> onConfirm());

        grid.asSingleSelect().addValueChangeListener(e -> confirmBtn.setEnabled(e.getValue() != null));

        refreshBtn.addClickListener(e -> {
            try {
                loadData();
            } catch (Exception ex) {
                showError(ex);
            }
        });

        createBtn.addThemeNames("success", "tertiary");
        createBtn.addClickListener(e -> UI.getCurrent().navigate("company/new"));

        HorizontalLayout actions = new HorizontalLayout(confirmBtn, refreshBtn, createBtn);
        actions.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        content.add(
                title,
                new Paragraph("Selecione uma empresa e clique em “Usar empresa”."),
                grid,
                actions);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!currentUserService.isLoggedIn()) {
            Notification.show("Faça login para continuar.", 2500, Notification.Position.MIDDLE);
            event.rerouteTo("home");
        }
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        if (!currentUserService.isLoggedIn()) {
            return;
        }

        try {
            long uid = currentUserService.requireUserId();
            if (currentCompanyService.ensureAutoSelectionIfSingle(uid)) {
                Notification.show("Empresa selecionada automaticamente.", 1500, Notification.Position.MIDDLE);
                UI.getCurrent().navigate("home");
                return;
            }

            loadData();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void loadData() throws SQLException {
        grid.setItems(List.of());

        long uid = currentUserService.requireUserId();
        List<CompanyChoice> items = currentCompanyService.listSelectableChoicesForUser(uid);
        grid.setItems(items);

        if (items.isEmpty()) {
            Notification.show(
                    "Você ainda não possui empresas. Crie uma para continuar.",
                    3000,
                    Notification.Position.BOTTOM_CENTER);
            confirmBtn.setEnabled(false);
            createBtn.focus();
            return;
        }

        if (items.size() == 1) {
            grid.select(items.get(0));
        } else {
            grid.focus();
        }

        confirmBtn.setEnabled(grid.asSingleSelect().getValue() != null);
    }

    private void onConfirm() {
        CompanyChoice selected = grid.asSingleSelect().getValue();
        if (selected == null) {
            Notification.show("Selecione uma empresa.", 2500, Notification.Position.MIDDLE);
            return;
        }

        try {
            long uid = currentUserService.requireUserId();
            currentCompanyService.selectCompanyForUser(uid, selected.id);

            Notification.show("Empresa selecionada: " + selected.name, 2500, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("users");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        ex.printStackTrace();
        Notification.show("Falha: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                .addThemeNames("error");
    }
}