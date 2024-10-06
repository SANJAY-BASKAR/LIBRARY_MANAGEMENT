import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LibraryManagementSystem {
    private JFrame frame;
    private JPanel contentPanel;
    private Connection conn;
    private JTextArea viewBookListArea;
    private JTextArea removeBookListArea;

    public static void main(String[] args) {
        new LibraryManagementSystem();
    }

    public LibraryManagementSystem() {
        // Initialize Database Connection
        connectToDatabase();

        // Set up JFrame
        frame = new JFrame("Library Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // Welcome Page
        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome to the Library Management System", JLabel.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);

        JButton enterButton = new JButton("Enter");
        enterButton.addActionListener(e -> {
            frame.getContentPane().removeAll();
            frame.repaint();
            showHomePage();
        });
        welcomePanel.add(enterButton, BorderLayout.SOUTH);

        frame.add(welcomePanel);
        frame.setVisible(true);
    }

    private void connectToDatabase() {
        try {
            String url = "jdbc:postgresql://localhost:5432/library_management";
            String user = "postgres";
            String password = "sanjay2005";
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to the database successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database connection failed!");
        }
    }

    private void showHomePage() {
        JPanel homePanel = new JPanel(new BorderLayout());

        // Side Menu Panel
        JPanel sideMenu = new JPanel(new GridLayout(4, 1, 0, 10));
        JButton addBookButton = new JButton("Add Book");
        JButton viewBookButton = new JButton("View Books");
        JButton removeBookButton = new JButton("Remove Book");
        JButton changeStatusButton = new JButton("Mark Book as Occupied");

        sideMenu.add(addBookButton);
        sideMenu.add(viewBookButton);
        sideMenu.add(removeBookButton);
        sideMenu.add(changeStatusButton);

        // Content Panel with CardLayout
        contentPanel = new JPanel(new CardLayout());

        // Panels for each feature
        JPanel addBookPanel = createAddBookPanel();
        JPanel viewBookPanel = createViewBookPanel();
        JPanel removeBookPanel = createRemoveBookPanel();
        JPanel markBookOccupiedPanel = createMarkBookOccupiedPanel();

        contentPanel.add(addBookPanel, "Add Book");
        contentPanel.add(viewBookPanel, "View Books");
        contentPanel.add(removeBookPanel, "Remove Book");
        contentPanel.add(markBookOccupiedPanel, "Mark Book Occupied");

        // Button Actions
        addBookButton.addActionListener(e -> switchPanel("Add Book"));
        viewBookButton.addActionListener(e -> {
            updateBookList(viewBookListArea);
            switchPanel("View Books");
        });
        removeBookButton.addActionListener(e -> {
            updateBookList(removeBookListArea);
            switchPanel("Remove Book");
        });
        changeStatusButton.addActionListener(e -> switchPanel("Mark Book Occupied"));

        homePanel.add(sideMenu, BorderLayout.WEST);
        homePanel.add(contentPanel, BorderLayout.CENTER);

        frame.add(homePanel);
        frame.validate();
    }

    private JPanel createAddBookPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        JLabel titleLabel = new JLabel("Title:");
        JTextField titleField = new JTextField();
        JLabel authorLabel = new JLabel("Author:");
        JTextField authorField = new JTextField();
        JButton addButton = new JButton("Add Book");

        addButton.addActionListener(e -> {
            String title = titleField.getText();
            String author = authorField.getText();
            if (!title.isEmpty() && !author.isEmpty()) {
                addBook(title, author);
                titleField.setText("");
                authorField.setText("");
            } else {
                JOptionPane.showMessageDialog(frame, "Please enter book title and author.");
            }
        });

        panel.add(titleLabel);
        panel.add(titleField);
        panel.add(authorLabel);
        panel.add(authorField);
        panel.add(new JLabel());
        panel.add(addButton);

        return panel;
    }

    private JPanel createViewBookPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        viewBookListArea = new JTextArea();
        viewBookListArea.setEditable(false);
        panel.add(new JScrollPane(viewBookListArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRemoveBookPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        removeBookListArea = new JTextArea();
        removeBookListArea.setEditable(false);
        panel.add(new JScrollPane(removeBookListArea), BorderLayout.CENTER);

        JButton removeButton = new JButton("Remove Book ID");
        removeButton.addActionListener(e -> {
            String bookIdStr = JOptionPane.showInputDialog(frame, "Enter Book ID to remove:");
            if (bookIdStr != null) {
                try {
                    int bookId = Integer.parseInt(bookIdStr);
                    removeBook(bookId);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid Book ID");
                }
            }
        });

        panel.add(removeButton, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createMarkBookOccupiedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextField bookIdField = new JTextField();
        // Set the preferred size for the input area (width: 200, height: 30)
        bookIdField.setPreferredSize(new Dimension(200, 30));
        JButton markOccupiedButton = new JButton("Mark as Occupied");

        panel.add(new JLabel("Enter Book ID to Mark as Occupied:"), BorderLayout.NORTH);
        panel.add(bookIdField, BorderLayout.CENTER);
        panel.add(markOccupiedButton, BorderLayout.SOUTH);

        markOccupiedButton.addActionListener(e -> {
            try {
                int bookId = Integer.parseInt(bookIdField.getText());
                changeBookStatus(bookId, "Occupied");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid Book ID");
            }
        });

        return panel;
    }


    private void switchPanel(String panelName) {
        CardLayout cl = (CardLayout) contentPanel.getLayout();
        cl.show(contentPanel, panelName);
    }

    private void addBook(String title, String author) {
        String sql = "INSERT INTO books (title, author) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, author);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(frame, "Book added successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateBookList(JTextArea bookListArea) {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT * FROM books";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String author = rs.getString("author");
                String status = rs.getString("status");
                sb.append("ID: ").append(id).append(", Title: ").append(title)
                        .append(", Author: ").append(author)
                        .append(", Status: ").append(status).append("\n");
            }
            bookListArea.setText(sb.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void removeBook(int id) {
        String sql = "DELETE FROM books WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(frame, "Book removed successfully!");
            } else {
                JOptionPane.showMessageDialog(frame, "Book ID not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void changeBookStatus(int bookId, String status) {
        String sql = "UPDATE books SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, bookId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(frame, "Book status updated to 'Occupied' successfully!");
            } else {
                JOptionPane.showMessageDialog(frame, "Book ID not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
