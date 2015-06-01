package org.systinetowl;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.TimerTask;

/**
 * User: dvasunin
 * Date: 27.08.2014
 * Time: 18:54
 * To change this template use File | Settings | File Templates.
 */
public class Gui {
    private JFormattedTextField formattedTextFieldURI;
    private JTextField textFieldFileName;
    private JTextArea console;
    private JButton processButton;
    private boolean isAlreadyOneClick;
    private JPanel main;
    private JTextField textFieldUserName;
    private JPasswordField passwordField;
    private JButton updateButton;


    public class CustomOutputStream extends OutputStream {
        private JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(final int b) throws IOException {
            // redirects data to the text area

            textArea.append(String.valueOf((char) b));
            // scrolls the text area to the end of data
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }

    public Gui() {
        textFieldFileName.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (isAlreadyOneClick) {
                    JFileChooser chooser = textFieldFileName.getText().isEmpty() ? new JFileChooser() : new JFileChooser(new File(textFieldFileName.getText()).getParent());
                    FileNameExtensionFilter filter = new FileNameExtensionFilter(
                            "OWL files", "owl");
                    chooser.setFileFilter(filter);
                    int returnVal = chooser.showOpenDialog(main);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        textFieldFileName.setText(chooser.getSelectedFile().getAbsolutePath());
                    }
                    isAlreadyOneClick = false;
                } else {
                    isAlreadyOneClick = true;
                    java.util.Timer t = new java.util.Timer("doubleclickTimer", false);
                    t.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            isAlreadyOneClick = false;
                        }
                    }, 500);
                }
            }
        });
        processButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final Main m = new Main(formattedTextFieldURI.getText(), textFieldUserName.getText(), passwordField.getText());
                    m.console = new PrintStream(new CustomOutputStream(console));
                    final File file = new File(textFieldFileName.getText());
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        processButton.setEnabled(false);
                                        updateButton.setEnabled(false);
                                    }
                                });
                                m.importOWL();
                                m.saveToFile(file);
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        processButton.setEnabled(true);
                                        updateButton.setEnabled(true);
                                    }
                                });
                            } catch (FileNotFoundException e1) {
                                e1.printStackTrace();
                            } catch (OWLOntologyStorageException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }).start();
                } catch (URISyntaxException e1) {
                    e1.printStackTrace();
                } catch (OWLOntologyCreationException e1) {
                    e1.printStackTrace();
                }
            }
        });
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final File file = new File(textFieldFileName.getText());
                    final Main m = new Main(formattedTextFieldURI.getText(), textFieldUserName.getText(), passwordField.getText(), file);
                    m.console = new PrintStream(new CustomOutputStream(console));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        processButton.setEnabled(false);
                                        updateButton.setEnabled(false);
                                    }
                                });
                                m.importOWL();
                                m.saveToFile(file);
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        processButton.setEnabled(true);
                                        updateButton.setEnabled(true);
                                    }
                                });
                            } catch (FileNotFoundException e1) {
                                e1.printStackTrace();
                            } catch (OWLOntologyStorageException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }).start();
                } catch (URISyntaxException e1) {
                    e1.printStackTrace();
                } catch (OWLOntologyCreationException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    private void createUIComponents() {
        try {
            formattedTextFieldURI = new JFormattedTextField(new URL("http://systinet.local:8080/soa"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Gui");
        System.out.println(frame);
        frame.setContentPane(new Gui().main);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
