/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcpapplication;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 *
 * @author giorg
 */
public class TcpApplication implements Runnable {

    public final static int ERROR = 0;
    public final static int DISCONNECTED = 1;
    public final static int DISCONNECTING = 2;
    public final static int CONNECTING = 3;
    public final static int CONNECTED = 4;

    public static TcpApplication tcpobj = new TcpApplication();

    public static String statusMessages[] = {
        "Δεν μπορώ να συνδεθώ", "Αποσυνδεθηκα", "Αποσυνδέομαι...", "Συνδέομαι...", "Συνδέθηκα"
    };
    //GUI elements
    public static JFrame mainFrame = null;
    public static JTextArea chatText = null;
    public static JTextField chatLine = null;
    public static JPanel statusBar = null;
    public static JLabel statusField = null;
    public static JTextField statusColor = null;
    public static JTextField ipField = null;
    public static JTextField portField = null;
    public static JRadioButton hostOption = null;
    public static JRadioButton guestOption = null;
    public static JButton connectButton = null;
    public static JButton disconnectButton = null;

    public static String hostIP = "localhost";
    public static int port = 1234;
    public static int connectionStatus = DISCONNECTED;
    public static boolean isHost = true;
    public static String statusString = statusMessages[connectionStatus];
    public static StringBuffer toAppend = new StringBuffer("");
    public static StringBuffer toSend = new StringBuffer("");
    public static String END_CHAT_SESSION = new Character((char) 0).toString();

    //TCP variables
    public static ServerSocket hostServer = null;
    public static Socket socket = null;
    public static BufferedReader in = null;
    public static PrintWriter out = null;

    public static void changeStatusNTS(int newConnectStatus, boolean noError) {
        if (newConnectStatus != ERROR) {
            connectionStatus = newConnectStatus;
        }
        if (noError) {
            statusString = statusMessages[connectionStatus];
        } else {
            statusString = statusMessages[ERROR];
        }
        tcpobj.run();
    }

    public static void changeStatusTS(int newConnectStatus, boolean noError) {
        if (newConnectStatus != ERROR) {
            connectionStatus = newConnectStatus;
        }
        if (noError) {
            statusString = statusMessages[connectionStatus];
        } else {
            statusString = statusMessages[ERROR];
        }
        SwingUtilities.invokeLater(tcpobj);
    }

