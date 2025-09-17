package com.vanderlande.bsminserter;

import javax.swing.text.*;

public class LengthRestrictedDocumentFilter extends DocumentFilter {
    private int maxLength;
    private boolean digitsOnly;
    private boolean lettersOnly;
    private boolean alphanumeric;
    private boolean fixedZero; // special rule for license plate

    // Constructor for normal fields
    public LengthRestrictedDocumentFilter(int maxLength) {
        this(maxLength, false, false, false, false);
    }

    // Full constructor with rules
    public LengthRestrictedDocumentFilter(int maxLength, boolean digitsOnly, boolean lettersOnly, boolean alphanumeric, boolean fixedZero) {
        this.maxLength = maxLength;
        this.digitsOnly = digitsOnly;
        this.lettersOnly = lettersOnly;
        this.alphanumeric = alphanumeric;
        this.fixedZero = fixedZero;
    }

    private String filterText(String text) {
        if (text == null) return "";
        if (digitsOnly) return text.replaceAll("\\D", "");          // keep only digits
        if (lettersOnly) return text.replaceAll("[^a-zA-Z]", "");   // keep only letters
        if (alphanumeric) return text.replaceAll("[^a-zA-Z0-9]", ""); // keep only letters+digits
        return text;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        if (string == null) return;

        // License plate special case: prevent inserting before '0'
        if (fixedZero && offset == 0) return;

        String filtered = filterText(string);
        if ((fb.getDocument().getLength() + filtered.length()) <= maxLength) {
            super.insertString(fb, offset, filtered, attr);
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (text == null) return;

        // License plate special case: prevent replacing '0'
        if (fixedZero && offset == 0) {
            offset++;
            length = Math.max(0, length - 1);
        }

        String filtered = filterText(text);
        if ((fb.getDocument().getLength() - length + filtered.length()) <= maxLength) {
            super.replace(fb, offset, length, filtered, attrs);
        }
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        // License plate special case: prevent deleting '0'
        if (fixedZero && offset == 0) {
            offset++;
            length = Math.max(0, length - 1);
        }
        super.remove(fb, offset, length);
    }
}
