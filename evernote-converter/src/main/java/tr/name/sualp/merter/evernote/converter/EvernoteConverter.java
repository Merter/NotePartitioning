
package tr.name.sualp.merter.evernote.converter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tr.name.sualp.merter.evernote.converter.mail.MailSender;

public class EvernoteConverter {

  private static final String END_LINE = "line.separator";
  private static final Pattern MY_DATE_TIME_PATTERN = Pattern
      .compile("(\\d\\d).(\\d\\d).(\\d\\d\\d\\d)\\s*-\\s*(\\d\\d)(\\d\\d)");

  private static final Logger logger = LogManager.getLogger(EvernoteConverter.class);

  private EvernoteConverter() {
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      logger.info("Usage : java EvernoteConverter <file_name>");
      System.exit(1);
    }
    String fileName = args[0].trim();
    BufferedReader br = null;
    String line = "";

    // find the first line with date - time

    try {
      br = new BufferedReader(new FileReader(fileName));
      line = br.readLine();
    } catch (FileNotFoundException ex) {
      StringBuilder sb = new StringBuilder("File ");
      sb.append(fileName);
      sb.append(" not found!");
      logger.error(sb.toString());
      System.exit(2);
    } catch (IOException ex) {
      StringBuilder sb = new StringBuilder("File ");
      sb.append(fileName);
      sb.append(" IO error!");
      logger.error(sb.toString());
      System.exit(3);
    }

    Map<String, Map<String, List<String>>> monthlyRecords = new TreeMap<>();
    String previousDay = "";
    String previousMonth = "";
    String previousYear = "";
    String keyDate = "";
    String keyTime = "";

    while (line != null) {
      Matcher matcher = MY_DATE_TIME_PATTERN.matcher(line);
      if (matcher.find()) {
        String newDay = matcher.group(1);
        String newMonth = matcher.group(2);
        String newYear = matcher.group(3);
        String newHour = matcher.group(4);
        String newMinute = matcher.group(5);
        keyDate = newYear + newMonth + newDay;
        keyTime = newHour + newMinute;

        if (!newMonth.equals(previousMonth) || !newYear.equals(previousYear)) {
          if (!"".equals(previousMonth))
            try {
              mailNewNote(monthlyRecords, generateNoteHeader(newYear, newMonth));
            } catch (MessagingException mex) {
              logger.error(mex.getMessage());
            }
          monthlyRecords = new TreeMap<>();
        }
        if (previousDay.equals(newDay) && previousMonth.equals(newMonth) && previousYear.equals(newYear)) {
          Map<String, List<String>> dailyRecords = monthlyRecords.get(keyDate);
          dailyRecords.put(keyTime, new LinkedList<String>());
        } else {
          previousDay = newDay;
          previousMonth = newMonth;
          previousYear = newYear;
          Map<String, List<String>> dailyRecords = new TreeMap<>();
          monthlyRecords.put(keyDate, dailyRecords);
        }
      } else {
        Map<String, List<String>> dailyRecords = monthlyRecords.get(keyDate);
        addActivityToRecords(line, dailyRecords, keyTime);
      }
      try {
        line = br.readLine();
      } catch (IOException ex) {
        StringBuilder sb = new StringBuilder("File ");
        sb.append(fileName);
        sb.append(" IO error!");
        logger.error(sb.toString());
        System.exit(3);
      }
    }
    try {
      br.close();
      mailNewNote(monthlyRecords, generateNoteHeader(previousYear, previousMonth));
    } catch (MessagingException mex) {
      logger.error(mex.getMessage());
    } catch (IOException ex) {
      StringBuilder sb = new StringBuilder("File ");
      sb.append(fileName);
      sb.append(" IO error!");
      logger.error(sb.toString());
      System.exit(4);
    }
  }

  private static String generateNoteHeader(String year, String month) {
    StringBuilder stb = new StringBuilder();
    stb.append("Journal-");
    stb.append(year);
    stb.append("-");
    stb.append(month);
    return stb.toString();
  }

  private static void mailNewNote(Map<String, Map<String, List<String>>> monthlyRecords, String subjectAsNoteHeader)
      throws MessagingException {
    StringBuilder stb = new StringBuilder();
    for (Entry<String, Map<String, List<String>>> dateEntry : monthlyRecords.entrySet()) {
      stb.append("## ");
      stb.append(dateEntry.getKey());
      stb.append(System.getProperty(END_LINE));
      Map<String, List<String>> dailyRecords = dateEntry.getValue();
      for (Entry<String, List<String>> timeEntry : dailyRecords.entrySet()) {
        stb.append("@");
        stb.append(timeEntry.getKey());
        stb.append(System.getProperty(END_LINE));
        List<String> hourlyActivities = timeEntry.getValue();
        for (String activity : hourlyActivities) {
          stb.append(activity);
          stb.append(System.getProperty(END_LINE));
        }
        stb.append(System.getProperty(END_LINE));
      }
    }
    if (stb.length() > 0) {
      MailSender mse = MailSender.mailSenderFactory("merter.sualp@tcmb.gov.tr", subjectAsNoteHeader, stb.toString());
      mse.sendThis();
    }
  }

  private static void addActivityToRecords(String line, Map<String, List<String>> dailyRecords, String keyTime) {
    String activity = line.trim();
    if (activity.startsWith("* ") || activity.startsWith("- ")) {
      activity = activity.substring(2).trim();
    }
    if ("".equals(activity))
      return;

    List<String> hourlyActivities = dailyRecords.get(keyTime);
    if (hourlyActivities == null) {
      hourlyActivities = new LinkedList<>();
      dailyRecords.put(keyTime, hourlyActivities);
    }
    hourlyActivities.add(activity);
  }
}
