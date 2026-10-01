package com.forge.autophone;

/**
 * Forge AutoPhone — AIDL interface for Forge OS integration.
 *
 * Forge OS binds to com.forge.autophone and calls these methods to:
 *   a) Control the device UI via accessibility (tap, type, scroll, etc.)
 *   b) Read, dismiss, and reply to status-bar notifications
 *   c) Notify AutoPhone when a schedule plan starts / finishes
 *
 * This interface must be kept in sync with the copy in Forge OS.
 */
interface IAutoPhoneService {

    // ── Screen-control tools ─────────────────────────────────────────────────
    String readScreen();
    String tapByText(String text);
    String tapAt(int x, int y);
    String typeText(String text);
    String swipe(String direction, int amount);
    String scroll(String direction);
    String launchApp(String packageOrLabel);
    String goBack();
    String goHome();
    String openNotifications();
    String screenshot();
    String findAndTap(String text);
    boolean isServiceActive();

    // ── Notification tools ───────────────────────────────────────────────────
    String readNotifications();
    String dismissNotification(String key);
    String replyToNotification(String key, String text);
    boolean isNotificationListenerActive();

    // OCR tools. Text recognition over a MediaProjection screenshot; reaches apps
    // whose UI exposes no accessibility labels (games, canvas, image-heavy
    // views). Requires screen-capture consent - see screenshot() for the error
    // text returned when it is missing.
    String ocrReadScreen();
    String ocrFindText(String query);
    String ocrFindAllText(String query);
    boolean ocrTapText(String query);

    // Icon-template tools. Register a PNG/JPEG (base64) as a named template,
    // then find it on screen. Templates live only in this process memory and
    // are lost on restart.
    String registerIcon(String name, String base64Image);
    String unregisterIcon(String name);
    String listIcons();
    String findIcon(String name, double threshold);
    String findAllIcons(String name, double threshold, int maxMatches);
    boolean isIconVisible(String name, double threshold);

    // Screen context: current app + screen type from the accessibility tree.
    String describeContext();
    // ── Schedule lifecycle (Forge OS → AutoPhone) ─────────────────────────────
    oneway void notifyScheduleStarted(String scheduleId, String planSummary);
    oneway void notifyScheduleCompleted(String scheduleId, boolean ok, String result);
}
