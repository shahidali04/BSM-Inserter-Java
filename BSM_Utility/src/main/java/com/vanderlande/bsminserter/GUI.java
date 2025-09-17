package com.vanderlande.bsminserter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.File;

class GUI {

    private static final Logger logger = LogManager.getLogger(GUI.class);

    JFrame frame;
    JTextField totalBSMField, licensePlateField, carrierDesignatorField;
    JTextField flightNumberField, offloadAirportField;
    JSpinner scheduledDateSpinner;
    JButton insertButton;
    JLabel dbStatusLabel;
    ImageIcon dbIconGray, dbIconGreen;
    JTextArea logArea; // GUI log output

    public GUI() {

        // Clear log file on startup
        try {
            PrintWriter writer = new PrintWriter("C:/Users/shahi/OneDrive/Desktop/logs/bsminserter.log");
            writer.print(""); // Clear contents
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to clear log file: " + e.getMessage());
        }



        frame = new JFrame();
        frame.setSize(600, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        // Load db icons
        try {
            dbIconGreen = new ImageIcon(ImageIO.read(getClass().getResourceAsStream("/images/dbicon2.png")));
            dbIconGray = new ImageIcon(ImageIO.read(getClass().getResourceAsStream("/images/dbicon1.png")));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Database icon images not found in resources/images/");
            dbIconGreen = new ImageIcon();
            dbIconGray = new ImageIcon();
        }

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 10));
        JLabel titleLabel = new JLabel("BSM Inserter", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 30));
        dbStatusLabel = new JLabel(dbIconGray);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(dbStatusLabel, BorderLayout.EAST);
        frame.add(headerPanel, BorderLayout.NORTH);

        // Form
        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(2, 20, 10, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        Font labelFont = new Font("Arial", Font.BOLD, 16);
        Font fieldFont = new Font("Arial", Font.PLAIN, 16);
        Dimension fieldSize = new Dimension(300, 40);

        //input fields
        totalBSMField = new JTextField();
        setupField(formPanel, gbc, "Total BSM :", totalBSMField, labelFont, fieldFont, fieldSize);

        licensePlateField = new JTextField("0");
        setupField(formPanel, gbc, "LPN (4 digits) :", licensePlateField, labelFont, fieldFont, fieldSize);
        ((AbstractDocument) licensePlateField.getDocument()).setDocumentFilter
                (new LengthRestrictedDocumentFilter(5, true, false, false, true));

        carrierDesignatorField = new JTextField();
        setupField(formPanel, gbc, "Flight Carrier :", carrierDesignatorField, labelFont, fieldFont, fieldSize);
        ((AbstractDocument) carrierDesignatorField.getDocument()).setDocumentFilter
                (new LengthRestrictedDocumentFilter(2, false, false, true, false));

        flightNumberField = new JTextField();
        setupField(formPanel, gbc, "Flight No. :", flightNumberField, labelFont, fieldFont, fieldSize);
        ((AbstractDocument) flightNumberField.getDocument()).setDocumentFilter
                (new LengthRestrictedDocumentFilter(4, true, false, false, false));

        offloadAirportField = new JTextField();
        setupField(formPanel, gbc, "Flight Destination :", offloadAirportField, labelFont, fieldFont, fieldSize);
        ((AbstractDocument) offloadAirportField.getDocument()).setDocumentFilter
                (new LengthRestrictedDocumentFilter(3, false, true, false, false));

        SpinnerDateModel dateModel1 = new SpinnerDateModel();
        scheduledDateSpinner = new JSpinner(dateModel1);
        scheduledDateSpinner.setEditor(new JSpinner.DateEditor(scheduledDateSpinner, "yyyy-MM-dd"));
        setupField(formPanel, gbc, "Schedule Date :", scheduledDateSpinner, labelFont, fieldFont, fieldSize);

        // Buttons
        JPanel buttonsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcButtons = new GridBagConstraints();
        gbcButtons.gridy = 0;
        gbcButtons.fill = GridBagConstraints.HORIZONTAL;
        gbcButtons.anchor = GridBagConstraints.CENTER;

        //Insert button
        insertButton = new JButton("Insert BSM");
        insertButton.setFont(new Font("Arial", Font.BOLD, 16));
        insertButton.setEnabled(false);
        gbcButtons.gridx = 0;
        gbcButtons.weightx = 0.7;
        buttonsPanel.add(insertButton, gbcButtons);

        JPanel gapPanel = new JPanel();
        gbcButtons.gridx = 1;
        gbcButtons.weightx = 0.05;
        buttonsPanel.add(gapPanel, gbcButtons);

        //Clear button
        JButton clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Arial", Font.BOLD, 16));
        gbcButtons.gridx = 2;
        gbcButtons.weightx = 0.25;
        buttonsPanel.add(clearButton, gbcButtons);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        formPanel.add(buttonsPanel, gbc);

        Dimension buttonSize = new Dimension(300, 40);
        insertButton.setPreferredSize(buttonSize);
        clearButton.setPreferredSize(buttonSize);

        formWrapper.add(formPanel, BorderLayout.NORTH);

        // Logs
        logArea = new JTextArea(8, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("Arial", Font.BOLD, 15));
        JScrollPane logScroll = new JScrollPane(logArea);
        formWrapper.add(logScroll, BorderLayout.CENTER);

        frame.add(formWrapper, BorderLayout.CENTER);

        // Enable Insert button only if all fields filled
        DocumentListener fieldListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                checkFields();
            }

            public void removeUpdate(DocumentEvent e) {
                checkFields();
            }

            public void insertUpdate(DocumentEvent e) {
                checkFields();
            }

            private void checkFields() {
                boolean allFilled = !totalBSMField.getText().trim().isEmpty()
                        && !licensePlateField.getText().trim().isEmpty()
                        && !carrierDesignatorField.getText().trim().isEmpty()
                        && !flightNumberField.getText().trim().isEmpty()
                        && !offloadAirportField.getText().trim().isEmpty();
                insertButton.setEnabled(allFilled);
            }
        };

        totalBSMField.getDocument().addDocumentListener(fieldListener);
        licensePlateField.getDocument().addDocumentListener(fieldListener);
        carrierDesignatorField.getDocument().addDocumentListener(fieldListener);
        flightNumberField.getDocument().addDocumentListener(fieldListener);
        offloadAirportField.getDocument().addDocumentListener(fieldListener);

        // Clear button
        clearButton.addActionListener(e -> {
            totalBSMField.setText("");
            licensePlateField.setText("");
            carrierDesignatorField.setText("");
            flightNumberField.setText("");
            offloadAirportField.setText("");
            scheduledDateSpinner.setValue(new Date());
        });

        // Insert button
        insertButton.addActionListener(e -> {
            try {

                // Clear log for new insertion
                logArea.setText("");

                boolean isConnected = BSMInserter.testConnection();
                updateDbStatusIcon(isConnected);

                if (!isConnected) {
                    JOptionPane.showMessageDialog(frame, "Database not connected! Cannot insert data.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int totalBSM = Integer.parseInt(totalBSMField.getText());
                String licensePlate = licensePlateField.getText().trim();
                String carrier = carrierDesignatorField.getText().toUpperCase().trim();
                String flight = flightNumberField.getText().trim();
                String airport = offloadAirportField.getText().toUpperCase().trim();

                Date schDate = (Date) scheduledDateSpinner.getValue();
                String schDateStr = new SimpleDateFormat("yyyy-MM-dd").format(schDate);
                String lastModifiedStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

                // Run insertion in a separate thread to avoid UI freeze
                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        BSMInserter.insertBSM(totalBSM, licensePlate, carrier, flight, airport, schDateStr, lastModifiedStr);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get(); // To catch exceptions from doInBackground
                            JOptionPane.showMessageDialog(frame, "BSM records inserted successfully!");
                            clearButton.doClick();
                            loadLogsToTextArea();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Insertion Failed", JOptionPane.ERROR_MESSAGE);
                            ex.printStackTrace();
                        }
                    }
                };

                worker.execute();


            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Insertion Failed", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        SwingUtilities.invokeLater(() -> {
            boolean isConnected = BSMInserter.testConnection();
            updateDbStatusIcon(isConnected);
        });

        // Attach log4j appender to redirect logs to logArea
        attachLogAppender();

        frame.setVisible(true);
    }

    private void setupField(JPanel panel, GridBagConstraints gbc, String labelText, JComponent inputField,
                            Font labelFont, Font fieldFont, Dimension fieldSize) {
        JLabel label = new JLabel(labelText);
        label.setFont(labelFont);
        inputField.setFont(fieldFont);
        inputField.setPreferredSize(fieldSize);

        gbc.gridx = 0;
        panel.add(label, gbc);
        gbc.gridx = 1;
        panel.add(inputField, gbc);
        gbc.gridy++;
    }

    // Load logs from file into logArea
    private void loadLogsToTextArea() {
        String logPath = System.getProperty("user.home") + "/OneDrive/Desktop/logs/bsminserter.log";
        File logFile = new File(logPath);
        if (!logFile.exists()) {
            logArea.setText("Log file not found.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            StringBuilder recentLogs = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("BSM Inserting for LPN") ||
                        line.contains("Database Connectivity") ||
                        line.contains("BSM Inserted Successfully")) {
                    recentLogs.append(line).append("\n");
                }

            }
            logArea.setText(recentLogs.toString());
        } catch (IOException e) {
            logArea.setText("Error reading logs: " + e.getMessage());
        }
    }

    //Check database connectivity
    private void updateDbStatusIcon(boolean isConnected) {
        if (isConnected) {
            dbStatusLabel.setIcon(dbIconGreen);
            dbStatusLabel.setToolTipText("Database Connected");
        } else {
            dbStatusLabel.setIcon(dbIconGray);
            dbStatusLabel.setToolTipText("Database Not Connected");
        }
        dbStatusLabel.repaint();
    }

    private void attachLogAppender() {
        // Tell GuiLogAppender where to print logs
        GuiLogAppender.setLogArea(logArea);

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();

        GuiLogAppender guiAppender = GuiLogAppender.createAppender();
        guiAppender.start();

        config.addAppender(guiAppender);
        config.getRootLogger().addAppender(guiAppender, null, null);

        context.updateLoggers();
    }

    public static void main(String[] args) {
        try {
            // Initialize Log4j2 from log4j2.xml inside resources
            String log4jConfig = GUI.class.getClassLoader().getResource("log4j2.xml").toString();
            org.apache.logging.log4j.core.config.Configurator.initialize(null, log4jConfig);
        } catch (Exception e) {
            System.err.println("Failed to load log4j2.xml: " + e.getMessage());
        }

        // Launch GUI safely on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            GUI gui = new GUI();
        });
    }

}
