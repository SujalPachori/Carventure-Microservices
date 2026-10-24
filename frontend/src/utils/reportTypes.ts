// Mirrors the backend's ReportType enum (adminreporting/model/ReportType.java).
// Update this list whenever a report type is added or retired there —
// it's the one place the frontend needs to change.
export const ACTIVE_REPORT_TYPES = ['SALES', 'INVENTORY', 'INSPECTIONS', 'USERS'];

export function isActiveReportType(reportType) {
  return ACTIVE_REPORT_TYPES.includes(reportType?.toUpperCase());
}