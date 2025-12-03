package com.dlightplanner.gui;

import com.dlightplanner.models.City;
import com.dlightplanner.services.CityService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class QuizPage extends JFrame {

    private final List<QuizQuestion> questions;
    private final Map<Integer, List<Integer>> selectedOptionIndexes = new LinkedHashMap<>();
    private final JPanel optionsPanel = new JPanel();
    private final JLabel questionLabel = new JLabel();
    private final JButton backButton = new JButton("Back");
    private final JButton nextButton = new JButton("Next");
    private final JLabel helperLabel = new JLabel();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final CityService cityService = new CityService();
    private final AnimatedBackgroundPanel backgroundPanel = new AnimatedBackgroundPanel();

    private int currentIndex = 0;
    private float gradientProgress = 0.0f;

    private static final Map<String, List<String>> TAG_CITY_MAP = Map.ofEntries(
            Map.entry("beach", List.of("Goa", "Andaman & Nicobar", "Kerala")),
            Map.entry("mountain", List.of("Manali", "Darjeeling", "Shimla")),
            Map.entry("urban", List.of("Mumbai", "Delhi", "Bangalore", "Pune")),
            Map.entry("luxury", List.of("Udaipur", "Kerala", "Goa")),
            Map.entry("spiritual", List.of("Rishikesh", "Assam", "Kerala")),
            Map.entry("nature", List.of("Kerala", "Assam", "Darjeeling")),
            Map.entry("adventure", List.of("Manali", "Andaman & Nicobar", "Rishikesh")),
            Map.entry("culture", List.of("Udaipur", "Kolkata", "Mysore", "Delhi")),
            Map.entry("foodie", List.of("Hyderabad", "Mumbai", "Kolkata")),
            Map.entry("nightlife", List.of("Goa", "Mumbai", "Bangalore"))
    );

    public QuizPage() {
        setTitle("Voya | Travel Persona Quiz");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

        cityService.loadCitiesFromJson("resources/cities.json");
        questions = buildQuestions();

        backgroundPanel.setLayout(new BorderLayout());
        
        JPanel quizPanel = buildQuizPanel();
        backgroundPanel.add(quizPanel, BorderLayout.CENTER);
        contentPanel.add(backgroundPanel, "QUIZ");
        add(contentPanel);

        startBackgroundAnimation();

        showQuestion();
        setVisible(true);
    }

    private void startBackgroundAnimation() {
        javax.swing.Timer animationTimer = new javax.swing.Timer(16, e -> {
            gradientProgress += 0.002f;
            if (gradientProgress >= 1.0f) {
                gradientProgress = 0.0f;
            }
            backgroundPanel.repaint();
        });
        animationTimer.start();
    }

    private JPanel buildQuizPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false); // Transparent to show background
        wrapper.setBorder(new EmptyBorder(40, 60, 40, 60));

        questionLabel.setFont(new Font("Georgia", Font.BOLD, 26));
        questionLabel.setForeground(new Color(40, 50, 70));
        questionLabel.setHorizontalAlignment(SwingConstants.LEFT);

        helperLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        helperLabel.setForeground(new Color(100, 110, 130));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(questionLabel, BorderLayout.NORTH);
        header.add(helperLabel, BorderLayout.SOUTH);
        header.setBorder(new EmptyBorder(0, 0, 30, 0));

        optionsPanel.setLayout(new GridLayout(0, 1, 15, 15));
        optionsPanel.setOpaque(false);

        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        navigation.setOpaque(false);
        
        styleButton(backButton);
        styleButton(nextButton);
        
        backButton.addActionListener(e -> {
            if (currentIndex > 0) {
                currentIndex--;
                showQuestion();
            }
        });

        nextButton.addActionListener(e -> advanceFromQuestion());

        navigation.add(backButton);
        navigation.add(nextButton);

        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(optionsPanel, BorderLayout.CENTER);
        wrapper.add(navigation, BorderLayout.SOUTH);

        return wrapper;
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        button.setPreferredSize(new Dimension(120, 45));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setBackground(new Color(100, 150, 220));
        button.setForeground(Color.WHITE);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(80, 130, 200));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(100, 150, 220));
            }
        });
    }

    private void showQuestion() {
        QuizQuestion question = questions.get(currentIndex);
        questionLabel.setText("Q" + (currentIndex + 1) + "/" + questions.size() + ": " + question.prompt());
        helperLabel.setText(question.multiSelect() ? "Select every option that resonates with you." : "Tap an option and we'll move ahead.");

        optionsPanel.removeAll();
        if (question.multiSelect()) {
            renderMultiSelect(question);
        } else {
            renderSingleSelect(question);
        }
        optionsPanel.revalidate();
        optionsPanel.repaint();

        backButton.setEnabled(currentIndex > 0);
        nextButton.setText(currentIndex == questions.size() - 1 ? "Finish" : "Next");
        nextButton.setEnabled(hasSelection(currentIndex));

        if (backButton.isEnabled()) {
            backButton.setBackground(new Color(100, 150, 220));
        } else {
            backButton.setBackground(new Color(180, 180, 180));
        }
    }

    private void renderSingleSelect(QuizQuestion question) {
        ButtonGroup group = new ButtonGroup();
        List<Integer> savedAnswers = selectedOptionIndexes.getOrDefault(currentIndex, new ArrayList<>());
        for (int i = 0; i < question.options().size(); i++) {
            QuizOption option = question.options().get(i);
            OptionCard card = new OptionCard(option.label(), false);
            card.setSelected(!savedAnswers.isEmpty() && savedAnswers.get(0) == i);
            
            int index = i;
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {

                    for (Component comp : optionsPanel.getComponents()) {
                        if (comp instanceof OptionCard) {
                            ((OptionCard) comp).setSelected(false);
                        }
                    }
                    card.setSelected(true);
                    selectedOptionIndexes.put(currentIndex, List.of(index));
                    nextButton.setEnabled(true);
                    nextButton.setBackground(new Color(100, 150, 220));
                    autoAdvance();
                }
            });
            
            group.add(card.getRadioButton());
            optionsPanel.add(card);
        }
    }

    private void renderMultiSelect(QuizQuestion question) {
        List<Integer> savedAnswers = selectedOptionIndexes.getOrDefault(currentIndex, new ArrayList<>());
        for (int i = 0; i < question.options().size(); i++) {
            QuizOption option = question.options().get(i);
            OptionCard card = new OptionCard(option.label(), true);
            card.setSelected(savedAnswers.contains(i));
            
            int index = i;
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    boolean newState = !card.isSelected();
                    card.setSelected(newState);
                    
                    List<Integer> selections = new ArrayList<>(selectedOptionIndexes.getOrDefault(currentIndex, new ArrayList<>()));
                    if (newState) {
                        if (!selections.contains(index)) {
                            selections.add(index);
                        }
                    } else {
                        selections.remove(Integer.valueOf(index));
                    }
                    if (selections.isEmpty()) {
                        selectedOptionIndexes.remove(currentIndex);
                    } else {
                        selectedOptionIndexes.put(currentIndex, selections);
                    }
                    nextButton.setEnabled(!selections.isEmpty());
                    if (nextButton.isEnabled()) {
                        nextButton.setBackground(new Color(100, 150, 220));
                    } else {
                        nextButton.setBackground(new Color(180, 180, 180));
                    }
                }
            });
            
            optionsPanel.add(card);
        }
    }

    private void autoAdvance() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    advanceFromQuestion();
                    timer.cancel();
                });
            }
        }, 200);
    }

    private void advanceFromQuestion() {
        if (currentIndex < questions.size() - 1) {
            currentIndex++;
            showQuestion();
        } else {
            showResults();
        }
    }

    private boolean hasSelection(int index) {
        return selectedOptionIndexes.containsKey(index) && !selectedOptionIndexes.get(index).isEmpty();
    }

    private void showResults() {
        List<City> recommendations = calculateRecommendations();

        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setOpaque(false);
        resultPanel.setBorder(new EmptyBorder(40, 60, 40, 60));

        JLabel title = new JLabel("Your Voya Matches", SwingConstants.CENTER);
        title.setFont(new Font("Georgia", Font.BOLD, 32));
        title.setForeground(new Color(40, 50, 70));
        resultPanel.add(title, BorderLayout.NORTH);

        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setOpaque(false);
        
        if (recommendations.isEmpty()) {
            JLabel noResults = new JLabel("No recommendations found. Please retake the quiz.", SwingConstants.CENTER);
            noResults.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            noResults.setForeground(new Color(100, 100, 100));
            noResults.setBorder(new EmptyBorder(40, 20, 40, 20));
            cardsPanel.add(noResults);
        } else {
            for (City city : recommendations) {
                if (city != null) {
                    JPanel card = buildResultCard(city);
                    cardsPanel.add(card);
                    cardsPanel.add(Box.createVerticalStrut(20));
                }
            }
        }

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        actions.setOpaque(false);
        JButton goToBooking = new JButton("Go to Booking");
        styleButton(goToBooking);
        goToBooking.addActionListener(e -> {
            new HomePage();
            dispose();
        });
        JButton retake = new JButton("Retake Quiz");
        styleButton(retake);
        retake.addActionListener(e -> {
            selectedOptionIndexes.clear();
            currentIndex = 0;
            showQuestion();
            cardLayout.show(contentPanel, "QUIZ");
        });
        actions.add(retake);
        actions.add(goToBooking);

        JScrollPane scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(scrollPane, BorderLayout.CENTER);
        center.add(actions, BorderLayout.SOUTH);

        resultPanel.add(center, BorderLayout.CENTER);

        Component[] components = contentPanel.getComponents();
        for (Component comp : components) {
            if (comp.getName() != null && comp.getName().equals("RESULT")) {
                contentPanel.remove(comp);
            }
        }

        JPanel resultWrapper = new JPanel(new BorderLayout());
        resultWrapper.setName("RESULT");
        resultWrapper.add(resultPanel, BorderLayout.CENTER);
        resultWrapper.setOpaque(false);
        
        contentPanel.add(resultWrapper, "RESULT");
        cardLayout.show(contentPanel, "RESULT");

        SwingUtilities.invokeLater(() -> {
            resultWrapper.revalidate();
            resultWrapper.repaint();
            contentPanel.revalidate();
            contentPanel.repaint();
            scrollPane.revalidate();
            scrollPane.repaint();
        });
    }

    private JPanel buildResultCard(City city) {
        JPanel card = new JPanel(new BorderLayout(20, 0));
        card.setBackground(new Color(255, 255, 255, 240));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(20, 20, 20, 20)
        ));
        card.setOpaque(true);
        card.setPreferredSize(new Dimension(1000, 200));
        card.setMaximumSize(new Dimension(1200, Integer.MAX_VALUE));

        JPanel textContainer = new JPanel(new BorderLayout());
        textContainer.setOpaque(false);
        textContainer.setPreferredSize(new Dimension(600, 0));

        JLabel name = new JLabel(city.getName(), SwingConstants.LEFT);
        name.setFont(new Font("Segoe UI", Font.BOLD, 20));
        name.setForeground(new Color(40, 50, 70));
        name.setBorder(new EmptyBorder(0, 0, 10, 0));

        String fullOverview = city.getOverview();
        String displayText = fullOverview != null ? fullOverview : "A perfect match for your travel vibe!";
        boolean isTruncated = fullOverview != null && fullOverview.length() > 220;
        
        JTextArea blurb = new JTextArea();
        blurb.setLineWrap(true);
        blurb.setWrapStyleWord(true);
        blurb.setEditable(false);
        blurb.setOpaque(false);
        blurb.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        blurb.setForeground(new Color(60, 70, 90));
        
        if (isTruncated) {
            blurb.setText(displayText.substring(0, 217) + "...");
        } else {
            blurb.setText(displayText);
        }

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(name);
        textPanel.add(blurb);

        if (isTruncated) {
            JButton readMoreBtn = new JButton("Read more");
            readMoreBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            readMoreBtn.setForeground(new Color(100, 150, 220));
            readMoreBtn.setBorderPainted(false);
            readMoreBtn.setContentAreaFilled(false);
            readMoreBtn.setFocusPainted(false);
            readMoreBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            readMoreBtn.addActionListener(e -> showFullDescription(city.getName(), fullOverview));
            textPanel.add(Box.createVerticalStrut(5));
            textPanel.add(readMoreBtn);
        }

        textContainer.add(textPanel, BorderLayout.NORTH);

        JPanel imageContainer = new JPanel(new BorderLayout());
        imageContainer.setOpaque(false);
        imageContainer.setPreferredSize(new Dimension(280, 0));
        imageContainer.setMaximumSize(new Dimension(280, Integer.MAX_VALUE));

        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);

        try {
            ImageIcon icon = new ImageIcon("resources/images/" + city.getImagePath());
            if (icon.getIconWidth() > 0) {

                int maxWidth = 280;
                int maxHeight = 200;
                int originalWidth = icon.getIconWidth();
                int originalHeight = icon.getIconHeight();
                
                double widthRatio = (double) maxWidth / originalWidth;
                double heightRatio = (double) maxHeight / originalHeight;
                double ratio = Math.min(widthRatio, heightRatio);
                
                int scaledWidth = (int) (originalWidth * ratio);
                int scaledHeight = (int) (originalHeight * ratio);
                
                Image scaledImage = icon.getImage().getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
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

        imageLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(10, 10, 10, 10)
        ));
        imageLabel.setBackground(new Color(255, 255, 255));
        imageLabel.setOpaque(true);

        imageContainer.add(imageLabel, BorderLayout.NORTH);

        card.add(textContainer, BorderLayout.WEST);
        card.add(imageContainer, BorderLayout.EAST);

        return card;
    }
    
    private void showFullDescription(String cityName, String fullText) {
        JDialog dialog = new JDialog(this, cityName + " - Full Description", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(new EmptyBorder(20, 20, 20, 20));
        content.setBackground(new Color(255, 255, 255));
        
        JTextArea textArea = new JTextArea(fullText);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        textArea.setForeground(new Color(60, 70, 90));
        textArea.setBackground(new Color(255, 255, 255));
        textArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        
        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        closeBtn.setPreferredSize(new Dimension(100, 35));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setBackground(new Color(100, 150, 220));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dialog.dispose());

        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setBackground(new Color(80, 130, 200));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setBackground(new Color(100, 150, 220));
            }
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeBtn);
        
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(content);
        dialog.setVisible(true);
    }

    private List<City> calculateRecommendations() {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (int qIndex = 0; qIndex < questions.size(); qIndex++) {
            List<Integer> selections = selectedOptionIndexes.get(qIndex);
            if (selections == null) continue;
            QuizQuestion question = questions.get(qIndex);
            for (Integer optionIndex : selections) {
                QuizOption option = question.options().get(optionIndex);
                for (String tag : option.tags()) {
                    List<String> cities = TAG_CITY_MAP.getOrDefault(tag, List.of());
                    for (String cityName : cities) {
                        scores.merge(cityName, 1, Integer::sum);
                    }
                }
            }
        }

        Map<String, City> cityByName = cityService.getCities().stream()
                .collect(Collectors.toMap(City::getName, c -> c, (a, b) -> a));

        List<String> rankedCityNames = scores.entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Set<City> recommendations = new LinkedHashSet<>();
        for (String name : rankedCityNames) {
            City city = cityByName.get(name);
            if (city != null) {
                recommendations.add(city);
            }
            if (recommendations.size() >= 4) break;
        }

        if (recommendations.size() < 4) {
            for (City city : cityService.getCities()) {
                recommendations.add(city);
                if (recommendations.size() >= 4) break;
            }
        }

        return new ArrayList<>(recommendations);
    }

    private List<QuizQuestion> buildQuestions() {
        List<QuizQuestion> list = new ArrayList<>();

        list.add(new QuizQuestion(
                "What's your travel aura right now?",
                false,
                List.of(
                        new QuizOption("Beachy + carefree", List.of("beach", "nightlife")),
                        new QuizOption("Mountain fresh & adventurous", List.of("mountain", "adventure", "nature")),
                        new QuizOption("City buzz + culture fix", List.of("urban", "culture", "foodie")),
                        new QuizOption("Spiritual unwind + calm", List.of("spiritual", "nature"))
                )
        ));

        list.add(new QuizQuestion(
                "Pick the statement that feels true.",
                false,
                List.of(
                        new QuizOption("I chase sunrise hikes and local food trails.", List.of("adventure", "foodie")),
                        new QuizOption("I want luxe stays and curated experiences.", List.of("luxury", "urban")),
                        new QuizOption("I crave water, waves, and barefoot walks.", List.of("beach", "nature")),
                        new QuizOption("I recharge around temples, ghats, or tea gardens.", List.of("spiritual", "nature"))
                )
        ));

        list.add(new QuizQuestion(
                "Which sensory moments light you up? (Select all that apply)",
                true,
                List.of(
                        new QuizOption("Salt air and sunset parties", List.of("beach", "nightlife")),
                        new QuizOption("Pine forests and snowfall vibes", List.of("mountain", "nature")),
                        new QuizOption("Street art, cafes, and film screenings", List.of("urban", "culture")),
                        new QuizOption("Chants, bells, and river breezes", List.of("spiritual", "nature"))
                )
        ));

        list.add(new QuizQuestion(
                "How social do you want this trip to feel?",
                false,
                List.of(
                        new QuizOption("Give me buzzing nightlife and events.", List.of("nightlife", "urban")),
                        new QuizOption("Balanced: I'll mingle and also retreat.", List.of("culture", "luxury")),
                        new QuizOption("Low-key: nature, journaling, maybe yoga.", List.of("spiritual", "nature"))
                )
        ));

        list.add(new QuizQuestion(
                "Pick the experiences you MUST have. (Select all that apply)",
                true,
                List.of(
                        new QuizOption("Water sports or diving", List.of("adventure", "beach")),
                        new QuizOption("Heritage walks + palace tours", List.of("culture", "luxury")),
                        new QuizOption("Yoga, wellness, or slow mornings", List.of("spiritual", "nature")),
                        new QuizOption("Food crawls + hidden cafes", List.of("foodie", "urban"))
                )
        ));

        list.add(new QuizQuestion(
                "What kind of climate feels healing right now?",
                false,
                List.of(
                        new QuizOption("Warm sands + tropical breezes", List.of("beach", "nature")),
                        new QuizOption("Cool misty hills", List.of("mountain", "nature")),
                        new QuizOption("Pleasant city weather", List.of("urban", "culture"))
                )
        ));

        list.add(new QuizQuestion(
                "Choose the soundtrack to your trip.",
                false,
                List.of(
                        new QuizOption("Indie beach playlists + crashing waves", List.of("beach", "nightlife")),
                        new QuizOption("Folk music, temple bells, and mantra chants", List.of("spiritual", "culture")),
                        new QuizOption("Live gigs, jazz bars, and city hum", List.of("urban", "foodie"))
                )
        ));

        return list;
    }

    private class AnimatedBackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int width = getWidth();
            int height = getHeight();

            Color color1 = new Color(240, 245, 255); // Soft blue-white
            Color color2 = new Color(250, 240, 255); // Soft lavender
            Color color3 = new Color(255, 245, 250); // Soft pink

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
        }
        
        private Color interpolateColor(Color c1, Color c2, float t) {
            int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * t);
            int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
            int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t);
            return new Color(r, g, b);
        }
    }

    private class OptionCard extends JPanel {
        private final JLabel textLabel;
        private final JRadioButton radioButton;
        private final JCheckBox checkBox;
        private boolean selected = false;
        private final boolean isMultiSelect;

        public OptionCard(String text, boolean multiSelect) {
            this.isMultiSelect = multiSelect;
            setLayout(new BorderLayout());
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(20, 25, 20, 25));

            textLabel = new JLabel(text);
            textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            textLabel.setForeground(new Color(50, 60, 80));
            
            if (multiSelect) {
                checkBox = new JCheckBox();
                checkBox.setOpaque(false);
                checkBox.setFocusPainted(false);
                add(checkBox, BorderLayout.WEST);
                add(textLabel, BorderLayout.CENTER);
                radioButton = null;
            } else {
                radioButton = new JRadioButton();
                radioButton.setOpaque(false);
                radioButton.setFocusPainted(false);
                add(radioButton, BorderLayout.WEST);
                add(textLabel, BorderLayout.CENTER);
                checkBox = null;
            }

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!selected) {
                        setBackground(new Color(255, 255, 255, 180));
                        setOpaque(true);
                    }
                    repaint();
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!selected) {
                        setOpaque(false);
                    }
                    repaint();
                }
            });
        }
        
        public void setSelected(boolean selected) {
            this.selected = selected;
            if (isMultiSelect && checkBox != null) {
                checkBox.setSelected(selected);
            } else if (radioButton != null) {
                radioButton.setSelected(selected);
            }
            
            if (selected) {
                setBackground(new Color(230, 240, 255));
                setOpaque(true);
            } else {
                setOpaque(false);
            }
            repaint();
        }
        
        public boolean isSelected() {
            return selected;
        }
        
        public JRadioButton getRadioButton() {
            return radioButton;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (isOpaque()) {

                int arc = 15;
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

                if (selected) {
                    g2d.setColor(new Color(100, 150, 220, 200));
                    g2d.setStroke(new BasicStroke(2));
                } else {
                    g2d.setColor(new Color(220, 220, 220, 150));
                    g2d.setStroke(new BasicStroke(1));
                }
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
        }
    }

    private record QuizQuestion(String prompt, boolean multiSelect, List<QuizOption> options) {}

    private record QuizOption(String label, List<String> tags) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(QuizPage::new);
    }
}
