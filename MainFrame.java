package ui;

import model.User;
import ui.auth.LoginPanel;
import ui.user.UserDashboard;
import ui.admin.AdminDashboard;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    public static final String CARD_LOGIN = "LOGIN";
    public static final String CARD_USER  = "USER";
    public static final String CARD_ADMIN = "ADMIN";

    private static MainFrame instance;
    public static MainFrame getInstance() {
        if (instance == null) instance = new MainFrame();
        return instance;
    }

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     cardPanel  = new JPanel(cardLayout);
    private LoginPanel     loginPanel;
    private UserDashboard  userDashboard;
    private AdminDashboard adminDashboard;
    private User currentUser;

    private MainFrame() {
        super("NewWay — Intelligent Food Delivery");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 820);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);
        loginPanel = new LoginPanel();
        cardPanel.add(loginPanel, CARD_LOGIN);
        add(cardPanel);
        cardLayout.show(cardPanel, CARD_LOGIN);
        setVisible(true);
    }

    public void onLoginSuccess(User user) {
        this.currentUser = user;
        if (user.isAdmin()) {
            if (adminDashboard == null) { adminDashboard = new AdminDashboard(); cardPanel.add(adminDashboard, CARD_ADMIN); }
            adminDashboard.refresh(); navigateTo(CARD_ADMIN);
        } else {
            if (userDashboard == null) { userDashboard = new UserDashboard(user); cardPanel.add(userDashboard, CARD_USER); }
            userDashboard.setUser(user); userDashboard.refresh(); navigateTo(CARD_USER);
        }
    }

    public void navigateTo(String card) { SwingUtilities.invokeLater(() -> cardLayout.show(cardPanel, card)); }

    public void logout() {
        currentUser = null; userDashboard = null; adminDashboard = null;
        cardPanel.removeAll();
        loginPanel = new LoginPanel();
        cardPanel.add(loginPanel, CARD_LOGIN);
        cardLayout.show(cardPanel, CARD_LOGIN);
        cardPanel.revalidate(); cardPanel.repaint();
    }

    public User getCurrentUser() { return currentUser; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::getInstance);
    }
}
