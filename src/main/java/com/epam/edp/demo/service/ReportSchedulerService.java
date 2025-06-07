package com.epam.edp.demo.service;

import com.epam.edp.demo.entity.LocationComparisonEntry;
import com.epam.edp.demo.entity.ReportPeriod;
import com.epam.edp.demo.entity.StaffPerformanceEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportSchedulerService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ReportGenerationService reportGenerationService;
    private final EmailService emailService;

    @Autowired
    public ReportSchedulerService(
            ReportGenerationService reportGenerationService,
            EmailService emailService) {
        this.reportGenerationService = reportGenerationService;
        this.emailService = emailService;
    }

    // Run weekly on Monday at 1:00 AM
    @Scheduled(cron = "0 0 7 * * SUN")
    public void generateWeeklyReports() {
        logger.info("Starting weekly report generation");
        try {
            // Generate reports for the last 7 days
            ReportPeriod weeklyPeriod = reportGenerationService.calculateReportPeriod(7, 7);

            // Generate and send staff performance report
            List<StaffPerformanceEntry> staffEntries =
                    reportGenerationService.generateStaffPerformanceReport(weeklyPeriod);
            emailService.sendStaffPerformanceReport(staffEntries, weeklyPeriod);

            // Generate and send location comparison report
            List<LocationComparisonEntry> locationEntries =
                    reportGenerationService.generateLocationComparisonReport(weeklyPeriod);
            emailService.sendLocationComparisonReport(locationEntries, weeklyPeriod);

            logger.info("Weekly report generation and email sending completed successfully");
        } catch (Exception e) {
            logger.error("Error generating weekly reports: {}", e.getMessage(), e);
        }
    }

    // Run monthly on the 1st at 2:00 AM
    @Scheduled(cron = "0 0 12 * * SUN")
    public void generateMonthlyReports() {
        logger.info("Starting monthly report generation");
        try {
            // Generate reports for the last 30 days
            ReportPeriod monthlyPeriod = reportGenerationService.calculateReportPeriod(30, 30);

            // Generate and send reports
            List<StaffPerformanceEntry> staffEntries =
                    reportGenerationService.generateStaffPerformanceReport(monthlyPeriod);
            emailService.sendStaffPerformanceReport(staffEntries, monthlyPeriod);

            List<LocationComparisonEntry> locationEntries =
                    reportGenerationService.generateLocationComparisonReport(monthlyPeriod);
            emailService.sendLocationComparisonReport(locationEntries, monthlyPeriod);

            logger.info("Monthly report generation and email sending completed successfully");
        } catch (Exception e) {
            logger.error("Error generating monthly reports: {}", e.getMessage(), e);
        }
    }

    // For testing or on-demand report generation
    public void generateReportForCustomPeriod(int daysBack, int periodLength) {
        logger.info("Generating custom period report for last {} days", daysBack);
        try {
            ReportPeriod customPeriod = reportGenerationService.calculateReportPeriod(daysBack, periodLength);

            List<StaffPerformanceEntry> staffEntries =
                    reportGenerationService.generateStaffPerformanceReport(customPeriod);
            emailService.sendStaffPerformanceReport(staffEntries, customPeriod);

            List<LocationComparisonEntry> locationEntries =
                    reportGenerationService.generateLocationComparisonReport(customPeriod);
            emailService.sendLocationComparisonReport(locationEntries, customPeriod);

            logger.info("Custom report generated successfully");
        } catch (Exception e) {
            logger.error("Error generating custom report: {}", e.getMessage(), e);
        }
    }
}