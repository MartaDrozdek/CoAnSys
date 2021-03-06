/*
 * (C) 2010-2012 ICM UW. All rights reserved.
 */
package pl.edu.icm.coansys.importers;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import pl.edu.icm.synat.api.services.audit.model.AuditEntry;

/**
 *
 * @author Artur Czeczko <a.czeczko@icm.edu.pl>
 * 
 * This class generates a list of dummy log entries for tests.
 * 
 */
public class GenerateDummyLogs {

    private static class AuditEntryComparator implements Comparator<AuditEntry> {

        @Override
        public int compare(AuditEntry o1, AuditEntry o2) {
            return o1.getTimestamp().compareTo(o2.getTimestamp());
        }
    }
    
    private static final int SESSIONMIN = 5;
    private static final int SESSIONMAX = 40;
    private static String startLogs = "2012-01-01";
    private static String endLogs = "2012-08-01";
    private static final String[] EVENTTYPES = {"SAVE_TO_DISK"};
    private static final String[] IPADDRESSES = {"173.194.70.101", "173.194.70.102",
        "173.194.70.113", "173.194.70.138", "173.194.70.139",
        "173.194.70.100", "2a00:1450:400d:803::1004"};
    private static final String[] URLS = {"http://server/url1", "http://server/url2", "http://server/url3", "http://server/url4"};
    private static final String[] USERS = {"user1", "user2", "user3", "user4", "user5"};
    private static final String[] RESOURCES = {"resource1", "resource2", "resource3", "resource4", "resource5",
        "resource6", "resource7", "resource8", "resource9", "resource10",
        "resource11", "resource12", "resource13", "resource15", "resource16"};
    private static final Random random = new Random(System.currentTimeMillis());

    private static String generateRandomId() {
        return new BigInteger(70, random).toString(32);
    }

    public static List<AuditEntry> generateLogs(int loglines) throws ParseException, MalformedURLException {

        List<AuditEntry> result = new ArrayList<AuditEntry>();

        long startTime = new SimpleDateFormat("yyyy-MM-dd").parse(startLogs).getTime();
        long endTime = new SimpleDateFormat("yyyy-MM-dd").parse(endLogs).getTime();

        while (result.size() < loglines) {
            //new session in logs
            String sessionId = generateRandomId();

            //session's start and end timestamps
            float randomFloat = random.nextFloat();
            long sessionStart = startTime + (long) (randomFloat * (endTime - startTime));
            long sessionEnd = sessionStart;
            while (sessionEnd == sessionStart) {
                randomFloat = random.nextFloat();
                sessionEnd = startTime + (long) (randomFloat * (endTime - startTime));
            }
            if (sessionEnd > sessionStart) {
                long tmp = sessionStart;
                sessionStart = sessionEnd;
                sessionEnd = tmp;
            }

            //user
            String user = USERS[random.nextInt(USERS.length)];

            //entries in this session
            int sessionLength = random.nextInt(SESSIONMAX - SESSIONMIN) + SESSIONMIN;
            if (sessionLength > loglines - result.size()) {
                sessionLength = loglines - result.size();
            }

            for (int i = 0; i < sessionLength; i++) {
                //one log entry

                //timestamp
                randomFloat = random.nextFloat();
                long time = sessionStart + (long) (randomFloat * (sessionEnd - sessionStart));
                
                //args
                String eventType = EVENTTYPES[random.nextInt(EVENTTYPES.length)];                
                String args[];
                if (eventType.equals("SAVE_TO_DISK")) {
                    args = new String[6];
                    args[5] = RESOURCES[random.nextInt(RESOURCES.length)];
                } else {
                    args = new String[5];
                }
                args[0] = IPADDRESSES[random.nextInt(IPADDRESSES.length)];
                args[1] = URLS[random.nextInt(URLS.length)]; // URL
                args[2] = URLS[random.nextInt(URLS.length)]; // referrer
                args[3] = sessionId;
                args[4] = user;

                AuditEntry newLog = new AuditEntry(generateRandomId(), AuditEntry.Level.INFO, new Date(time), "PORTAL", eventType, args);
                result.add(newLog);
            }
        }

        Collections.sort(result, new AuditEntryComparator());
        return result;
    }
}
