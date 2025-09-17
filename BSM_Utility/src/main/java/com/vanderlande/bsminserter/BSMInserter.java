package com.vanderlande.bsminserter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.sql.*;

public class BSMInserter {

    private static final Logger logger = LogManager.getLogger(BSMInserter.class);

    public static void main(String[] args) {
        try {
            boolean dbConnected = testConnection();
            if (dbConnected) {
                logger.info("Database Connectivity : TRUE");
            } else {
                logger.error("Database Connectivity : FALSE");
                return;
            }
        } catch (Exception e) {
            logger.error("Unexpected error while testing DB connection", e);
        }
    }

    public BSMInserter() {}

    public static void insertBSM(int totalBSM, String licensePlatePrefix, String CarDsntr, String FlgtNum,
                                 String OffLArpt, String SchDate, String LstMfdTimeStp) throws Exception {

        Properties props = new Properties();
        try (InputStream fis = BSMInserter.class.getResourceAsStream("/db.properties")) {
            if (fis == null) {
                throw new FileNotFoundException("db.properties not found in classpath");
            }
            props.load(fis);
        }

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.username");
        String password = props.getProperty("db.password");

        String insertSQL = "INSERT INTO BSM_Table (ID, LICENSE_PLATE, ORIGIN, CARRIER_DESIGNATOR, FLIGHT_NUMBER, SUFFIX, " +
                "SCHEDULED_DATE, BAG_MESSAGE_STATUS, PASSENGER, CARRIER_CLASS_OF_SERVICE, RISK_LEVEL, SCREEN_RESULT, " +
                "SCREEN_RESULT_REASON, SCREEN_METHOD, EXCEPTION_TYPE, AUTHORITY_TO_LOAD, OFFLOAD_AIRPORT, " +
                "INBOUND_CARRIER_DESIGNATOR, INBOUND_AIRPORT, ONWARD_CARRIER_DESIGNATOR, ONWARD_AIRPORT, ONWARD_FLIGHT_INFO, " +
                "PRINTER_ID, CHECKIN_LOCATION, V_AIRPORT_CODE, V_DATA_DICTIONARY, V_BAG_SOURCE_INDIC, RECORD_STATUS, TYPE_BINFO, " +
                "LAST_MODIFIED_TIME_STAMP, LAST_MODIFIED_BY, VERSION) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        final int BATCH_SIZE = 10000;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            conn.setAutoCommit(false);

            int lastSuffix = getLastSuffix(conn);
            long lastId = getLastId(conn);

            long startTime = System.currentTimeMillis();

            for (int i = 1; i <= totalBSM; i++) {
                long id = ++lastId;
                String idStr = String.format("%012d", id);

                lastSuffix++;

                String licPlate = String.format("%s%05d", licensePlatePrefix, lastSuffix);

                logger.info("BSM Inserting for LPN : {}", licPlate);

                pstmt.setString(1, idStr);
                pstmt.setString(2, licPlate);
                pstmt.setString(3, BSMConstants.ORIGIN);
                pstmt.setString(4, CarDsntr);
                pstmt.setString(5, FlgtNum);
                pstmt.setNull(6, Types.NULL);
                pstmt.setString(7, SchDate);
                pstmt.setString(8, BSMConstants.BAG_MESSAGE_STATUS);
                pstmt.setString(9, BSMConstants.PASSENGER);
                pstmt.setNull(10, Types.NULL);
                pstmt.setNull(11, Types.NULL);
                pstmt.setNull(12, Types.NULL);
                pstmt.setNull(13, Types.NULL);
                pstmt.setNull(14, Types.NULL);
                pstmt.setNull(15, Types.NULL);
                pstmt.setNull(16, Types.NULL);
                pstmt.setString(17, OffLArpt);
                pstmt.setNull(18, Types.NULL);
                pstmt.setNull(19, Types.NULL);
                pstmt.setNull(20, Types.NULL);
                pstmt.setNull(21, Types.NULL);
                pstmt.setNull(22, Types.NULL);
                pstmt.setNull(23, Types.NULL);
                pstmt.setNull(24, Types.NULL);
                pstmt.setNull(25, Types.NULL);
                pstmt.setNull(26, Types.NULL);
                pstmt.setNull(27, Types.NULL);
                pstmt.setString(28, BSMConstants.RECORD_STATUS);
                pstmt.setNull(29, Types.NULL);
                pstmt.setTimestamp(30, Timestamp.valueOf(LstMfdTimeStp));
                pstmt.setString(31, BSMConstants.LAST_MODIFIED_BY);
                pstmt.setInt(32, BSMConstants.VERSION);

                pstmt.addBatch();

                if (i % BATCH_SIZE == 0) {
                    pstmt.executeBatch();
                    conn.commit();
                    pstmt.clearBatch();
                }
            }

            pstmt.executeBatch();
            conn.commit();

            long endTime = System.currentTimeMillis();
            double durationSec = (endTime - startTime) / 1000.0;
            logger.info("BSM Inserted Successfully in {} sec", String.format("%.2f", durationSec));

        } catch (Exception e) {
            logger.error("Error while inserting data", e);
            throw e;
        }
    }

    public static boolean testConnection() {
        try {
            Properties props = new Properties();
            try (InputStream fis = BSMInserter.class.getResourceAsStream("/db.properties")) {
                if (fis == null) {
                    throw new FileNotFoundException("db.properties not found in classpath");
                }
                props.load(fis);
            }

            String url = props.getProperty("db.url");
            String user = props.getProperty("db.username");
            String password = props.getProperty("db.password");

            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                boolean connected = conn != null && conn.isValid(2);
                if (connected) {
                    logger.info("Database Connectivity : TRUE");
                } else {
                    logger.error("Database Connectivity : FALSE");
                }
                return connected;
            }
        } catch (Exception e) {
            logger.error("Database Connectivity : FALSE", e);
            return false;
        }
    }

    private static int getLastSuffix(Connection conn) throws Exception {
        String query = "SELECT MAX(CAST(RIGHT(LICENSE_PLATE, 5) AS INT)) FROM BSM_Table";
        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next() && rs.getObject(1) != null) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private static long getLastId(Connection conn) throws Exception {
        String query = "SELECT MAX(CAST(ID AS BIGINT)) FROM BSM_Table";
        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next() && rs.getObject(1) != null) {
                return rs.getLong(1);
            }
        }
        return 0L;
    }
}