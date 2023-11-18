import javax.swing.*;
import javax.swing.text.MaskFormatter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

// 添加Binome对话框类
public class BinomeAddDialog extends JFrame {
    private Connection connection;
    private JFrame parentFrame;
    private DefaultTableModel tableModel;
    private int projectNumber = -1;

    // 构造函数
    public BinomeAddDialog(Connection connection, JFrame parentFrame, DefaultTableModel tableModel, int projectNumber) {
        this.connection = connection;
        this.parentFrame = parentFrame;
        this.tableModel = tableModel;
        this.projectNumber = projectNumber;

        setTitle("Ajouter Binôme");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(new Dimension(400, 250));

        JPanel mainPanel = new JPanel(new GridLayout(8, 2));

        JComboBox<String> student1ComboBox = new JComboBox<>(getStudentOptions());
        JComboBox<String> student2ComboBox = new JComboBox<>(getStudentOptions());

        JTextField rapportField = new JTextField(10);
        JTextField soutenance1Field = new JTextField(10);
        JTextField soutenance2Field = new JTextField(10);

        MaskFormatter dateMask = null;
        try {
            dateMask = new MaskFormatter("####-##-##"); // 日期格式的掩码 (YYYY-MM-DD)
            dateMask.setPlaceholderCharacter('_'); // 替换破折号的字符
        } catch (ParseException e) {
            e.printStackTrace();
        }
        JFormattedTextField dateRemiseEffectiveField = new JFormattedTextField(dateMask);
        dateRemiseEffectiveField.setColumns(10);

        mainPanel.add(new JLabel("Étudiant 1 :"));
        mainPanel.add(student1ComboBox);
        mainPanel.add(new JLabel("Étudiant 2 :"));
        mainPanel.add(student2ComboBox);
        mainPanel.add(new JLabel("Note Rapport :"));
        mainPanel.add(rapportField);
        mainPanel.add(new JLabel("Note Soutenance Étudiant 1 :"));
        mainPanel.add(soutenance1Field);
        mainPanel.add(new JLabel("Note Soutenance Étudiant 2 :"));
        mainPanel.add(soutenance2Field);
        mainPanel.add(new JLabel("Date de Remise Effective (YYYY-MM-DD) :"));
        mainPanel.add(dateRemiseEffectiveField);

        JButton validerButton = new JButton("Valider");
        JButton effacerButton = new JButton("Effacer");
        mainPanel.add(validerButton);
        mainPanel.add(effacerButton);

        add(mainPanel);

        ImageIcon icon = new ImageIcon("C:\\Users\\MATEBOOK14\\Desktop\\Gestion_projets\\logo_D.jpg"); // 替换为实际图标文件的路径
        setIconImage(icon.getImage()); // 设置 JFrame 的图标

        // 添加事件处理器到"Valider"按钮
        validerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedStudent1 = (String) student1ComboBox.getSelectedItem();
                String selectedStudent2 = (String) student2ComboBox.getSelectedItem();
                String rapport = rapportField.getText();
                String soutenance1 = soutenance1Field.getText();
                String soutenance2 = soutenance2Field.getText();
                String dateRemiseEffective = dateRemiseEffectiveField.getText().replace('_', '-');

                if (!selectedStudent1.isEmpty() && (!selectedStudent2.isEmpty() || selectedStudent2 == null) && !rapport.isEmpty() && !soutenance1.isEmpty() && !soutenance2.isEmpty() && !dateRemiseEffective.isEmpty() && isValidDateFormat(dateRemiseEffective) && isValidGrade(rapport) && isValidGrade(soutenance1) && isValidGrade(soutenance2)) {
                    int projectId = projectNumber;
                    int student1Id = getStudentId(selectedStudent1);
                    int student2Id = getStudentId(selectedStudent2);

                    // 使用变量构建表名
                    String tableName = "Project_" + projectId;

                    try {
                        String insertSql = "INSERT INTO " + tableName +
                                " (projet_numero, etudiant1_numero, etudiant2_numero, note_rapport, note_soutenance_etu1, note_soutenance_etu2, date_remise_effective) VALUES (?, ?, ?, ?, ?, ?, ?)";
                        PreparedStatement preparedStatement = connection.prepareStatement(insertSql);
                        preparedStatement.setInt(1, projectId);
                        preparedStatement.setInt(2, student1Id);
                        preparedStatement.setInt(3, student2Id);
                        preparedStatement.setString(4, rapport);
                        preparedStatement.setString(5, soutenance1);
                        preparedStatement.setString(6, soutenance2);
                        preparedStatement.setString(7, dateRemiseEffective);
                        preparedStatement.executeUpdate();

                        int binomeId = getLastInsertedBinomeId();

                        // 在第一个学生的表中插入一条记录
                        String insertNoteSql1 = "INSERT INTO Notes (etudiant_id, projet_id, note_rapport, note_soutenance, note_finale) VALUES (?, ?, ?, ?, ?)";
                        PreparedStatement preparedStatementNote1 = connection.prepareStatement(insertNoteSql1);
                        preparedStatementNote1.setInt(1, student1Id);
                        preparedStatementNote1.setInt(2, projectId);
                        preparedStatementNote1.setString(3, rapport);
                        preparedStatementNote1.setString(4, soutenance1);
                        preparedStatementNote1.setString(5, "0");
                        preparedStatementNote1.executeUpdate();

                        // 在第二个学生的表中插入一条记录
                        String insertNoteSql2 = "INSERT INTO Notes (etudiant_id, projet_id, note_rapport, note_soutenance, note_finale) VALUES (?, ?, ?, ?, ?)";
                        PreparedStatement preparedStatementNote2 = connection.prepareStatement(insertNoteSql2);
                        preparedStatementNote2.setInt(1, student2Id);
                        preparedStatementNote2.setInt(2, projectId);
                        preparedStatementNote2.setString(3, rapport);
                        preparedStatementNote2.setString(4, soutenance2);
                        preparedStatementNote2.setString(5, "0");
                        preparedStatementNote2.executeUpdate();

                        tableModel.addRow(new Object[]{binomeId, projectNumber, selectedStudent1, selectedStudent2, rapport, soutenance1, soutenance2, dateRemiseEffective});

                        // 清空表单字段
                        student1ComboBox.setSelectedIndex(0);
                        student2ComboBox.setSelectedIndex(0);
                        rapportField.setText("");
                        soutenance1Field.setText("");
                        soutenance2Field.setText("");
                        dateRemiseEffectiveField.setText("");

                        // 显示确认窗口
                        JOptionPane.showMessageDialog(parentFrame, "Binôme ajouté avec succès!", "Confirmation", JOptionPane.INFORMATION_MESSAGE);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(parentFrame, "Veuillez remplir tous les champs correctement et assurez-vous que les notes sont entre 0 et 20.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 添加事件处理器到"Effacer"按钮
        effacerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                student1ComboBox.setSelectedIndex(0);
                student2ComboBox.setSelectedIndex(0);
                rapportField.setText("");
                soutenance1Field.setText("");
                soutenance2Field.setText("");
                dateRemiseEffectiveField.setText("");
            }
        });

        setLocationRelativeTo(parentFrame);
        setVisible(true);
    }

    // 获取学生ID的方法
    private int getStudentId(String studentName) {
        int studentId = -1;
        try {
            String query = "SELECT numero FROM Etudiants WHERE nom = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, studentName);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                studentId = resultSet.getInt("numero");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return studentId;
    }

    // 验证日期格式的方法
    private boolean isValidDateFormat(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);

        try {
            Date parsedDate = sdf.parse(date);
            return parsedDate != null;
        } catch (ParseException e) {
            return false;
        }
    }

    // 获取最后插入的Binome ID的方法
    private int getLastInsertedBinomeId() {
        int binomeId = -1;
        try {
            String query = "SELECT MAX(numero) as max_binome_id FROM Binomes";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                binomeId = resultSet.getInt("max_binome_id");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return binomeId;
    }

    // 获取学生选项的方法
    private DefaultComboBoxModel<String> getStudentOptions() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        try {
            String query = "SELECT nom FROM Etudiants";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String studentName = resultSet.getString("nom");
                model.addElement(studentName);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return model;
    }

    // 验证成绩的方法
    private boolean isValidGrade(String grade) {
        try {
            double value = Double.parseDouble(grade);
            return value >= 0 && value <= 20;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
