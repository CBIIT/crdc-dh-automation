/*
 * Inbox judge: runs PythonFiles/CRDC/VerifyNotificationEmails.py against
 * today's NotificationEmail_Checklist.xlsx rows. Fails the case if the
 * script exits non-zero (any Fail, or missing checklist).
 */
CustomKeywords.'utilities.NotificationEmailLog.verifyNotificationEmails'()
