/*
 * Inbox judge + Git content parity: one VerifyNotificationEmails.py --git-parity
 * run against today's NotificationEmail_Checklist.xlsx rows. Gmail Result is
 * written first, then Git parity for allowlisted To rows that Gmail-Passed.
 * One process, so Git parity still runs when Gmail has Fail rows. Fails the
 * case if the script exits non-zero (any Gmail Fail, Git Fail, or missing checklist).
 */
CustomKeywords.'utilities.NotificationEmailLog.verifyNotificationEmailsWithGitParity'()
