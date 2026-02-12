package com.example.application.base.ui.component;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

public class DocumentField extends CustomField<String> {

    private final ComboBox<String> docTypeField = new ComboBox<>("Tipo de documento");
    private final TextField documentField = new TextField("Documento");

    public DocumentField() {
        docTypeField.setItems("CPF", "CNPJ", "Passaporte");
        docTypeField.setPlaceholder("Selecione");
        docTypeField.setClearButtonVisible(true);

        documentField.setMaxLength(20);
        documentField.setValueChangeMode(ValueChangeMode.EAGER);

        HorizontalLayout layout = new HorizontalLayout(docTypeField, documentField);
        layout.setWidthFull();
        docTypeField.setWidth("160px");
        documentField.setWidthFull();
        add(layout);

        docTypeField.addValueChangeListener(e -> {
            documentField.clear();
            String type = e.getValue();
            if ("CPF".equals(type)) {
                documentField.setPlaceholder("000.000.000-00");
            } else if ("CNPJ".equals(type)) {
                documentField.setPlaceholder("00.000.000/0000-00");
            } else if ("Passaporte".equals(type)) {
                documentField.setPlaceholder("ABC12345");
            } else {
                documentField.setPlaceholder("");
            }
        });

        documentField.addValueChangeListener(e -> {
            String type = docTypeField.getValue();
            if (type == null || type.isBlank()) {
                return;
            }
            String raw = e.getValue();
            String digits = raw == null ? "" : raw.replaceAll("\\D", "");
            String formatted = formatDocument(type, digits);
            if (formatted != null && !formatted.equals(raw)) {
                documentField.setValue(formatted);
            }
        });
    }

    private String formatDocument(String type, String digits) {
        if ("CPF".equals(type)) {
            if (digits.length() > 11)
                digits = digits.substring(0, 11);
            if (digits.length() <= 3)
                return digits;
            if (digits.length() <= 6)
                return digits.replaceFirst("(\\d{3})(\\d+)", "$1.$2");
            if (digits.length() <= 9)
                return digits.replaceFirst("(\\d{3})(\\d{3})(\\d+)", "$1.$2.$3");
            return digits.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{0,2})", "$1.$2.$3-$4").replaceAll("-$", "");
        }
        if ("CNPJ".equals(type)) {
            if (digits.length() > 14)
                digits = digits.substring(0, 14);
            if (digits.length() <= 2)
                return digits;
            if (digits.length() <= 5)
                return digits.replaceFirst("(\\d{2})(\\d+)", "$1.$2");
            if (digits.length() <= 8)
                return digits.replaceFirst("(\\d{2})(\\d{3})(\\d+)", "$1.$2.$3");
            if (digits.length() <= 12)
                return digits.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d+)", "$1.$2.$3/$4");
            return digits.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{0,2})",
                    "$1.$2.$3/$4-$5").replaceAll("-$", "");
        }
        if (digits.length() > 20) {
            return digits.substring(0, 20);
        }
        return digits;
    }

    @Override
    protected String generateModelValue() {
        return documentField.getValue();
    }

    @Override
    protected void setPresentationValue(String newPresentationValue) {
        documentField.setValue(newPresentationValue != null ? newPresentationValue : "");
    }

    public String getDocType() {
        return docTypeField.getValue();
    }

    public void setDocType(String type) {
        this.docTypeField.setValue(type);
    }

    public ComboBox<String> getDocTypeComponent() {
        return docTypeField;
    }

    public TextField getDocumentComponent() {
        return documentField;
    }
}