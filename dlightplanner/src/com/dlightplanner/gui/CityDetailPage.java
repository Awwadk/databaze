package com.dlightplanner.gui;

import com.dlightplanner.models.City;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CityDetailPage extends JFrame {

    private City city;
    private float gradientProgress = 0.0f;

    public CityDetailPage(City city) {
        this.city = city;

        setTitle(city.getName());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true); // Allow resizing/minimizing
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Default to maximized

        addWindowStateListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowStateChanged(java.awt.event.WindowEvent e) {
                if ((e.getNewState() & JFrame.ICONIFIED) != 0) {
                    setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            }
        });

        initUI();
        startBackgroundAnimation();
        setVisible(true);
    }
    
    private void startBackgroundAnimation() {
        javax.swing.Timer animationTimer = new javax.swing.Timer(16, e -> {
            gradientProgress += 0.002f;
            if (gradientProgress >= 1.0f) {
                gradientProgress = 0.0f;
            }
            repaint();
        });
        animationTimer.start();
    }

    private void initUI() {

        ThemedBackgroundPanel bgPanel = new ThemedBackgroundPanel();
        bgPanel.setLayout(new BorderLayout());
        
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabs.setOpaque(false); // Make tabs transparent to show background
        tabs.setBackground(new Color(0, 0, 0, 0)); // Transparent

        tabs.addTab("Overview", createTabContent(
            "Overview: Book your visit to " + city.getName(),
            city.getOverview(),
            getImagePath("overview")
        ));

        tabs.addTab("Attractions", createTabContent(
            "Attractions: Things to do in " + city.getName(),
            city.getAttractions(),
            getImagePath("attractions")
        ));

        tabs.addTab("Activities", createTabContent(
            "Activities: Activities in " + city.getName(),
            city.getActivities(),
            getImagePath("activities")
        ));

        tabs.addTab("Dining", createTabContent(
            "Dining: Food in " + city.getName(),
            city.getDining(),
            getImagePath("dining")
        ));

        tabs.addTab("Shopping", createTabContent(
            "Shopping: Shopping in " + city.getName(),
            city.getShopping(),
            getImagePath("shopping")
        ));

        bgPanel.add(tabs, BorderLayout.CENTER);
        add(bgPanel);
    }

    private class ThemedBackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int width = getWidth();
            int height = getHeight();

            Color color1 = new Color(240, 245, 255); // Calm - soft blue-white
            Color color2 = new Color(255, 240, 245); // Beauty - soft lavender-pink
            Color color3 = new Color(255, 250, 240); // Adventure - warm cream

            float progress = gradientProgress;
            Color startColor = interpolateColor(color1, color2, progress);
            Color endColor = interpolateColor(color2, color3, progress);
            
            GradientPaint gradient = new GradientPaint(
                0, 0, startColor,
                width, height, endColor
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, width, height);

            RadialGradientPaint radialOverlay = new RadialGradientPaint(
                width * 0.3f, height * 0.2f, width * 0.8f,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{
                    new Color(255, 255, 255, 30),
                    new Color(255, 255, 255, 10),
                    new Color(255, 255, 255, 0)
                }
            );
            g2d.setPaint(radialOverlay);
            g2d.fillRect(0, 0, width, height);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
            g2d.setColor(new Color(100, 150, 220));
            for (int i = 0; i < 8; i++) {
                float x = width * (0.1f + i * 0.12f);
                float y = height * (0.3f + (i % 3) * 0.25f);
                int size = 150 + (i % 3) * 50;
                g2d.fillOval((int)(x - size/2), (int)(y - size/2), size, size);
            }
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        
        private Color interpolateColor(Color c1, Color c2, float t) {
            int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * t);
            int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
            int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t);
            return new Color(r, g, b);
        }
    }

    private String getImagePath(String section) {



        String cityName = city.getName().toLowerCase().replace(" ", "_");
        return "resources/images/" + cityName + "/" + section + ".jpg";
    }

    private JPanel createTabContent(String title, String content, String imagePath) {

        DecorativeSectionPanel sectionPanel = new DecorativeSectionPanel(title);
        sectionPanel.setLayout(new BorderLayout());
        
        JPanel tabPanel = new JPanel(new BorderLayout(20, 0));
        tabPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        tabPanel.setOpaque(false); // Transparent to show decorative background

        JPanel textContainer = new JPanel(new BorderLayout());
        textContainer.setOpaque(false);
        textContainer.setBorder(new EmptyBorder(50, 70, 0, 0)); // 50px top margin, 70px left margin
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 24));
        titleLabel.setForeground(new Color(40, 50, 70));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        textPanel.add(titleLabel);


        String htmlContent = "<html><body style='font-family:Georgia; font-size:13px; line-height:1.30; padding:20px; color:#3c3c3c;'>" + 
                            content.replace("\n", "<br><br>") + "</body></html>";

        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setText(htmlContent);
        textPane.setEditable(false);

        textPane.setBackground(new Color(255, 255, 255, 200)); // Semi-transparent white
        textPane.setForeground(new Color(60, 60, 60));
        textPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 2),
            new EmptyBorder(20, 20, 20, 20)
        ));
        textPane.setOpaque(true);

        textPane.setSize(new Dimension(500, Integer.MAX_VALUE));
        Dimension textSize = textPane.getPreferredSize();
        int contentHeight = textSize.height; // Exact height, no extra white space
        textPane.setPreferredSize(new Dimension(500, contentHeight));
        textPane.setMinimumSize(new Dimension(500, contentHeight));
        textPane.setMaximumSize(new Dimension(500, contentHeight));
        textPane.setSize(new Dimension(500, contentHeight));

        textPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(textPane);

        textContainer.add(textPanel, BorderLayout.CENTER);

        JPanel imageContainer = new JPanel(new BorderLayout());
        imageContainer.setOpaque(false);
        imageContainer.setBorder(new EmptyBorder(50, 0, 0, 220)); // 50px top margin (match text), 220px right margin (70 + 150)
        
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setOpaque(false);
        imagePanel.setPreferredSize(new Dimension(300, 250)); // Smaller, shorter rectangle

        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);

        int panelWidth = 300;
        int panelHeight = 250;
        
        try {
            ImageIcon icon = new ImageIcon(imagePath);
            if (icon.getIconWidth() > 0) {

                Image scaledImage = icon.getImage().getScaledInstance(panelWidth, panelHeight, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
            } else {

                imageLabel.setText("Image not available");
                imageLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                imageLabel.setForeground(new Color(150, 150, 150));
            }
        } catch (Exception e) {

            imageLabel.setText("Image not available");
            imageLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            imageLabel.setForeground(new Color(150, 150, 150));
        }

        imageLabel.setBorder(null);
        imageLabel.setPreferredSize(new Dimension(panelWidth, panelHeight));
        imageLabel.setMinimumSize(new Dimension(panelWidth, panelHeight));
        imageLabel.setMaximumSize(new Dimension(panelWidth, panelHeight));
        imageLabel.setOpaque(false);
        
        imagePanel.add(imageLabel, BorderLayout.CENTER);

        imageContainer.add(imagePanel, BorderLayout.CENTER);

        tabPanel.add(textContainer, BorderLayout.WEST);
        tabPanel.add(imageContainer, BorderLayout.EAST);

        sectionPanel.add(tabPanel, BorderLayout.CENTER);

        return sectionPanel;
    }

    private class DecorativeSectionPanel extends JPanel {
        private String sectionTitle;
        
        public DecorativeSectionPanel(String sectionTitle) {
            this.sectionTitle = sectionTitle;
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            int width = getWidth();
            int height = getHeight();

            Color[] themeColors = getThemeForSection(sectionTitle);
            Color primaryColor = themeColors[0];
            Color secondaryColor = themeColors[1];
            Color accentColor = themeColors[2];

            GradientPaint gradient = new GradientPaint(
                0, 0, primaryColor,
                width, height, secondaryColor
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, width, height);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            drawSectionPattern(g2d, width, height, accentColor);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
            drawSectionObjects(g2d, width, height, sectionTitle, accentColor);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        
        private Color[] getThemeForSection(String title) {
            String lowerTitle = title.toLowerCase();
            
            if (lowerTitle.contains("overview")) {

                return new Color[]{
                    new Color(235, 245, 255), // Light sky blue
                    new Color(245, 250, 255), // Very light blue
                    new Color(100, 180, 255)  // Sky blue accent
                };
            } else if (lowerTitle.contains("attraction")) {

                return new Color[]{
                    new Color(255, 248, 240), // Warm cream
                    new Color(255, 240, 230), // Peach
                    new Color(255, 165, 80)   // Orange accent
                };
            } else if (lowerTitle.contains("activit")) {

                return new Color[]{
                    new Color(240, 255, 245), // Mint green
                    new Color(245, 255, 250), // Light green
                    new Color(60, 200, 140)   // Green accent
                };
            } else if (lowerTitle.contains("din")) {

                return new Color[]{
                    new Color(255, 245, 250), // Light pink
                    new Color(255, 240, 245), // Soft pink
                    new Color(255, 120, 150)  // Rose accent
                };
            } else if (lowerTitle.contains("shop")) {

                return new Color[]{
                    new Color(250, 245, 255), // Lavender
                    new Color(245, 240, 255), // Light purple
                    new Color(180, 140, 220)  // Purple accent
                };
            }

            return new Color[]{
                new Color(240, 245, 255),
                new Color(245, 250, 255),
                new Color(150, 180, 220)
            };
        }
        
        private void drawSectionPattern(Graphics2D g2d, int width, int height, Color accentColor) {
            g2d.setColor(accentColor);
            g2d.setStroke(new BasicStroke(1.5f));

            for (int i = 0; i < width; i += 60) {
                g2d.drawLine(i, 0, i, height);
            }
            for (int i = 0; i < height; i += 60) {
                g2d.drawLine(0, i, width, i);
            }
        }
        
        private void drawSectionObjects(Graphics2D g2d, int width, int height, String sectionTitle, Color accentColor) {
            String lowerTitle = sectionTitle.toLowerCase();
            g2d.setColor(accentColor);
            
            if (lowerTitle.contains("overview")) {

                drawCompass(g2d, (int)(width * 0.9), (int)(height * 0.1), 80);
                drawMapPin(g2d, (int)(width * 0.85), (int)(height * 0.85), 60);
            } else if (lowerTitle.contains("attraction")) {

                drawCamera(g2d, (int)(width * 0.9), (int)(height * 0.15), 70);
                drawStar(g2d, (int)(width * 0.88), (int)(height * 0.8), 50);
            } else if (lowerTitle.contains("activit")) {

                drawBike(g2d, (int)(width * 0.87), (int)(height * 0.12), 70);
                drawHikingBoot(g2d, (int)(width * 0.92), (int)(height * 0.75), 50);
            } else if (lowerTitle.contains("din")) {

                drawForkKnife(g2d, (int)(width * 0.9), (int)(height * 0.1), 70);
                drawPlate(g2d, (int)(width * 0.85), (int)(height * 0.82), 60);
            } else if (lowerTitle.contains("shop")) {

                drawShoppingBag(g2d, (int)(width * 0.88), (int)(height * 0.15), 70);
                drawPriceTag(g2d, (int)(width * 0.92), (int)(height * 0.8), 55);
            }
        }
        
        private void drawCompass(Graphics2D g2d, int x, int y, int size) {
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x - size/2, y - size/2, size, size);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawLine(x, y - size/2, x, y - size/2 + 10);
            g2d.drawLine(x, y + size/2, x, y + size/2 - 10);
            g2d.drawLine(x + size/2, y, x + size/2 - 10, y);
            g2d.drawLine(x - size/2, y, x - size/2 + 10, y);
            g2d.fillOval(x - 4, y - 4, 8, 8);
        }
        
        private void drawMapPin(Graphics2D g2d, int x, int y, int size) {

            g2d.fillOval(x - size/4, y - size/2, size/2, size/2);

            int[] xPoints = {x, x - size/3, x + size/3};
            int[] yPoints = {y, y + size/2, y + size/2};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }
        
        private void drawCamera(Graphics2D g2d, int x, int y, int size) {
            g2d.fillRoundRect(x - size/2, y - size/3, size, size * 2/3, 8, 8);
            g2d.fillOval(x - size/4, y - size/6, size/2, size/2);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(x - size/3, y - size/2, size * 2/3, size/4);
        }
        
        private void drawStar(Graphics2D g2d, int x, int y, int size) {
            int[] xPoints = new int[10];
            int[] yPoints = new int[10];
            int outerRadius = size / 2;
            int innerRadius = size / 4;
            
            for (int i = 0; i < 10; i++) {
                double angle = Math.PI * i / 5;
                int radius = (i % 2 == 0) ? outerRadius : innerRadius;
                xPoints[i] = x + (int)(radius * Math.cos(angle - Math.PI / 2));
                yPoints[i] = y + (int)(radius * Math.sin(angle - Math.PI / 2));
            }
            g2d.fillPolygon(xPoints, yPoints, 10);
        }
        
        private void drawBike(Graphics2D g2d, int x, int y, int size) {
            int scale = size / 60;

            g2d.drawOval(x - 25 * scale, y, 20 * scale, 20 * scale);
            g2d.drawOval(x + 5 * scale, y, 20 * scale, 20 * scale);

            g2d.setStroke(new BasicStroke(3 * scale));
            g2d.drawLine(x - 15 * scale, y + 10 * scale, x - 5 * scale, y);
            g2d.drawLine(x - 5 * scale, y, x + 15 * scale, y);
            g2d.drawLine(x + 15 * scale, y, x + 15 * scale, y + 10 * scale);
        }
        
        private void drawHikingBoot(Graphics2D g2d, int x, int y, int size) {
            int scale = size / 40;
            g2d.fillRoundRect(x - 15 * scale, y - 10 * scale, 30 * scale, 20 * scale, 5, 5);
            g2d.setStroke(new BasicStroke(2 * scale));
            g2d.drawArc(x - 5 * scale, y - 15 * scale, 10 * scale, 10 * scale, 180, 180);
        }
        
        private void drawForkKnife(Graphics2D g2d, int x, int y, int size) {
            int scale = size / 50;

            g2d.setStroke(new BasicStroke(3 * scale));
            g2d.drawLine(x - 10 * scale, y - 20 * scale, x - 10 * scale, y + 20 * scale);
            g2d.drawLine(x - 15 * scale, y - 20 * scale, x - 5 * scale, y - 20 * scale);
            g2d.drawLine(x - 12 * scale, y - 15 * scale, x - 8 * scale, y - 15 * scale);

            g2d.drawLine(x + 10 * scale, y - 20 * scale, x + 10 * scale, y + 20 * scale);
            g2d.fillOval(x + 5 * scale, y - 22 * scale, 10 * scale, 8 * scale);
        }
        
        private void drawPlate(Graphics2D g2d, int x, int y, int size) {
            g2d.drawOval(x - size/2, y - size/2, size, size);
            g2d.drawOval(x - size/3, y - size/3, size * 2/3, size * 2/3);
        }
        
        private void drawShoppingBag(Graphics2D g2d, int x, int y, int size) {
            int scale = size / 50;
            g2d.fillRoundRect(x - 20 * scale, y - 25 * scale, 40 * scale, 50 * scale, 5, 5);
            g2d.setStroke(new BasicStroke(4 * scale));
            g2d.drawArc(x - 15 * scale, y - 30 * scale, 30 * scale, 15 * scale, 0, 180);
            g2d.fillOval(x - 8 * scale, y - 8 * scale, 16 * scale, 16 * scale);
        }
        
        private void drawPriceTag(Graphics2D g2d, int x, int y, int size) {
            int scale = size / 40;
            g2d.fillRoundRect(x - 15 * scale, y - 20 * scale, 30 * scale, 25 * scale, 3, 3);
            g2d.setStroke(new BasicStroke(2 * scale));
            g2d.drawLine(x, y + 5 * scale, x, y + 15 * scale);
            g2d.fillOval(x - 3 * scale, y + 15 * scale, 6 * scale, 6 * scale);
        }
    }
}


