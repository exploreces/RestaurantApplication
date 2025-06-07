package com.epam.edp.demo.service;


import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import com.epam.edp.demo.entity.LocationComparisonEntry;
import com.epam.edp.demo.entity.ReportPeriod;
import com.epam.edp.demo.entity.StaffPerformanceEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AmazonSimpleEmailService sesClient;


    private String senderEmail = "golanihimanshu@gmail.com";

    private String[] recipientEmails = new String[]{"himanshu_golani@epam.com"};

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Autowired
    public EmailService(AmazonSimpleEmailService sesClient) {
        this.sesClient = sesClient;
    }

    public void sendStaffPerformanceReport(List<StaffPerformanceEntry> entries, ReportPeriod period) {
        String subject = "Staff Performance Report - " +
                DATE_FORMATTER.format(period.getStartDate()) + " to " +
                DATE_FORMATTER.format(period.getEndDate());

        String htmlBody = generateStaffPerformanceHtml(entries, period);

        sendEmail(subject, htmlBody);
    }

    public void sendLocationComparisonReport(List<LocationComparisonEntry> entries, ReportPeriod period) {
        String subject = "Location Comparison Report - " +
                DATE_FORMATTER.format(period.getStartDate()) + " to " +
                DATE_FORMATTER.format(period.getEndDate());

        String htmlBody = generateLocationComparisonHtml(entries, period);

        sendEmail(subject, htmlBody);
    }

    private void sendEmail(String subject, String htmlBody) {
        try {
            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(new Destination().withToAddresses(recipientEmails))
                    .withMessage(new Message()
                            .withBody(new Body().withHtml(new Content().withCharset("UTF-8").withData(htmlBody)))
                            .withSubject(new Content().withCharset("UTF-8").withData(subject)))
                    .withSource(senderEmail);

            sesClient.sendEmail(request);
            logger.info("Email sent successfully: {}", subject);
        } catch (Exception e) {
            logger.error("Failed to send email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String generateStaffPerformanceHtml(List<StaffPerformanceEntry> entries, ReportPeriod period) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("table {border-collapse: collapse; width: 100%;}");
        html.append("th, td {border: 1px solid #dddddd; text-align: left; padding: 8px;}");
        html.append("th {background-color: #f2f2f2;}");
        html.append("tr:nth-child(even) {background-color: #f9f9f9;}");
        html.append("</style></head><body>");

        html.append("<h2>Staff Performance Report</h2>");
        html.append("<p>Period: ").append(DATE_FORMATTER.format(period.getStartDate()))
                .append(" to ").append(DATE_FORMATTER.format(period.getEndDate())).append("</p>");

        html.append("<table>");
        html.append("<tr>");
        html.append("<th>Location</th>");
        html.append("<th>Waiter Name</th>");
        html.append("<th>Waiter Email</th>");
        html.append("<th>Start Date</th>");
        html.append("<th>End Date</th>");
        html.append("<th>Hours Worked</th>");
        html.append("<th>Orders Processed</th>");
        html.append("<th>Orders Delta (%)</th>");
        html.append("<th>Avg Feedback</th>");
        html.append("<th>Min Feedback</th>");
        html.append("<th>Min Feedback Delta (%)</th>");
        html.append("</tr>");

        for (StaffPerformanceEntry entry : entries) {
            html.append("<tr>");
            html.append("<td>").append(entry.getLocationName()).append("</td>");
            html.append("<td>").append(entry.getWaiterName()).append("</td>");
            html.append("<td>").append(entry.getWaiterEmail()).append("</td>");
            html.append("<td>").append(DATE_FORMATTER.format(entry.getReportStartDate())).append("</td>");
            html.append("<td>").append(DATE_FORMATTER.format(entry.getReportEndDate())).append("</td>");
            html.append("<td>").append(entry.getHoursWorked()).append("</td>");
            html.append("<td>").append(entry.getOrdersProcessed()).append("</td>");
            html.append("<td>").append(formatPercent(entry.getOrdersProcessedDeltaPercent())).append("</td>");
            html.append("<td>").append(String.format("%.1f", entry.getAverageServiceFeedback())).append("</td>");
            html.append("<td>").append(entry.getMinimumServiceFeedback()).append("</td>");
            html.append("<td>").append(formatPercent(entry.getMinimumServiceFeedbackDeltaPercent())).append("</td>");
            html.append("</tr>");
        }

        html.append("</table></body></html>");
        return html.toString();
    }

    private String generateLocationComparisonHtml(List<LocationComparisonEntry> entries, ReportPeriod period) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("table {border-collapse: collapse; width: 100%;}");
        html.append("th, td {border: 1px solid #dddddd; text-align: left; padding: 8px;}");
        html.append("th {background-color: #f2f2f2;}");
        html.append("tr:nth-child(even) {background-color: #f9f9f9;}");
        html.append("</style></head><body>");

        html.append("<h2>Location Comparison Report</h2>");
        html.append("<p>Period: ").append(DATE_FORMATTER.format(period.getStartDate()))
                .append(" to ").append(DATE_FORMATTER.format(period.getEndDate())).append("</p>");

        html.append("<table>");
        html.append("<tr>");
        html.append("<th>Location</th>");
        html.append("<th>Start Date</th>");
        html.append("<th>End Date</th>");
        html.append("<th>Orders Processed</th>");
        html.append("<th>Orders Delta (%)</th>");
        html.append("<th>Avg Cuisine Feedback</th>");
        html.append("<th>Min Cuisine Feedback</th>");
        html.append("<th>Avg Feedback Delta (%)</th>");
        html.append("<th>Revenue</th>");
        html.append("<th>Revenue Delta (%)</th>");
        html.append("</tr>");

        for (LocationComparisonEntry entry : entries) {
            html.append("<tr>");
            html.append("<td>").append(entry.getLocationName()).append("</td>");
            html.append("<td>").append(DATE_FORMATTER.format(entry.getReportStartDate())).append("</td>");
            html.append("<td>").append(DATE_FORMATTER.format(entry.getReportEndDate())).append("</td>");
            html.append("<td>").append(entry.getOrdersProcessed()).append("</td>");
            html.append("<td>").append(formatPercent(entry.getOrdersProcessedDeltaPercent())).append("</td>");
            html.append("<td>").append(String.format("%.1f", entry.getAverageCuisineFeedback())).append("</td>");
            html.append("<td>").append(entry.getMinimumCuisineFeedback()).append("</td>");
            html.append("<td>").append(formatPercent(entry.getAverageCuisineFeedbackDeltaPercent())).append("</td>");
            html.append("<td>").append(String.format("%.2f", entry.getRevenue())).append("</td>");
            html.append("<td>").append(formatPercent(entry.getRevenueDeltaPercent())).append("</td>");
            html.append("</tr>");
        }

        html.append("</table></body></html>");
        return html.toString();
    }

    private String formatPercent(Double value) {
        String prefix = value >= 0 ? "+" : "";
        return prefix + String.format("%.1f%%", value);
    }
}