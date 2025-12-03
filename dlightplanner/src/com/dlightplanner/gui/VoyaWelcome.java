package com.dlightplanner.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VoyaWelcome extends JFrame {

    private AnimatedBackgroundPanel backgroundPanel;
    private Timer animationTimer;
    private boolean continueVisible = false;

    private String leftPart = "Welcome to ";
    private String rightPart = "Voya";
    private int leftCharsTyped = 0; // Characters typed for "Welcome to "
    private int rightCharsTyped = 0; // Characters typed for "Voya"
    private double[] leftCharAlphas; // Individual character alpha for smooth fade
    private double[] rightCharAlphas;
    private boolean leftPartComplete = false;
    private boolean titleAnimationComplete = false;
    private double subtitleAlpha = 0.0; // 0.0 to 1.0 for subtitle fade
    private long lastCharTime = 0;
    private long subtitleFadeStartTime = 0;
    private static final long CHAR_DELAY = 100; // Milliseconds between each character (smooth typewriter)

    private float gradientProgress = 0.0f;
    private float auroraTime = 0.0f;
    private float waveTime = 0.0f;

    private Plane plane1, plane2;

    public VoyaWelcome() {
        setTitle("Welcome to Voya");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true); // Allow resizing/minimizing
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Default to maximized
        setLayout(new BorderLayout());
        setUndecorated(false);

        addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                if ((e.getNewState() & JFrame.ICONIFIED) != 0) {
                    setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            }
        });

        leftCharAlphas = new double[leftPart.length()];
        rightCharAlphas = new double[rightPart.length()];
        for (int i = 0; i < leftCharAlphas.length; i++) leftCharAlphas[i] = 0.0;
        for (int i = 0; i < rightCharAlphas.length; i++) rightCharAlphas[i] = 0.0;

        backgroundPanel = new AnimatedBackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout());



        MouseAdapter clickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getButton() == MouseEvent.BUTTON1 && continueVisible) {
                    proceedToNext();
                }
            }
        };
        backgroundPanel.addMouseListener(clickListener);

        addMouseListener(clickListener);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (continueVisible && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE)) {
                    proceedToNext();
                }
            }
        });
        setFocusable(true);

        add(backgroundPanel, BorderLayout.CENTER);

        startAnimations();
        setVisible(true);
        requestFocus();
    }

    private void startAnimations() {
        lastCharTime = System.currentTimeMillis();

        animationTimer = new Timer(16, e -> {
            long currentTime = System.currentTimeMillis();

            if (!titleAnimationComplete) {

                if (!leftPartComplete) {
                    if (currentTime - lastCharTime >= CHAR_DELAY) {
                        if (leftCharsTyped < leftPart.length()) {
                            leftCharsTyped++;
                            lastCharTime = currentTime;
                        } else {
                            leftPartComplete = true;

                            if (rightCharsTyped == 0) {
                                rightCharsTyped = 1;
                            }
                            lastCharTime = currentTime;
                        }
                    }

                    for (int i = 0; i < leftCharsTyped; i++) {
                        if (leftCharAlphas[i] < 1.0) {
                            leftCharAlphas[i] = Math.min(1.0, leftCharAlphas[i] + 0.2);
                        }
                    }
                } else {

                    if (currentTime - lastCharTime >= CHAR_DELAY) {
                        if (rightCharsTyped < rightPart.length()) {
                            rightCharsTyped++;
                            lastCharTime = currentTime;

                            if (rightCharsTyped == rightPart.length()) {
                                for (int i = 0; i < rightCharAlphas.length; i++) {
                                    rightCharAlphas[i] = 1.0;
                                }
                            }
                        } else {
                            titleAnimationComplete = true;
                            subtitleFadeStartTime = currentTime;

                            for (int i = 0; i < rightCharAlphas.length; i++) {
                                rightCharAlphas[i] = 1.0;
                            }
                        }
                    }

                    for (int i = 0; i < rightCharsTyped; i++) {
                        if (rightCharAlphas[i] < 1.0) {

                            double increment = (i == rightCharsTyped - 1) ? 0.3 : 0.2;
                            rightCharAlphas[i] = Math.min(1.0, rightCharAlphas[i] + increment);
                        } else {

                            rightCharAlphas[i] = 1.0;
                        }
                    }
                }
            }

            if (titleAnimationComplete && subtitleAlpha < 1.0 && continueVisible == false) {
                long elapsed = currentTime - subtitleFadeStartTime;
                double duration = 1500.0; // 1.5 seconds
                
                if (elapsed < duration) {
                    double t = elapsed / duration;
                    subtitleAlpha = easeInOutQuad(t);
                } else {
                    subtitleAlpha = 1.0;
                    continueVisible = true;
                }
            }

            if (continueVisible && subtitleAlpha < 1.0) {
                subtitleAlpha = 1.0;
            }

            auroraTime += 0.01f;
            waveTime += 0.008f;
            gradientProgress += 0.003f;
            if (gradientProgress >= 1.0f) {
                gradientProgress = 0.0f;
            }

            if (plane1 != null && plane2 != null) {
                int width = backgroundPanel.getWidth() > 0 ? backgroundPanel.getWidth() : 1200;
                int height = backgroundPanel.getHeight() > 0 ? backgroundPanel.getHeight() : 800;
                plane1.update(width, height);
                plane2.update(width, height);
            }
            
            backgroundPanel.repaint();
        });
        
        animationTimer.start();
    }

    private double easeInOutQuad(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    private void proceedToNext() {

        if (!continueVisible) {
            return;
        }

        continueVisible = false;

        if (animationTimer != null) {
            animationTimer.stop();
        }

        Timer fadeOutTimer = new Timer(16, e -> {
            subtitleAlpha = Math.max(0.0, subtitleAlpha - 0.08);
            backgroundPanel.repaint();
            
            if (subtitleAlpha <= 0.0) {
                ((Timer) e.getSource()).stop();

                SwingUtilities.invokeLater(() -> {
                    try {
            new QuizPage();
            dispose();
                    } catch (Exception ex) {
                        ex.printStackTrace();

                        dispose();
                        JOptionPane.showMessageDialog(null, "Error opening Quiz Page", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        });
        fadeOutTimer.start();
    }

    private class AnimatedBackgroundPanel extends JPanel {
        private List<FloatingParticle> particles;
        private List<AuroraWave> auroraWaves;
        private Random random;

        public AnimatedBackgroundPanel() {
            random = new Random();
            particles = new ArrayList<>();
            auroraWaves = new ArrayList<>();
        }
        
        private void initializeParticlesAndWaves() {
            int width = getWidth() > 0 ? getWidth() : 1200;
            int height = getHeight() > 0 ? getHeight() : 800;

            particles.clear();
            auroraWaves.clear();

            for (int i = 0; i < 40; i++) {
                particles.add(new FloatingParticle(
                    random.nextInt(width),
                    random.nextInt(height),
                    random.nextFloat() * 0.4f + 0.1f,
                    random.nextFloat() * 0.03f + 0.01f
                ));
            }

            for (int i = 0; i < 3; i++) {
                auroraWaves.add(new AuroraWave(
                    random.nextFloat() * width,
                    random.nextFloat() * height * 0.5f + height * 0.2f,
                    random.nextFloat() * 200f + 100f,
                    random.nextFloat() * 0.02f + 0.01f,
                    i * 0.3f
                ));
            }
        }
        
        @Override
        public void addNotify() {
            super.addNotify();

            initializeParticlesAndWaves();
            initializePlanes();
        }
        
        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);

            if (width > 0 && height > 0) {
                initializeParticlesAndWaves();
                initializePlanes();
            }
        }
        
        private void initializePlanes() {
            int width = getWidth() > 0 ? getWidth() : 1200;
            int height = getHeight() > 0 ? getHeight() : 800;

            if (plane1 == null || plane2 == null) {

                plane1 = new Plane(0, height, width, 0, 0.003f); // bottom-left to top-right
                plane2 = new Plane(width, height, 0, 0, 0.003f); // bottom-right to top-left
            } else {

                plane1.updatePath(0, height, width, 0);
                plane2.updatePath(width, height, 0, 0);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int width = getWidth();
            int height = getHeight();


            GradientPaint baseGradient = new GradientPaint(
                0, 0, new Color(20, 25, 50),
                0, height, new Color(40, 30, 70)
            );
            g2d.setPaint(baseGradient);
            g2d.fillRect(0, 0, width, height);

            float colorShift = (float)(Math.sin(gradientProgress * Math.PI * 2) * 0.5 + 0.5);
            Color overlay1 = new Color(100, 150, 220, (int)(80 * colorShift));
            Color overlay2 = new Color(180, 120, 200, (int)(80 * (1 - colorShift)));
            GradientPaint colorOverlay = new GradientPaint(
                width * 0.3f, 0, overlay1,
                width * 0.7f, height, overlay2
            );
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            g2d.setPaint(colorOverlay);
            g2d.fillRect(0, 0, width, height);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            for (AuroraWave wave : auroraWaves) {
                wave.update(width, height, auroraTime);
                wave.draw(g2d, auroraTime);
            }

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            drawAnimatedWaves(g2d, width, height, waveTime);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            for (FloatingParticle particle : particles) {
                particle.update(width, height);

                for (int i = 3; i > 0; i--) {
                    float alpha = 0.1f / i;
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g2d.setColor(new Color(255, 255, 255, 255));
                    int glowSize = (int)(particle.size * 30 + i * 8);
                    g2d.fillOval((int)particle.x - glowSize/2, (int)particle.y - glowSize/2, glowSize, glowSize);
                }

                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                g2d.setColor(new Color(255, 255, 255, 255));
                int size = (int)(particle.size * 20);
                g2d.fillOval((int)particle.x - size/2, (int)particle.y - size/2, size, size);
            }

            if (plane1 != null && plane2 != null) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
                plane1.draw(g2d, width, height);
                plane2.draw(g2d, width, height);
            }
            
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));


            Font welcomeFont;
            float fontSize = 120f; // Bigger, more prominent title
            try {

                String[] fontNames = {"Great Vibes", "Dancing Script", "Brush Script MT", "Lucida Handwriting", "Monotype Corsiva"};
                Font baseFont = null;
                for (String fontName : fontNames) {
                    Font testFont = new Font(fontName, Font.PLAIN, (int)fontSize);
                    if (testFont.getFamily().equals(fontName)) {
                        baseFont = testFont;
                        break;
                    }
                }
                if (baseFont == null) {

                    baseFont = new Font("Serif", Font.ITALIC, (int)fontSize);
                }
                welcomeFont = baseFont.deriveFont(fontSize);
            } catch (Exception e) {
                welcomeFont = new Font("Serif", Font.ITALIC, (int)fontSize);
            }
            
            g2d.setFont(welcomeFont);
            FontMetrics fm = g2d.getFontMetrics();
            int textHeight = fm.getHeight();
            int centerY = height / 2 - 30;

            int leftWidth = fm.stringWidth(leftPart);
            int rightWidth = fm.stringWidth(rightPart);
            int totalWidth = leftWidth + rightWidth;
            int centerX = width / 2;
            
            int leftX = centerX - totalWidth / 2;
            int rightX = centerX - totalWidth / 2 + leftWidth;

            for (int i = 0; i < leftCharsTyped; i++) {
                String charStr = String.valueOf(leftPart.charAt(i));
                int charX = leftX + fm.stringWidth(leftPart.substring(0, i));

                float charAlpha = (float)leftCharAlphas[i];

                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, charAlpha * 0.12f));
                g2d.setColor(new Color(255, 255, 255, 255));

                g2d.setFont(welcomeFont.deriveFont(fontSize + 2));
                g2d.drawString(charStr, charX, centerY);

                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, charAlpha));
                g2d.setFont(welcomeFont);
                g2d.setColor(new Color(255, 255, 255, 255));
                g2d.drawString(charStr, charX, centerY);
            }

            if (leftPartComplete) {
                for (int i = 0; i < rightCharsTyped; i++) {
                    String charStr = String.valueOf(rightPart.charAt(i));
                    int charX = rightX + fm.stringWidth(rightPart.substring(0, i));

                    float charAlpha = Math.max(0.95f, (float)rightCharAlphas[i]); // Ensure minimum visibility

                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, charAlpha * 0.12f));
                    g2d.setColor(new Color(255, 255, 255, 255));

                    g2d.setFont(welcomeFont.deriveFont(fontSize + 2));
                    g2d.drawString(charStr, charX, centerY);

                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, charAlpha));
                    g2d.setFont(welcomeFont);
                    g2d.setColor(new Color(255, 255, 255, 255));
                    g2d.drawString(charStr, charX, centerY);
                }

                if (!titleAnimationComplete && rightCharsTyped > 0 && rightCharsTyped < rightPart.length()) {
                    int cursorX = rightX + fm.stringWidth(rightPart.substring(0, rightCharsTyped));
                    long time = System.currentTimeMillis();
                    if ((time / 500) % 2 == 0) {
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                        g2d.setColor(new Color(255, 255, 255, 240));
                        g2d.fillRect(cursorX, centerY - textHeight + 10, 3, textHeight - 20);
                    }
                }
            }

            if (subtitleAlpha > 0.0) {

                float stableAlpha = (float)Math.min(1.0, Math.max(0.0, subtitleAlpha));
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, stableAlpha * 0.6f)); // Low opacity
                
                String continueText = "Click to continue";
                Font continueFont = new Font("SansSerif", Font.PLAIN, 16);
                g2d.setFont(continueFont);
                g2d.setColor(new Color(255, 255, 255, 200));
                
                FontMetrics continueFm = g2d.getFontMetrics();
                int continueWidth = continueFm.stringWidth(continueText);
                int continueX = (width - continueWidth) / 2;
                int continueY = centerY + textHeight + 60;
                
                g2d.drawString(continueText, continueX, continueY);
            }

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        private void drawAnimatedWaves(Graphics2D g2d, int width, int height, float time) {
            Path2D.Float wavePath = new Path2D.Float();
            int waveHeight = height / 3;
            int baseY = height - waveHeight;
            
            wavePath.moveTo(0, height);
            for (int x = 0; x <= width; x += 5) {
                float y = baseY + (float)(Math.sin((x * 0.01) + (time * 2)) * 30) +
                          (float)(Math.sin((x * 0.02) + (time * 1.5)) * 15);
                wavePath.lineTo(x, y);
            }
            wavePath.lineTo(width, height);
            wavePath.closePath();

            GradientPaint waveGradient = new GradientPaint(
                0, baseY, new Color(100, 150, 220, 100),
                0, height, new Color(180, 120, 200, 150)
            );
            g2d.setPaint(waveGradient);
            g2d.fill(wavePath);
        }
    }

    private static class FloatingParticle {
        float x, y;
        float size;
        float speedX, speedY;
        float driftX, driftY;
        float phase;

        public FloatingParticle(float x, float y, float size, float drift) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.driftX = drift * (Math.random() > 0.5 ? 1 : -1);
            this.driftY = drift * (Math.random() > 0.5 ? 1 : -1);
            this.speedX = driftX;
            this.speedY = driftY;
            this.phase = (float)(Math.random() * Math.PI * 2);
        }

        public void update(int width, int height) {

            phase += 0.02f;
            x += speedX * 0.3f + (float)(Math.sin(phase) * 0.5);
            y += speedY * 0.3f + (float)(Math.cos(phase * 0.7) * 0.5);

            if (x < 0) x = width;
            if (x > width) x = 0;
            if (y < 0) y = height;
            if (y > height) y = 0;

            if (width > 0 && height > 0) {
                if (x < 0) x = 0;
                if (x > width) x = width;
                if (y < 0) y = 0;
                if (y > height) y = height;
            }
        }
    }

    private static class AuroraWave {
        float baseX, baseY;
        float amplitude;
        float speed;
        float phase;
        Color color1, color2;

        public AuroraWave(float x, float y, float amp, float spd, float ph) {
            this.baseX = x;
            this.baseY = y;
            this.amplitude = amp;
            this.speed = spd;
            this.phase = ph;

            this.color1 = new Color(100, 150, 220, 120);
            this.color2 = new Color(180, 120, 200, 120);
        }

        public void update(int width, int height, float time) {
            baseX += speed;
            if (baseX > width + 200) baseX = -200;
        }

        public void draw(Graphics2D g2d, float time) {
            Path2D.Float path = new Path2D.Float();
            float startX = baseX - 200;
            float endX = baseX + 200;
            int segments = 50;
            
            path.moveTo(startX, baseY);
            for (int i = 0; i <= segments; i++) {
                float x = startX + (endX - startX) * (i / (float)segments);
                float wave = (float)(Math.sin((x * 0.01) + (phase + time)) * amplitude);
                float y = baseY + wave;
                path.lineTo(x, y);
            }

            GradientPaint waveGradient = new GradientPaint(
                baseX - 200, baseY, color1,
                baseX + 200, baseY, color2
            );
            g2d.setPaint(waveGradient);
            g2d.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(path);
        }
    }

    private static class Plane {
        float startX, startY;
        float endX, endY;
        float progress;
        float speed;
        float x, y;
        float angle;
        boolean active;
        
        public Plane(float startX, float startY, float endX, float endY, float speed) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.speed = speed;
            this.progress = 0.0f;
            this.x = startX;
            this.y = startY;
            this.active = true;

            this.angle = (float)Math.atan2(endY - startY, endX - startX);
        }
        
        public void updatePath(float newStartX, float newStartY, float newEndX, float newEndY) {

            float oldProgress = this.progress;
            this.startX = newStartX;
            this.startY = newStartY;
            this.endX = newEndX;
            this.endY = newEndY;

            this.angle = (float)Math.atan2(endY - startY, endX - startX);

            this.x = startX + (endX - startX) * oldProgress;
            this.y = startY + (endY - startY) * oldProgress;
        }
        
        public void update(int width, int height) {
            if (!active) return;
            
            progress += speed;
            if (progress >= 1.0f) {
                progress = 0.0f; // Reset and loop
                x = startX;
                y = startY;
            }

            x = startX + (endX - startX) * progress;
            y = startY + (endY - startY) * progress;
        }
        
        public void draw(Graphics2D g2d, int width, int height) {
            if (!active) return;

            AffineTransform oldTransform = g2d.getTransform();

            g2d.translate(x, y);
            g2d.rotate(angle);

            int planeSize = 40;
            Path2D.Float planePath = new Path2D.Float();

            planePath.moveTo(planeSize, 0); // Nose
            planePath.lineTo(-planeSize/2, -planeSize/3); // Top wing
            planePath.lineTo(-planeSize/4, 0); // Back center
            planePath.lineTo(-planeSize/2, planeSize/3); // Bottom wing
            planePath.closePath();

            GradientPaint planeGradient = new GradientPaint(
                -planeSize/2, 0, new Color(255, 255, 255, 220),
                planeSize, 0, new Color(200, 220, 255, 180)
            );
            g2d.setPaint(planeGradient);
            g2d.fill(planePath);

            g2d.setColor(new Color(150, 180, 255, 200));
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.draw(planePath);

            g2d.setColor(new Color(100, 150, 220, 200));
            g2d.fillOval(-planeSize/3, -planeSize/6, planeSize/4, planeSize/3);

            g2d.setTransform(oldTransform);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new VoyaWelcome();
        });
    }
}

