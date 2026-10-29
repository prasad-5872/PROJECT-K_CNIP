package emergency.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Tactical Emergency Operations Center theme definition.
 * Enforces high-contrast, consistent typography, dark navy visual hierarchy,
 * and high-DPI scaling across all Swing components.
 */
public final class Theme {
    private Theme() {}

    // Emergency Operations Center Palette
    public static final Color BG_PAGE           = new Color(10, 15, 26);     // #0A0F1A deep navy
    public static final Color BG_HEADER         = new Color(13, 20, 36);     // #0D1424
    public static final Color BG_SIDEBAR        = new Color(15, 23, 42);     // #0F172A
    public static final Color BG_CARD           = new Color(17, 24, 39);     // #111827
    public static final Color BG_CARD_HOVER     = new Color(26, 36, 56);     // #1A2438
    public static final Color BG_ELEVATED       = new Color(24, 33, 53);     // #182135
    public static final Color BG_INPUT          = new Color(30, 41, 59);     // #1E293B

    public static final Color BORDER_SUBTLE     = new Color(51, 65, 85);     // #334155
    public static final Color BORDER_LIGHT      = new Color(71, 85, 105);    // #475569
    public static final Color BORDER_ACCENT     = new Color(56, 189, 248);   // #38BDF8

    public static final Color TEXT_PRIMARY      = new Color(248, 250, 252);  // #F8FAFC
    public static final Color TEXT_SECONDARY    = new Color(148, 163, 184);  // #94A3B8
    public static final Color TEXT_MUTED        = new Color(100, 116, 139);  // #64748B

    public static final Color CYAN_ACCENT       = new Color(6, 182, 212);    // #06B6D4
    public static final Color BLUE_ACCENT       = new Color(56, 189, 248);   // #38BDF8
    public static final Color GREEN_OPERATIONAL = new Color(16, 185, 129);   // #10B981
    public static final Color AMBER_WARNING     = new Color(245, 158, 11);   // #F59E0B
    public static final Color RED_FAILURE       = new Color(239, 68, 68);    // #EF4444
    public static final Color PURPLE_ANALYTICS  = new Color(139, 92, 246);   // #8B5CF6

    // Scaled Typography
    public static final Font FONT_HEADER        = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font FONT_TITLE         = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_SECTION       = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_REGULAR       = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL         = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO          = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_STAT_VALUE    = new Font("Segoe UI", Font.BOLD, 22);

    public static Border createPanelPadding() {
        return new EmptyBorder(12, 12, 12, 12);
    }

    public static Border createSectionBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                " " + title + " ",
                0, 0, FONT_SECTION, BLUE_ACCENT
        );
    }

    public static void applyAntiAliasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
}
