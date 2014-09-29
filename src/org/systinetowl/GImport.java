/*
 * Created by JFormDesigner on Fri Aug 29 17:36:30 CEST 2014
 */

package org.systinetowl;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author unknown
 */
public class GImport extends JPanel {
    public GImport() {
        initComponents();
    }

    private void importActionPerformed(ActionEvent e) {
        // TODO add your code here
    }

    private void updateActionPerformed(ActionEvent e) {
        // TODO add your code here
    }

    private void createUIComponents() {
        // TODO: add custom component creation code here
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - Dmitry Vasyunin
        DefaultComponentFactory compFactory = DefaultComponentFactory.getInstance();
        label1 = new JLabel();
        formattedTextField1 = new JFormattedTextField();
        label2 = new JLabel();
        textField1 = new JTextField();
        label3 = compFactory.createLabel("Username");
        textField2 = new JTextField();
        button1 = new JButton();
        label4 = new JLabel();
        passwordField1 = new JPasswordField();
        button2 = new JButton();
        scrollPane1 = new JScrollPane();
        textArea1 = new JTextArea();

        //======== this ========

        // JFormDesigner evaluation mark
        setBorder(new javax.swing.border.CompoundBorder(
            new javax.swing.border.TitledBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0),
                "JFormDesigner Evaluation", javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.BOTTOM, new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
                java.awt.Color.red), getBorder())); addPropertyChangeListener(new java.beans.PropertyChangeListener(){public void propertyChange(java.beans.PropertyChangeEvent e){if("border".equals(e.getPropertyName()))throw new RuntimeException();}});

        setLayout(new FormLayout(
            "2*(default, $lcgap), right:default:grow, $lcgap, 26dlu, $lcgap, 120dlu",
            "5*(default, $lgap), 73dlu:grow"));

        //---- label1 ----
        label1.setText("Systinet URI");
        add(label1, CC.xy(5, 3));

        //---- formattedTextField1 ----
        try{
        formattedTextField1.setValue(new URL("http://systinet.local"));
        } catch (Exception e) {}
        add(formattedTextField1, CC.xywh(7, 3, 3, 1, CC.FILL, CC.DEFAULT));

        //---- label2 ----
        label2.setText("OWL file");
        add(label2, CC.xy(5, 5));
        add(textField1, CC.xywh(7, 5, 3, 1, CC.FILL, CC.DEFAULT));
        add(label3, CC.xy(3, 7));
        add(textField2, CC.xywh(5, 7, 3, 1));

        //---- button1 ----
        button1.setText("Import");
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importActionPerformed(e);
            }
        });
        add(button1, CC.xy(9, 7, CC.RIGHT, CC.DEFAULT));

        //---- label4 ----
        label4.setText("Password");
        add(label4, CC.xy(3, 9));
        add(passwordField1, CC.xywh(5, 9, 3, 1));

        //---- button2 ----
        button2.setText("Update");
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateActionPerformed(e);
            }
        });
        add(button2, CC.xy(9, 9, CC.RIGHT, CC.DEFAULT));

        //======== scrollPane1 ========
        {
            scrollPane1.setViewportView(textArea1);
        }
        add(scrollPane1, CC.xywh(2, 11, 8, 1, CC.FILL, CC.FILL));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - Dmitry Vasyunin
    private JLabel label1;
    private JFormattedTextField formattedTextField1;
    private JLabel label2;
    private JTextField textField1;
    private JLabel label3;
    private JTextField textField2;
    private JButton button1;
    private JLabel label4;
    private JPasswordField passwordField1;
    private JButton button2;
    private JScrollPane scrollPane1;
    private JTextArea textArea1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
