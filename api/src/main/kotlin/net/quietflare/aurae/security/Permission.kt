package net.quietflare.aurae.security

/**
 * What a user may do. Endpoints check these, never role names, so a practice can change who
 * holds which permission in configuration without a code change.
 */
object Permission {
    const val REPORT_UPLOAD = "report:upload"
    const val REPORT_REVIEW = "report:review"
    const val REPORT_SEND_TO_PVS = "report:send-to-pvs"
    const val PATIENT_VALUES_READ = "patient-values:read"
    const val REPORT_ORIGINAL_READ = "report-original:read"
    const val USERS_MANAGE = "users:manage"
    const val AUDIT_READ = "audit:read"
}
