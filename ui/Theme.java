package ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public final class Theme {

    private Theme() { }   // constants only, no objects

    // Shared application colours.
    public static final Color STRIPE_TEAL = new Color(0x025A6C); // thin teal stripe along the top of the page
    public static final Color BANNER_GREY = new Color(0xF2F3F3); // neutral banner background
    public static final Color WHITE       = new Color(0xFFFFFF); // header, card and footer background
    public static final Color PAGE_GREY   = new Color(0xFAFAFA); // page background behind the card
    public static final Color DIVIDER     = new Color(0xBDC1C2); // divider and border colour
    public static final Color BORDER      = new Color(0xE5E6E7); // soft border colour
    public static final Color PANEL_TINT  = new Color(0xF6F9FB); // panel background tint
    public static final Color BUTTON_BLUE = new Color(0x227AA7); // primary action colour
    public static final Color LINK_BLUE   = new Color(0x195B7D); // active/linked text colour
    public static final Color NAV_NAVY    = new Color(0x113C52);
    public static final Color DEEP_NAVY   = new Color(0x12303B);
    public static final Color LOGO_GREY   = new Color(0x585E60);
    public static final Color AID_GREEN   = new Color(0x3EA11B);
    public static final Color HEADING     = new Color(0x040E13);
    public static final Color TEXT        = new Color(0x212529);

    // Status colours for the forms' status labels.
    public static final Color SUCCESS = AID_GREEN;
    public static final Color ERROR   = NAV_NAVY;

    // ================================================================ FONTS
    // Loaded from the fonts folder (next to db.properties) or from the classpath.
    // If a file is missing, Java's own sans-serif or serif stands in, and the app still runs.
    private static final Font PUBLIC_SANS       = load("PublicSans-Regular.ttf", Font.SANS_SERIF, Font.PLAIN);
    private static final Font PUBLIC_SANS_BOLD  = load("PublicSans-Bold.ttf", Font.SANS_SERIF, Font.BOLD);
    private static final Font ROBOTO_COND       = load("RobotoCondensed-Regular.ttf", Font.SANS_SERIF, Font.PLAIN);
    private static final Font ROBOTO_COND_BOLD  = load("RobotoCondensed-Bold.ttf", Font.SANS_SERIF, Font.BOLD);
    private static final Font MERRIWEATHER      = load("Merriweather-Regular.ttf", Font.SERIF, Font.PLAIN);

    // Shared font sizing used throughout the app.
    public static final Font BANNER     = PUBLIC_SANS.deriveFont(13f);       // top grey banner
    public static final Font NAV        = PUBLIC_SANS.deriveFont(15f);       // sidebar menu
    public static final Font NAV_ACTIVE = PUBLIC_SANS_BOLD.deriveFont(15f);
    public static final Font CAPTION    = PUBLIC_SANS_BOLD.deriveFont(11.5f); // small upper-case captions
    public static final Font LOGO       = PUBLIC_SANS.deriveFont(26f);       // "Azani" 
    public static final Font LOGO_BOLD  = PUBLIC_SANS_BOLD.deriveFont(26f);  // "ISP"  
    public static final Font TITLE      = ROBOTO_COND.deriveFont(26f);       // page title ("Create an Account")
    public static final Font HEADING_F  = ROBOTO_COND_BOLD.deriveFont(18f);  // card titles ("What You'll Need")
    public static final Font BODY       = ROBOTO_COND.deriveFont(15f);       // paragraphs, labels, fields
    public static final Font BUTTON     = ROBOTO_COND_BOLD.deriveFont(15f);  // "Get Started"
    public static final Font KPI        = ROBOTO_COND_BOLD.deriveFont(32f);  // the big dashboard numbers
    public static final Font LIST       = MERRIWEATHER.deriveFont(14f);      // list content: table rows
    public static final Font SMALL      = PUBLIC_SANS.deriveFont(13f);       // footer-sized notes

    /** Looks for a font file in the classpath, then in ./fonts, the same two places DBConnection checks. */
    private static Font load(String file, String fallbackFamily, int fallbackStyle) {
        try (InputStream in = open(file)) {
            if (in != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, in);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                return font;                                      // size 1; deriveFont sets the real size
            }
        } catch (IOException | FontFormatException e) {
            // fall through to Java's built-in font
        }
        return new Font(fallbackFamily, fallbackStyle, 1);
    }

    private static InputStream open(String file) throws IOException {
        InputStream in = Theme.class.getClassLoader().getResourceAsStream("fonts/" + file);
        if (in != null) return in;
        Path path = Path.of("fonts", file);
        return Files.exists(path) ? Files.newInputStream(path) : null;
    }

    // ================================================================ SHARED STYLING HELPERS

    /** White card with a soft outline and inner padding. */
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(20, 24, 20, 24));
    }

    /** Primary action buttons use a flat blue background and bold white text. */
    public static void styleButton(JButton b) {
        b.setFont(BUTTON);
        b.setBackground(BUTTON_BLUE);
        b.setForeground(WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(9, 22, 9, 22));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** One look for every JTable: tinted header like the "What You'll Need" box, serif rows like its list. */
    public static void styleTable(JTable table) {
        table.setRowHeight(36);
        table.setFont(LIST);
        table.setForeground(TEXT);
        table.setBackground(WHITE);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(BORDER);
        table.setSelectionForeground(HEADING);
        JTableHeader header = table.getTableHeader();
        header.setFont(ROBOTO_COND_BOLD.deriveFont(15f));
        header.setBackground(PANEL_TINT);
        header.setForeground(HEADING);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 38));
    }

    // ================================================================ LOOK AND FEEL

    /**
     * Installs Java's built-in Metal look and feel with the app colour palette.
     * Metal paints every control (fields, combo boxes, check boxes, scroll bars) from the theme,
     * so the palette stays consistent across the interface. Call before creating any component.
     */
    public static void install() {
        MetalLookAndFeel.setCurrentTheme(new AzaniMetalTheme());
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            // Metal ships with every JDK, so this cannot normally happen.
        }
        UIManager.put("Button.select", new ColorUIResource(LINK_BLUE));       // button colour while pressed
        UIManager.put("Panel.background", new ColorUIResource(WHITE));
        UIManager.put("OptionPane.background", new ColorUIResource(WHITE));
        UIManager.put("OptionPane.messageForeground", new ColorUIResource(TEXT));
        UIManager.put("ScrollPane.background", new ColorUIResource(WHITE));
        UIManager.put("Viewport.background", new ColorUIResource(WHITE));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(BORDER));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(HEADING));
        UIManager.put("List.selectionBackground", new ColorUIResource(BORDER));
        UIManager.put("List.selectionForeground", new ColorUIResource(HEADING));
    }

    /**
     * A Metal theme is a set of six colours plus fonts. We subclass DefaultMetalTheme
     * and override each getter to return the app's palette.
     */
    private static final class AzaniMetalTheme extends DefaultMetalTheme {
        @Override public String getName() { return "Azani"; }

        // primary: focus, selection and active parts
        @Override protected ColorUIResource getPrimary1() { return new ColorUIResource(NAV_NAVY); }
        @Override protected ColorUIResource getPrimary2() { return new ColorUIResource(BUTTON_BLUE); }
        @Override protected ColorUIResource getPrimary3() { return new ColorUIResource(BORDER); }
        // secondary: borders and control backgrounds
        @Override protected ColorUIResource getSecondary1() { return new ColorUIResource(DIVIDER); }
        @Override protected ColorUIResource getSecondary2() { return new ColorUIResource(BORDER); }
        @Override protected ColorUIResource getSecondary3() { return new ColorUIResource(WHITE); }
        @Override protected ColorUIResource getBlack() { return new ColorUIResource(HEADING); }
        @Override protected ColorUIResource getWhite() { return new ColorUIResource(WHITE); }

        @Override public FontUIResource getControlTextFont() { return new FontUIResource(BODY); }
        @Override public FontUIResource getSystemTextFont()  { return new FontUIResource(BODY); }
        @Override public FontUIResource getUserTextFont()    { return new FontUIResource(BODY); }
        @Override public FontUIResource getMenuTextFont()    { return new FontUIResource(NAV); }
        @Override public FontUIResource getWindowTitleFont() { return new FontUIResource(HEADING_F); }
        @Override public FontUIResource getSubTextFont()     { return new FontUIResource(SMALL); }
    }
}