    public static JPanel initOptionsPane() {
        JPanel optionsPane = new JPanel(new GridLayout(4, 1));
        JPanel pane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pane.add(new JLabel("Host IP:"));
        ipField = new JTextField(10);
        ipField.setText(hostIP);
        ipField.setEnabled(false);
        ipField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                ipField.selectAll();
                if (connectionStatus != DISCONNECTED) {
                    changeStatusNTS(ERROR, true);
                } else {
                    hostIP = ipField.getText();
                }
            }
        }
        );
        pane.add(ipField);
        optionsPane.add(pane);

        JPanel pane2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pane2.add(new JLabel("Port:"));
        portField = new JTextField(10);
        portField.setEditable(true);
        portField.setText(Integer.toString(port));
        portField.addFocusListener(new FocusAdapter() {
            public void focuLost(FocusEvent e) {
                if (connectionStatus != DISCONNECTED) {
                    changeStatusNTS(ERROR, true);
                } else {
                    int tmp;
                    try {
                        tmp = Integer.parseInt(portField.getText());
                        port = tmp;
                    } catch (NumberFormatException nfe) {
                        portField.setText(Integer.toString(port));
                        mainFrame.repaint();
                    }
                }

            }
        });
        pane2.add(portField);
        optionsPane.add(pane2);

        ActionListener radioButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (connectionStatus != DISCONNECTED) {
                    changeStatusNTS(ERROR, true);
                }
                isHost = e.getActionCommand().equals("Server");
                if (isHost) {
                    ipField.setEnabled(false);
                    ipField.setText("localhost");
                    hostIP = "localhost";
                } else {
                    ipField.setEnabled(true);
                }
            }
        };
        ButtonGroup bg = new ButtonGroup();
        hostOption = new JRadioButton("Server", true);
        hostOption.addActionListener(radioButtonListener);
        hostOption.setMnemonic(KeyEvent.VK_S);
        guestOption = new JRadioButton("Client", false);
        guestOption.setMnemonic(KeyEvent.VK_C);
        guestOption.addActionListener(radioButtonListener);
        bg.add(hostOption);
        bg.add(guestOption);
        JPanel pane3 = new JPanel(new GridLayout(1, 2));
        pane3.add(hostOption);
        pane3.add(guestOption);
        optionsPane.add(pane3);
        ActionListener buttonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("Σύνδεση")) {
                    changeStatusNTS(CONNECTING, true);
                } else {
                    changeStatusNTS(DISCONNECTING, true);
                }
            }
        };
        JPanel buttonPane = new JPanel(new GridLayout(1, 2));
        connectButton = new JButton("Σύνδεση");
        connectButton.setMnemonic(KeyEvent.VK_1);
        connectButton.addActionListener(buttonListener);
        disconnectButton = new JButton("Αποσύνδεση");
        disconnectButton.setEnabled(false);
        disconnectButton.setMnemonic(KeyEvent.VK_2);
        disconnectButton.addActionListener(buttonListener);
        buttonPane.add(connectButton);
        buttonPane.add(disconnectButton);
        optionsPane.add(buttonPane);

        return optionsPane;

    }

    public static void initGUI() {
        //Status bar
        statusField = new JLabel();
        statusField.setText(statusMessages[DISCONNECTED]);
        statusColor = new JTextField(1);
        statusColor.setBackground(Color.red);
        statusColor.setEditable(false);
        statusBar = new JPanel(new BorderLayout());
        statusBar.add(statusColor, BorderLayout.WEST);
        statusBar.add(statusField, BorderLayout.CENTER);

        //chat panel
        JPanel chatPane = new JPanel(new BorderLayout());
        chatText = new JTextArea(10, 20);
        chatText.setEditable(false);
        chatText.setLineWrap(true);
        chatText.setForeground(Color.blue);
        JScrollPane chatTextPane
                = new JScrollPane(chatText, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatLine = new JTextField();
        chatLine.setEnabled(false);
        chatLine.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String s = chatLine.getText();
                if(!s.equals(""))
                {
                    appendToChatBox(s + "\n");
                    chatLine.selectAll();
                    sendString(s);
                }
            }
        });
        chatPane.add(chatLine, BorderLayout.SOUTH);
        chatPane.add(chatTextPane, BorderLayout.CENTER);
        chatPane.setPreferredSize(new Dimension(200, 200));

        JPanel mainPane = new JPanel(new BorderLayout());
        mainPane.add(chatPane, BorderLayout.CENTER);
        mainPane.add(statusBar, BorderLayout.SOUTH);
        mainPane.add(initOptionsPane(), BorderLayout.WEST);

        //set up main frame
        mainFrame = new JFrame("TCP chat Application");
        mainFrame.setSize(mainFrame.getPreferredSize());
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setContentPane(mainPane);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    private static void appendToChatBox(String s) {
        synchronized (toAppend) {
            toAppend.append(s);
        }
    }
    private static void sendString(String s)
    {
        synchronized(toSend)
        {
            toSend.append(s + "\n");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        initGUI();
        while (true) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            switch (connectionStatus) {
                case CONNECTING:
                    try {
                        if (isHost) {
                            hostServer = new ServerSocket(port);
                            socket = hostServer.accept();
                        } else {
                            socket = new Socket(hostIP, port);
                        }
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        out = new PrintWriter(socket.getOutputStream(), true);
                        changeStatusTS(CONNECTED, true);
                    } catch (IOException io) {
                        io.printStackTrace();
                        changeStatusTS(DISCONNECTED, true);
                    }
                    break;
                case CONNECTED:
                    try {
                        //send data
                        if (toSend.length() != 0) {
                            out.print(toSend);
                            out.flush();
                            toSend.setLength(0);
                            changeStatusTS(ERROR, true);
                        }
                        //receive data
                        if (in.ready()) {
                            String s = in.readLine();
                            if (s != null && (s.length() != 0)) {
                                if (s.equals(END_CHAT_SESSION)) {
                                    changeStatusTS(DISCONNECTING, true);
                                } else {
                                    appendToChatBox(s + "\n");
                                    changeStatusTS(ERROR,true);
                                }
                            }
                        }
                    } catch (IOException io) {
                        io.printStackTrace();
                        changeStatusTS(DISCONNECTED, true);
                    }
                    break;
                case DISCONNECTING:
                    out.print(END_CHAT_SESSION);
                    out.flush();
                    changeStatusTS(DISCONNECTED,true);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void run() {
        switch (connectionStatus) {
            case CONNECTING:
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                ipField.setEnabled(false);
                portField.setEnabled(false);
                hostOption.setEnabled(false);
                guestOption.setEnabled(false);
                chatLine.setEnabled(false);
                chatLine.grabFocus();
                statusColor.setBackground(Color.orange);
                break;
            case CONNECTED:
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                ipField.setEnabled(false);
                portField.setEnabled(false);
                hostOption.setEnabled(false);
                guestOption.setEnabled(false);
                chatLine.setEnabled(true);
                statusColor.setBackground(Color.green);
                break;
            case DISCONNECTING:
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(false);
                ipField.setEnabled(false);
                portField.setEnabled(false);
                hostOption.setEnabled(false);
                guestOption.setEnabled(false);
                chatLine.setEnabled(false);
                statusColor.setBackground(Color.orange);
                break;
            case DISCONNECTED:
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                ipField.setEnabled(true);
                portField.setEnabled(true);
                hostOption.setEnabled(true);
                guestOption.setEnabled(true);
                chatLine.setEnabled(false);
                chatLine.setText("");
                chatText.setText("");
                statusColor.setBackground(Color.red);
                break;
        }
        ipField.setText(hostIP);
        portField.setText(Integer.toString(port));
        hostOption.setSelected(isHost);
        guestOption.setSelected(!isHost);
        statusField.setText(statusString);
        chatText.append(toAppend.toString());
        toAppend.setLength(0);
        
        mainFrame.repaint();
    }

}
