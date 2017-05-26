package utility;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;

import webserver.ServerSimulator;
import webserver.WebServer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.awt.event.MouseAdapter;



public class MessagePicker{

	
	private String settingsFile, webPath, messagesFolder, sequencesFolder, currentActionFolder;
	private Boolean settingsOn = false;
	
	private SortedListOfActions allMessagesListModel = new SortedListOfActions();
	private SortedListOfActions messagesListModel = new SortedListOfActions();
	private SortedListOfActions allSequencesListModel = new SortedListOfActions();
	private SortedListOfActions sequencesListModel = new SortedListOfActions();
	private DefaultListModel sequenceDetailListModel = new DefaultListModel();
	private String filter = "";
	private Boolean filterInsideSequences = true;
	private HashMap<String, String> sequenceDictionary = new HashMap<String, String>();
	private String waitActionStart = "   . . . Wait : "; 
	private Action currentAction = new Action();
	private Action currentDisplayedAction = new Action();
	private String actionMalformedSuffix = " *";
	
	private WebServer server = new WebServer();	
	
	/************************************
	 * SWING OBJECT DECLARATIONS
	 *************************************/
	
	/** MAIN PANELS **/
	
	private JList messagesList;
	private JList sequencesList;
	private JList sequenceDetail;
	private JSplitPane splitPane_Main;
	private JSplitPane splitPane_MessageSequence;
	private JSplitPane splitPane_Sequences;
	private JPanel panel_Main_LeftPane;
	private int splitPaneDivLocation;
	private JPanel panel_MessageFilter;
	private RSyntaxTextArea textArea_Json;
	private JTextField textField_MessageFilter;
	private JCheckBox chckbx_searchInSeqDetails;
	private JButton btnPickThisMessage;		// button to use selected message in list of messages
	private JButton btnPickThisSequence;	// button to use selected sequence in list of sequences
	private JButton btnUseThis; 		// button to use custom message
	private JButton btnSave;			// button to save custom message to file (new if not exists)
	private JButton btnNew;
	private JButton btnDelete;
	private JPanel panel_textAreaButtons;
	
	/** TOOLBAR **/
	
	private JToolBar toolBar;
	private JProgressBar progressBar_Server;
	private JButton btn_Params;
	private JPanel panel_inToolBar;
	
	/** PROGRESS BAR MENU **/
	
	private JPopupMenu popupMenu_ProgessBar;
	private JMenuItem menuItem_RestartServer;
	private JMenuItem menuItem_CurrentAction;
	
	/** POPUPS **/
	
	private JPanel panel_TextAndPopUp;
	private JPanel panel_PopUp;
	private JTextField newName;
	private JButton buttonSaveAsNew;
		
	/************************************
	 * USEFULL LISTENERS
	 *************************************/
	private ActionListener setAction = new SetAction();
	private KeyListener jsonEdited = new JsonEdited();
	private MouseListener popupClicked = new PopupClicked();
	private CaretListener nameTextFieldChangedListener = new NameTextFieldChangedListener();

	/************************************
	 * CONSTANTS
	 *************************************/
	private int dividerSize = 7;
	//private int startWindowWidth = 800;
	//private int startWindowHeight = 600;
	private Color background = new Color(245,245,249);
	private Color textFieldBG = UIManager.getColor("TextField.background");
	private Color buttonBG = UIManager.getColor("Button.background");
	private Color currentColor = new Color(205,230,255);
	private Color warningColor = new Color(250,200,75);
	private Color errorColor = new Color(250,0,0);

	/**
	 * @wbp.parser.entryPoint
	 */
	private void createWindow()
	{	
		JFrame frame = new JFrame("Choose your message(s)");
		frame.setBackground(background);
		frame.getContentPane().setBackground(background);
		frame.getContentPane().setLayout(new GridLayout(1, 1));

		splitPane_Main = new JSplitPane();
		splitPane_Main.setBackground(background);
		frame.getContentPane().add(splitPane_Main);
		splitPane_Main.setDividerSize(dividerSize);

		panel_Main_LeftPane = new JPanel();
		panel_Main_LeftPane.setBackground(background);
		panel_Main_LeftPane.setLayout(new BorderLayout(0, 0));
		splitPane_Main.setLeftComponent(panel_Main_LeftPane);

		JPanel panel_Main_RightPane = new JPanel();
		panel_Main_RightPane.setBackground(background);
		splitPane_Main.setRightComponent(panel_Main_RightPane);
		panel_Main_RightPane.setLayout(new BorderLayout(0, 0));

		
		/************************************
		 * PANEL: FILTER
		 *************************************/
		panel_MessageFilter = new JPanel();
		panel_MessageFilter.setBackground(background);
		panel_MessageFilter.setBorder(new TitledBorder(null, "Filter", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_MessageFilter.setLayout(new BorderLayout(0, 0));
		
		textField_MessageFilter = new JTextField();
		panel_MessageFilter.add(textField_MessageFilter, BorderLayout.CENTER);
		textField_MessageFilter.setColumns(10);
		textField_MessageFilter.addCaretListener(new FilterChangedListener());
		
		chckbx_searchInSeqDetails = new JCheckBox("in details");
		chckbx_searchInSeqDetails.setSelected(true);
		chckbx_searchInSeqDetails.setToolTipText("When filtering, keep sequences which contain an action (message or function) accepted by the filter.");
		chckbx_searchInSeqDetails.addItemListener(new ChckbxFilterItemListener());
		panel_MessageFilter.add(chckbx_searchInSeqDetails, BorderLayout.EAST);
		

		panel_Main_LeftPane.add(panel_MessageFilter, BorderLayout.NORTH);
		
		splitPane_MessageSequence = new JSplitPane();
		splitPane_MessageSequence.setBackground(background);
		panel_Main_LeftPane.add(splitPane_MessageSequence, BorderLayout.CENTER);
		splitPane_MessageSequence.setDividerSize(dividerSize);
		splitPane_MessageSequence.setResizeWeight(.5);

		
		/************************************
		 * PANEL: MESSAGES
		 *************************************/
		JPanel panel_Messages = new JPanel();
		panel_Messages.setBackground(background);
		splitPane_MessageSequence.setLeftComponent(panel_Messages);
		panel_Messages.setLayout(new BorderLayout(0, 0));
	
		// List of messages
		messagesList = new JList(messagesListModel);
		messagesList.setBackground(background);
		messagesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		messagesList.setCellRenderer(new SortedListOfActionsCellRenderer());
		
		JScrollPane scrollPane_messages = new JScrollPane();
		scrollPane_messages.setViewportBorder(new TitledBorder(null, "Messages", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		scrollPane_messages.setOpaque(false);
		scrollPane_messages.setViewportView(messagesList);
		panel_Messages.add(scrollPane_messages, BorderLayout.CENTER);
		
		// Button
		btnPickThisMessage = new JButton("Use this message");
		btnPickThisMessage.setBackground(buttonBG);
		btnPickThisMessage.setVerticalAlignment(SwingConstants.BOTTOM);
		btnPickThisMessage.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		btnPickThisMessage.setEnabled(false);
		btnPickThisMessage.addActionListener(setAction);
		panel_Messages.add(btnPickThisMessage, BorderLayout.SOUTH);

		/************************************
		 * PANEL: SEQUENCES
		 *************************************/
		JPanel panel_Sequences = new JPanel();
		panel_Sequences.setBackground(background);
		splitPane_MessageSequence.setRightComponent(panel_Sequences);
		panel_Sequences.setLayout(new BorderLayout(0, 0));

		splitPane_Sequences = new JSplitPane();
		splitPane_Sequences.setBackground(background);
		splitPane_Sequences.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPane_Sequences.setDividerSize(dividerSize);
		splitPane_Sequences.setContinuousLayout(true);
		panel_Sequences.add(splitPane_Sequences, BorderLayout.CENTER);

		// List of sequences
		sequencesList = new JList(sequencesListModel);
		sequencesList.setBackground(background);
		sequencesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		sequencesList.setCellRenderer(new SortedListOfActionsCellRenderer());
		
		JScrollPane scrollPane_sequences = new JScrollPane();
		scrollPane_sequences.setViewportBorder(new TitledBorder(null, "Sequences", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		scrollPane_sequences.setOpaque(false);
		scrollPane_sequences.setViewportView(sequencesList);
		splitPane_Sequences.setTopComponent(scrollPane_sequences);
		
		// Detailed sequence as list
		sequenceDetail = new JList(sequenceDetailListModel);
		sequenceDetail.setBackground(background);
		sequenceDetail.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		sequenceDetail.setCellRenderer(new SequenceDetailCellRenderer());
		
		JScrollPane scrollPane_sequenceDetails = new JScrollPane();
		scrollPane_sequenceDetails.setViewportBorder(new TitledBorder(null, "Sequence detail", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		scrollPane_sequenceDetails.setOpaque(false);
		scrollPane_sequenceDetails.setViewportView(sequenceDetail);
		splitPane_Sequences.setBottomComponent(scrollPane_sequenceDetails);
		
		// Button
		btnPickThisSequence = new JButton("Use this sequence");
		btnPickThisSequence.setBackground(buttonBG);
		btnPickThisSequence.setVerticalAlignment(SwingConstants.BOTTOM);
		btnPickThisSequence.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		btnPickThisSequence.addActionListener(setAction);
		btnPickThisSequence.setEnabled(false);
		panel_Sequences.add(btnPickThisSequence, BorderLayout.SOUTH);

		/************************************
		 * PANEL: MESSAGE EDITOR
		 *************************************/
	    
	    panel_TextAndPopUp = new JPanel();
	    panel_Main_RightPane.add(panel_TextAndPopUp, BorderLayout.CENTER);
	    panel_TextAndPopUp.setLayout(new BorderLayout(0, 0));
	    textArea_Json = new RSyntaxTextArea(20, 60);
	    textArea_Json.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
	    textArea_Json.setCodeFoldingEnabled(true);
	    RTextScrollPane scrollPane_Json = new RTextScrollPane(textArea_Json);
	    panel_TextAndPopUp.add(scrollPane_Json);
	    textArea_Json.addKeyListener(jsonEdited);
	    
	    panel_PopUp = new JPanel();
	    panel_PopUp.setVisible(false);
	    panel_TextAndPopUp.add(panel_PopUp, BorderLayout.SOUTH);
	    
	    JPanel panel_BottomBar = new JPanel();
	    panel_Main_RightPane.add(panel_BottomBar, BorderLayout.SOUTH);
	    panel_BottomBar.setLayout(new BorderLayout(0, 0));
	    panel_BottomBar.setMaximumSize(new Dimension(60, 10));
	    
	    panel_textAreaButtons = new JPanel();
	    panel_BottomBar.add(panel_textAreaButtons, BorderLayout.CENTER);
	    GridBagLayout gbl_panel_textAreaButtons = new GridBagLayout();
	    gbl_panel_textAreaButtons.rowHeights = new int[]{23, 0, 0};
	    gbl_panel_textAreaButtons.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0};
	    gbl_panel_textAreaButtons.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
	    panel_textAreaButtons.setLayout(gbl_panel_textAreaButtons);
	    
	    btnUseThis = new JButton("Use this message");
	    btnUseThis.addActionListener(setAction);
	    GridBagConstraints gbc_btnUseThis = new GridBagConstraints();
	    gbc_btnUseThis.weightx = 12.0;
	    gbc_btnUseThis.anchor = GridBagConstraints.LINE_START;
	    gbc_btnUseThis.fill = GridBagConstraints.HORIZONTAL;
	    gbc_btnUseThis.insets = new Insets(0, 0, 0, 5);
	    gbc_btnUseThis.gridx = 0;
	    gbc_btnUseThis.gridy = 0;
	    panel_textAreaButtons.add(btnUseThis, gbc_btnUseThis);
	    
	    btnNew = new JButton("New");
	    btnNew.addActionListener(new NewAction());
	    GridBagConstraints gbc_btnNew = new GridBagConstraints();
	    gbc_btnNew.insets = new Insets(0, 0, 0, 5);
	    gbc_btnNew.weightx = 1.0;
	    gbc_btnNew.fill = GridBagConstraints.BOTH;
	    gbc_btnNew.gridx = 1;
	    gbc_btnNew.gridy = 0;
	    panel_textAreaButtons.add(btnNew, gbc_btnNew);
	    
	    btnSave = new JButton("Save");
	    btnSave.addActionListener(new SaveAction());
	    GridBagConstraints gbc_btnSave = new GridBagConstraints();
	    gbc_btnSave.weightx = 1.0;
	    gbc_btnSave.fill = GridBagConstraints.BOTH;
	    gbc_btnSave.insets = new Insets(0, 0, 0, 5);
	    gbc_btnSave.gridx = 2;
	    gbc_btnSave.gridy = 0;
	    panel_textAreaButtons.add(btnSave, gbc_btnSave);
    
	    btnDelete = new JButton("Delete");
	    btnDelete.addActionListener(new DeleteAction());
	    GridBagConstraints gbc_btnDelete = new GridBagConstraints();
	    gbc_btnDelete.insets = new Insets(0, 0, 0, 0);
	    gbc_btnDelete.gridx = 3;
	    gbc_btnDelete.gridy = 0;
	    panel_textAreaButtons.add(btnDelete, gbc_btnDelete);
	    
	    /************************************
		 * TOOLBAR (PART OF MESSAGE EDITOR PANEL)
		 *************************************/	    
	    toolBar = new JToolBar();
	    panel_BottomBar.add(toolBar, BorderLayout.EAST);
	    
	    /** NORMAL TOOLBAR **/
	    
	    panel_inToolBar = new JPanel();
	    panel_inToolBar.setPreferredSize(new Dimension(80, 22));
	    panel_inToolBar.setMaximumSize(new Dimension(100, 22));
	    panel_inToolBar.setLayout(new GridLayout(0, 2, 4, 0));
	    toolBar.add(panel_inToolBar);

	    progressBar_Server = new JProgressBar();
	    progressBar_Server.setMaximum(10);
	    panel_inToolBar.add(progressBar_Server);
	    
	    popupMenu_ProgessBar = new JPopupMenu();
	    popupMenu_ProgessBar.setLabel("");
	    menuItem_RestartServer = new JMenuItem("Restart server");
	    menuItem_RestartServer.addMouseListener(new ProgressBarMenu_RestartServer());
	    popupMenu_ProgessBar.add(menuItem_RestartServer);
	    menuItem_CurrentAction = new JMenuItem("Current action");
	    menuItem_CurrentAction.addMouseListener(new ProgressBarMenu_ShowCurrentAction());
	    popupMenu_ProgessBar.add(menuItem_CurrentAction);
	    addPopup(progressBar_Server, popupMenu_ProgessBar);
	    
	    btn_Params = new JButton(new ImageIcon(MessagePicker.class.getResource("/ressources/icons/1481130257_Working_Tools_2.png")));		
	    btn_Params.addActionListener(new SettingsClicked());
	    panel_inToolBar.add(btn_Params);
	    
	    /************************************
		 * END OF SWING COMPONENT DEFINITIONS
		 *************************************/
	    
		messagesList.addListSelectionListener(new MessageSelectionListener());
		sequencesList.addListSelectionListener(new SequenceSelectionListener());
		sequenceDetail.addListSelectionListener(new SequenceDetailsSelectionListener());

		//Display the window. 
		//frame.setMinimumSize(new Dimension(startWindowWidth,startWindowHeight));
		//frame.setMinimumSize(new Dimension(startWindowWidth/2,startWindowHeight/2));
		//frame.setSize(new Dimension(startWindowWidth,startWindowHeight));
		//frame.setLocationRelativeTo(null);
		frame.pack(); 
		frame.setVisible(true); 
		
		setDefaults();
		
		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public void CreateWindow(String settingsPath, String aWebPath, String messagesPath, String sequencesPath, String currentActionPath)
	{
		SetPaths(settingsPath, aWebPath, messagesPath, sequencesPath, currentActionPath);
		createWindow();
		fillWindow();      
	}
	
	public void SetPaths(String settingsPath, String aWebPath, String messagesPath, String sequencesPath, String currentActionPath)
	{
		settingsFile = settingsPath;
		webPath = aWebPath;
		messagesFolder = messagesPath;
		sequencesFolder = sequencesPath;
		currentActionFolder = currentActionPath;
	}
	
	public void Warning(String message)
	{
		progressBar_Server.setBackground(warningColor);
		infoPopUp("WARNING! " + message, null, true);
	}
	
	public void Error(String message)
	{
		progressBar_Server.setBackground(errorColor);
		infoPopUp("ERROR! " + message, null, true);
	}
	
	/************************************
	 * HELPERS
	 *************************************/
	
	/** SERVER **/
	
	private void startWebServer()
	{
		if (server.startService(webPath, messagesFolder, sequencesFolder, currentActionFolder))
		{
			progressBar_Server.setIndeterminate(true);
			Logger.Info("Current action: " + currentAction.Type + " - " + currentAction.Name);
		}
		else
		{
			progressBar_Server.setIndeterminate(false);
			progressBar_Server.setValue(0);
		}
	}
	
	private void stopWebServer()
	{
		server.stopWebServer();
		
		progressBar_Server.setIndeterminate(false);
		progressBar_Server.setValue(0);
	}
	
	private void restartWebServer()
	{
		stopWebServer();
		startWebServer();
	}
	
	/** STARTUP AND RESET **/
	
	private void setDefaults() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
            	splitPane_Main.setDividerLocation(.5);
        		splitPane_MessageSequence.setDividerLocation(.5);
        		splitPane_Sequences.setDividerLocation(.6);
            }
        });
    }
	
	private void fillWindow() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {      	
            	resetApp(true);
            }
            
        });
    }
	
	private void resetApp(Boolean resetCurrentAction)
	{
		ServerSimulator.ResetSettings();
		fillMessagesList(messagesFolder);
        fillSequencesList(sequencesFolder);
        setFilterInDetails(filterInsideSequences);
        
        Boolean currentActiondDefined = false;
        
        if (resetCurrentAction)
        {
        	currentActiondDefined = loadCurrentActionFromDisk(currentActionFolder, true);
        }
        
        String text =  "You need to define a path to the web app. Click on the settings icon on the bottom right.";
        if (webPath.isEmpty())
        {
        	stopWebServer();
        	warningPopUp(text, null, false);
        }
        else if (currentActiondDefined)
        {
        	progressBar_Server.setBackground(buttonBG);
        	restartWebServer();
        }
	}

	/** LEFT PANEL **/
	
	private void fillMessagesList(String messagesPath)
	{
		File[] files = new File(messagesPath).listFiles();
		//If this pathname does not denote a directory, then listFiles() returns null. 

		messagesListModel.clear();
		allMessagesListModel.clear();
		
		if (files != null)
		{
			for (File file : files) {
				String filename = file.getName();
				if (file.isFile() && filename.endsWith(".json")) {
					messagesListModel.add(filename.substring(0, filename.lastIndexOf(".")), ActionType.Message);
					allMessagesListModel.add(filename.substring(0, filename.lastIndexOf(".")), ActionType.Message);
				}
			}
		}
	}
	
	private void fillSequencesList(String sequencesPath)
	{
		File[] files = new File(sequencesPath).listFiles();
		//If this pathname does not denote a directory, then listFiles() returns null. 

		sequencesListModel.clear();
		allSequencesListModel.clear();
		
		if (files != null)
		{
			for (File file : files) {
				String filename = file.getName();
				if (file.isFile() && filename.endsWith(".json")) {
					Boolean sequenceOK = addToSequencesDictionary(file);
					String sequenceName = "";
					sequenceName = filename.substring(0, filename.lastIndexOf("."));
					
					if (sequenceOK)
					{
						sequencesListModel.add(sequenceName, ActionType.Sequence);
						allSequencesListModel.add(sequenceName, ActionType.Sequence);
					}
					else
					{
						sequencesListModel.add(sequenceName, ActionType.Sequence, false);
						allSequencesListModel.add(sequenceName, ActionType.Sequence, false);
					}
				}
			}
		}
	}
	
	private Boolean addToSequencesDictionary(File file)
	{
		String filepath = file.getPath();
		String filename = file.getName().substring(0, file.getName().lastIndexOf("."));
		String actions = "";
		Boolean messagesDefined = true;
		
		JsonParser parser = new JsonParser();
		JsonElement jsonFile = null;
		JsonArray jsonSequence = null;
		try 
		{
			jsonFile = parser.parse(new FileReader(filepath));

		} catch (IOException ioe) {
			ioe.printStackTrace();
			UIManager.getLookAndFeel().provideErrorFeedback(textArea_Json);
			return false;
		} catch (Exception e)
		{
			e.printStackTrace();
			Logger.Error("Malformed sequence in file "+ filepath);
			return false;
		}

		if (jsonFile != null)
		{
			jsonSequence = jsonFile.getAsJsonObject().getAsJsonArray("Sequence");
			if (jsonSequence != null)
			{
				for (int i = 0; i < jsonSequence.size(); i++)
				{
					Boolean thisActionDefined = checkExistence(jsonSequence.get(i).getAsJsonObject());
					if (thisActionDefined)
						actions += jsonSequence.get(i).getAsJsonObject().get("Value").getAsString() + "&;&";
					else
						actions += jsonSequence.get(i).getAsJsonObject().get("Value").getAsString() + actionMalformedSuffix + "&;&";
					
					messagesDefined = messagesDefined && thisActionDefined;
				}
			}
			else
			{
				Logger.Error("Sequence not found in file "+ filepath);
				return false;
			}
		}
		else
		{
			Logger.Error("Some problem occured with file "+ filepath);
			return false;
		}
		
		sequenceDictionary.put(filename, actions);
		return messagesDefined;
		
	}
	
	private Boolean checkExistence(JsonObject jsonAction)
	{
		String type = jsonAction.get("Type").getAsString();
		String name = "";
		
		if (type.equalsIgnoreCase("message"))
		{
			name = jsonAction.get("Value").getAsString();
			
			Boolean found = false;
			for (int i = 0; i < allMessagesListModel.getSize(); i++)
			{
				found = found || ((String)allMessagesListModel.getElementAt(i)).equalsIgnoreCase(name);
			}
			
			return found;
		}
		
		return true;
	}
	
	private void setFilterInDetails(Boolean filterDetails)
	{
		filterInsideSequences = filterDetails;
		chckbx_searchInSeqDetails.setSelected(filterDetails);
	}
	
	/** RIGHT PANEL **/
	
	private void setRightPanelUI(UIState uiState)
	{
		switch (uiState)
		{
		case Message:
			btnUseThis.setText("Use this message");
			btnUseThis.setEnabled(true);
			btnDelete.setEnabled(true);
			break;
		case Sequence:
			btnUseThis.setText("Use this sequence");
			btnUseThis.setEnabled(true);
			btnDelete.setEnabled(true);
			break;
		case CustomMessage:
			btnUseThis.setText("Use this custom message");
			btnUseThis.setEnabled(true);
			btnDelete.setEnabled(false);
			break;
		case CustomSequence:
			btnUseThis.setText("Use this custom sequence");
			btnUseThis.setEnabled(true);
			btnDelete.setEnabled(false);
			break;
		case MessageFromDetailOfSequence:
			btnUseThis.setText("");
			btnUseThis.setEnabled(false);
			btnDelete.setEnabled(false);
			break;
		default:
			break;
		}
		
		btnUseThis.setEnabled(true);
	}
	
	/** FILTER **/
	
	private void filterLists()
	{
		filter = textField_MessageFilter.getText().toLowerCase();
		
		messagesList.clearSelection();	
		sequencesList.clearSelection();
		sequenceDetail.clearSelection();
		sequenceDetailListModel.clear();
		
		messagesListModel.clear();
		for (int i = 0; i < allMessagesListModel.getSize(); i++)
		{
			if (allMessagesListModel.getElementAt(i).toString().toLowerCase().contains(filter))
			{
				messagesListModel.add(allMessagesListModel.getActionListElementAt(i));
			}
		}
		
		sequencesListModel.clear();
		for (int i = 0; i < allSequencesListModel.getSize(); i++)
		{
			String seq = allSequencesListModel.getElementAt(i).toString();
			if (seq.toLowerCase().contains(filter))
			{
				sequencesListModel.add(allSequencesListModel.getActionListElementAt(i));
			}
			else if (filterInsideSequences && sequenceDictionary.get(seq) != null && sequenceDictionary.get(seq).toLowerCase().contains(filter))
			{
				sequencesListModel.add(allSequencesListModel.getActionListElementAt(i));
			}
		}
	}
	
	private Boolean loadCurrentActionFromDisk(String currentActionPath, Boolean showInUI)
	{		
		JsonParser parser = new JsonParser();
		JsonElement jsonFile = null;
		JsonObject jsonAction = null;
		
		File current = new File(currentActionPath + "\\current.json");
		if (!current.exists())
		{
			return false;
		}
		
		try 
		{
			jsonFile = parser.parse(new FileReader(currentActionPath + "\\current.json"));

		} catch (IOException ioe) {
			ioe.printStackTrace();
			Logger.Info("Current action file corrupted or malformed. Proceed to delete current action.");
			saveToDiskAsCurrentAction(null);
			return false;
		}
		
		String type, name = "";
		Boolean found = false;
		
		if (jsonFile != null)
			jsonAction = jsonFile.getAsJsonObject();
		if (jsonAction != null)
		{
			type = jsonAction.get("Type").getAsString();
			
			if (type.equals("Message"))
			{
				name = jsonAction.get("Name").getAsString();
				currentAction.Set(ActionType.Message, name);
				found = true;
			}
			else if (type.equals("Sequence"))
			{
				name = jsonAction.get("Name").getAsString();
				currentAction.Set(ActionType.Sequence, name);
				found = true;
			}
			else if (type.equals("CustomMessage"))
			{		
				currentAction.Set(ActionType.CustomMessage);
				found = true;
			}
			else if (type.equals("CustomSequence"))
			{
				currentAction.Set(ActionType.CustomSequence);
				found = true;
			}
			
			if (showInUI)
				loadAction(currentAction);
			
			return found;
		}
		
		return false;
	}

	private Boolean saveToDiskAsCurrentAction(Action action)
	{
		Boolean saved = false;
		if (action == null || action.Type == null)
		{
			BufferedWriter current = null;
			try 
			{
				current = new BufferedWriter(new FileWriter(currentActionFolder + "\\current.json"));
				current.write("{\"Type\":\"\", \"Name\":\"\"}");
				current.close();
				File custom = new File(currentActionFolder + "\\custom.json");
				custom.delete();
				
				saved = true;
			} catch (IOException ioe) {
				ioe.printStackTrace();
				saved = false;
			} finally {
				if (current != null)
					try { current.close(); } catch (IOException ex) { ex.printStackTrace(); saved = false;}
			}
			return saved;
		}
		else
		{
			String type = "", name = "";
			Boolean customText = false;
			switch (action.Type)
			{
			case CustomMessage:
				type = "CustomMessage";
				name = "";
				customText = true; // = textArea_Json.getText();
				break;
			case CustomSequence:
				type = "CustomSequence";
				name = "";
				customText = true;
				break;
			case Message:
				type = "Message";
				name = action.Name;
				break;
			case Sequence:
				type = "Sequence";
				name = action.Name;
				break;
			default:
				type = "";
				name = "";
				break;
			
			}
			
			BufferedWriter current = null;
			BufferedWriter custom = null;
			File customFile = new File(currentActionFolder + "\\custom.json");
			try {
				current = new BufferedWriter(new FileWriter(currentActionFolder + "\\current.json"));
				current.write("{\"Type\":\"" + type + "\", \"Name\":\"" + name + "\"}");
							
				if (customText)
				{
					custom = new BufferedWriter(new FileWriter(customFile));
					custom.write(textArea_Json.getText());
				}
				else
				{
					if (customFile.exists())
						customFile.delete();
				}
				saved = true;
			} catch (IOException e) {
				e.printStackTrace();
				saved = false;
			} finally {
				try {
					if (current != null) current.close();
					if (custom != null) custom.close();
				} catch (IOException ex) { ex.printStackTrace(); }
			}	
			
			return saved;
		}
	}
	
	private void loadAction(Action action)
	{
		String fileToRead = "";
		messagesList.clearSelection();
		sequencesList.clearSelection();
		sequenceDetail.clearSelection();		
		fillMessagesList(messagesFolder);		
		fillSequencesList(sequencesFolder);	
		textArea_Json.setText("");
		
		Boolean found = false;
		
		if (action.Type == null)
		{
			currentDisplayedAction.Name = "";
			currentDisplayedAction.OriginalName = "";
					
			messagesList.clearSelection();
			btnPickThisMessage.setEnabled(false);
			sequencesList.clearSelection();
			btnPickThisSequence.setEnabled(false);
			btnUseThis.setEnabled(false);
			
			return;
		}
		else if (action.Type == ActionType.Message)
		{
			setRightPanelUI(UIState.Message);
			fileToRead = messagesFolder + "\\" + action.Name + ".json";
			messagesList.setSelectedValue(action.Name, true);
			found = true;
		}
		else if (action.Type == ActionType.Sequence)
		{
			setRightPanelUI(UIState.Sequence);
			fileToRead = sequencesFolder + "\\" + action.Name + ".json";
			sequencesList.setSelectedValue(action.Name, true);
			found = true;
		}
		else if (action.Type == ActionType.CustomMessage)
		{		
			setRightPanelUI(UIState.CustomMessage);
			fileToRead = currentActionFolder + "\\custom.json";
			found = true;
		}
		else if (action.Type == ActionType.CustomSequence)
		{
			setRightPanelUI(UIState.CustomSequence);
			fileToRead = currentActionFolder + "\\custom.json";
			found = true;
		}
		
		if (found)
		{
			BufferedReader r = null;
			try {
				r = new BufferedReader(new FileReader(fileToRead));
				textArea_Json.read(r, null);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (r != null) try { r.close(); } catch (IOException ex) { ex.printStackTrace(); }
			}
		
			currentDisplayedAction.CopyOf(action);
		}
		else
		{
			Logger.Info("Load action: Action " + action.Name + " not found in list of " + action.Type.toString() + ".");
		}
	}
	
	private void select(Action action)
	{
		if (action.Type == ActionType.Message)
		{
			setRightPanelUI(UIState.Message);
			messagesList.setSelectedValue(action.Name, true);
		}
		else if (action.Type == ActionType.Sequence)
		{
			setRightPanelUI(UIState.Sequence);
			sequencesList.setSelectedValue(action.Name, true);
			sequenceDetail.clearSelection();
		}
	}
	
	/** SETTINGS **/
	
	private void readSettingsFile()
	{	
		BufferedReader r = null;
		try {
			r = new BufferedReader(new FileReader(settingsFile));
			textArea_Json.read(r, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (r != null) try { r.close(); } catch (Exception ex) { ex.printStackTrace(); }
		}
	}
	
	private void saveSettingsFile()
	{
		try 
		{
			BufferedWriter current = new BufferedWriter(new FileWriter(settingsFile));
			current.write(textArea_Json.getText());	
			current.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/************************************
	 * POP-UPS
	 *************************************/

	/* Generic method for creating simple popup: message with buttons */
	private void infoPopUp(String message, JButton[] buttons, Boolean closeOnClick)
	{
		popUp(message, buttons, closeOnClick, null);
	}
	
	private void warningPopUp(String message, JButton[] buttons, Boolean closeOnClick)
	{
		popUp(message, buttons, closeOnClick, warningColor);
	}

	private void popUp(String message, JButton[] buttons, Boolean closeOnClick, Color bg)
	{
		closePopUp();
		
		if (!message.startsWith("<html>"))
			message = "<html><p>" + message + "</p></html>";
		
		JPanel pnl_Popup = new JPanel();
		JLabel label_Message = new JLabel(message);
		
		pnl_Popup.add(label_Message);
		if (bg != null) 
			pnl_Popup.setBackground(bg);
		
		if (buttons != null)
		{
			for (JButton button : buttons)
			{
				pnl_Popup.add(button);
			}
		}
		
		showPopUp(pnl_Popup, closeOnClick);
	}
	
	/* Show popup with given elements (inside a panel) */
	private void showPopUp(JPanel panel, Boolean closeOnClick)
	{
		panel_PopUp.setVisible(false);
		panel_PopUp.removeAll();
		panel_PopUp.add(panel);
//		panel_PopUp.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		panel_PopUp.setVisible(true);
		
		if (closeOnClick)
			panel_PopUp.addMouseListener(popupClicked);
		else 
			panel_PopUp.removeMouseListener(popupClicked);
	}
	
	private void closePopUp()
	{
		panel_PopUp.removeAll();
		panel_PopUp.setVisible(false);
		panel_PopUp.removeMouseListener(popupClicked);
	}
	
	private void newActionPopUp()
	{		
		JButton[] buttons = new JButton[2];
		buttons[0] = new JButton("Message");
		buttons[0].addActionListener(new NewMessageClicked());
		buttons[1] = new JButton("Sequence");
		buttons[1].addActionListener(new NewSequenceClicked());
		
		infoPopUp("", buttons, true);
	}
	
	private void saveActionPopUp(Action currentAction)
	{		
			
		// Find out the purpose of the save:
		// 1) Old message was modified: 					isUpdate = true
		// 2) New message from scratch: 					isUpdate = false
		// 3) Copy of existing message w/ modification:		forNewCopy = true
		Boolean isUpdate = false;
		Boolean forNewCopy = false;
		String actionName = currentAction.Name;
		if ((currentAction.Type == ActionType.CustomMessage || currentAction.Type == ActionType.CustomSequence))
		{
			if (currentAction.OriginalName != null && !currentAction.OriginalName.isEmpty())
			{
				actionName = currentAction.OriginalName;
				isUpdate = true;
			}
			else
			{
				isUpdate = false;
			}
		}
		else
		{
			forNewCopy = true;
		}
		
		JButton buttonSave;
		JPanel savePanel = new JPanel();
		savePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		JLabel question1, question2, questionMark1, questionMark2;
		newName = new JTextField(20);
		newName.addCaretListener(nameTextFieldChangedListener);
				
		GridBagLayout gbl_Panel = new GridBagLayout();
//	    gbl_Panel.columnWeights = new double[]{10.0, 5.0, 1.0, 2.0};
	    savePanel.setLayout(gbl_Panel);
		
		if (isUpdate)
		{
			buttonSave = new JButton("Overwrite");
			buttonSave.addActionListener(new SaveOverwrite());
			buttonSaveAsNew = new JButton("Save as New");
			buttonSaveAsNew.addActionListener(new SaveNew());
			question1 = new JLabel("<html>Do you wish to save as <font color=blue>"+ actionName + "</font></html>");
			question2 = new JLabel("Or save as ");
			questionMark1 = new JLabel("?");
			questionMark2 = new JLabel("?");
			
		    GridBagConstraints gbc_Question1 = new GridBagConstraints();
		    gbc_Question1.gridx = 0;
		    gbc_Question1.gridy = 0;
		    gbc_Question1.gridwidth = 2;
		    gbc_Question1.anchor = GridBagConstraints.LINE_END;
		    gbc_Question1.insets = new Insets(0,0,10,0);
		    
		    GridBagConstraints gbc_QuestionMark1 = new GridBagConstraints();
		    gbc_QuestionMark1.gridx = 2;
		    gbc_QuestionMark1.gridy = 0;
		    gbc_QuestionMark1.gridwidth = 1;
		    gbc_QuestionMark1.anchor = GridBagConstraints.CENTER;
		    gbc_QuestionMark1.insets = new Insets(0,0,10,0);
		    
		    GridBagConstraints gbc_Question2 = new GridBagConstraints();
		    gbc_Question2.gridx = 0;
		    gbc_Question2.gridy = 1;
		    gbc_Question2.gridwidth = 1;
		    gbc_Question2.anchor = GridBagConstraints.LINE_END;
		    
		    GridBagConstraints gbc_NewName = new GridBagConstraints();
		    gbc_NewName.gridx = 1;
		    gbc_NewName.gridy = 1;
		    gbc_NewName.gridwidth = 1;
		    gbc_NewName.anchor = GridBagConstraints.CENTER;
		    gbc_NewName.fill = GridBagConstraints.HORIZONTAL;
		    
		    GridBagConstraints gbc_QuestionMark2 = new GridBagConstraints();
		    gbc_QuestionMark2.gridx = 2;
		    gbc_QuestionMark2.gridy = 1;
		    gbc_QuestionMark2.gridwidth = 1;
		    gbc_QuestionMark2.anchor = GridBagConstraints.CENTER;
		    
		    GridBagConstraints gbc_ButtonSave = new GridBagConstraints();
		    gbc_ButtonSave.gridx = 3;
		    gbc_ButtonSave.gridy = 0;
		    gbc_ButtonSave.gridwidth = 1;
		    gbc_ButtonSave.fill = GridBagConstraints.HORIZONTAL;
		    gbc_ButtonSave.insets = new Insets(0,20,10,0);
		    
		    GridBagConstraints gbc_ButtonSaveAsNew = new GridBagConstraints();
		    gbc_ButtonSaveAsNew.gridx = 3;
		    gbc_ButtonSaveAsNew.gridy = 1;
		    gbc_ButtonSaveAsNew.gridwidth = 1;
		    gbc_ButtonSaveAsNew.fill = GridBagConstraints.HORIZONTAL;
		    gbc_ButtonSaveAsNew.insets = new Insets(0,20,0,0);
		    
			savePanel.add(question1, gbc_Question1);
			savePanel.add(questionMark1, gbc_QuestionMark1);
			savePanel.add(buttonSave, gbc_ButtonSave);
			savePanel.add(question2, gbc_Question2);
			savePanel.add(newName, gbc_NewName);
			savePanel.add(questionMark2, gbc_QuestionMark2);
			savePanel.add(buttonSaveAsNew, gbc_ButtonSaveAsNew);
		}
		else 
		{
			
			buttonSaveAsNew = new JButton("Save");
			buttonSaveAsNew.addActionListener(new SaveNew());
			
			if(!forNewCopy)
			{
				question1 = new JLabel("Save as ");				
			}
			else
			{
				question1 = new JLabel("Make a new copy?");
			}
			
			GridBagConstraints gbc_Question1 = new GridBagConstraints();
			gbc_Question1.gridx = 0;
			gbc_Question1.gridy = 0;
			gbc_Question1.gridwidth = 1;
			gbc_Question1.anchor = GridBagConstraints.LINE_END;
		    
		    GridBagConstraints gbc_NewName = new GridBagConstraints();
		    gbc_NewName.gridx = 1;
		    gbc_NewName.gridy = 0;
		    gbc_NewName.gridwidth = 1;
		    gbc_NewName.anchor = GridBagConstraints.LINE_START;
//		    gbc_NewName.fill = GridBagConstraints.HORIZONTAL;
		    gbc_NewName.insets = new Insets(0,20,0,20);
		    
		    GridBagConstraints gbc_ButtonSaveAsNew = new GridBagConstraints();
		    gbc_ButtonSaveAsNew.gridx = 2;
		    gbc_ButtonSaveAsNew.gridy = 0;
		    gbc_ButtonSaveAsNew.gridwidth = 1;
//		    gbc_ButtonSaveAsNew.fill = GridBagConstraints.HORIZONTAL;
		    gbc_ButtonSaveAsNew.anchor = GridBagConstraints.LINE_END;
		    gbc_ButtonSaveAsNew.ipadx = 20;
		    gbc_ButtonSaveAsNew.insets = new Insets(0,20,0,0);
			
			savePanel.add(question1, gbc_Question1);
			savePanel.add(newName, gbc_NewName);
			savePanel.add(buttonSaveAsNew, gbc_ButtonSaveAsNew);
		}
		
		showPopUp(savePanel, true);
	}
	
	/************************************
	 * EVENT HANDLERS
	 *************************************/

	/** LIST PANEL **/
	
	private class MessageSelectionListener implements ListSelectionListener 
	{
		@Override
		public void valueChanged(ListSelectionEvent e) 
		{
			
			if (!e.getValueIsAdjusting())
			{
				if (messagesList.getSelectedIndex() != -1)
				{

					sequencesList.clearSelection();
					//sequenceDetail.clearSelection();
					//sequenceDetailListModel.removeAllElements();
					closePopUp();

					setRightPanelUI(UIState.Message);
					btnPickThisMessage.setEnabled(true);

					String selectedValue = messagesList.getSelectedValue().toString();
					currentDisplayedAction.Set(ActionType.Message, selectedValue);
					
					try
					{
						final FileReader f = new FileReader(messagesFolder + File.separator +selectedValue + ".json");
						try 
						{
							textArea_Json.read(f, null);
						} catch (IOException ioe) {
							ioe.printStackTrace();
							UIManager.getLookAndFeel().provideErrorFeedback(textArea_Json);
						} finally {
							if (f != null) 
								try { f.close(); } catch (Exception ex) { ex.printStackTrace(); }
						}
					} catch (Exception ex) { ex.printStackTrace(); }

					textArea_Json.setCaretPosition(0);

				}
				else
				{
					btnPickThisMessage.setEnabled(false);
				}
			}
		}
	}

	private class SequenceSelectionListener implements ListSelectionListener 
	{
		@Override
		public void valueChanged(ListSelectionEvent e) 
		{
			
			if (!e.getValueIsAdjusting())
			{

				if (sequencesList.getSelectedIndex() != -1)
				{

					messagesList.clearSelection();
					sequenceDetail.clearSelection();
					sequenceDetailListModel.removeAllElements();
					closePopUp();
				
					setRightPanelUI(UIState.Sequence);
					btnPickThisSequence.setEnabled(true);
				
					ActionListElement element = ((SortedListOfActions)sequencesList.getModel()).getActionListElementAt(sequencesList.getSelectedIndex());
					String selectedValue = sequencesList.getSelectedValue().toString();
					
					
					if (element != null && !element.IsCorrect)
					{
						warningPopUp("Sequence contains inexistant messages.", null, true);
					}
					
					File sequenceFile = new File(sequencesFolder + "\\" +selectedValue + ".json");
					JsonParser parser = new JsonParser();
					JsonElement jsonFile = null;
					JsonArray jsonSequence = null;
				
					try
					{
						final FileReader f = new FileReader(sequenceFile);
						try 
						{
							textArea_Json.read(f, null);
							jsonFile = parser.parse(new FileReader(sequenceFile));

						} catch (IOException ioe) {
							UIManager.getLookAndFeel().provideErrorFeedback(textArea_Json);
							ioe.printStackTrace();
						} finally {
							if (f != null) try { f.close(); } catch (Exception ex) { ex.printStackTrace(); }
						}
					} catch (Exception ex) { ex.printStackTrace(); }

					// If file JSON with sequence name exists, parse to get list of actions
					if (jsonFile != null)
						jsonSequence = jsonFile.getAsJsonObject().getAsJsonArray("Sequence");
					if (jsonSequence != null)
					{
						for (int i = 0; i < jsonSequence.size(); i++)
						{
							String type = jsonSequence.get(i).getAsJsonObject().get("Type").getAsString();
							String action;
							if (type.toLowerCase().equals("wait"))
							{
								action = waitActionStart + jsonSequence.get(i).getAsJsonObject().get("Value").getAsString();
								sequenceDetailListModel.addElement(new ActionListElement(action, ActionType.Message, true));
							}
							else 
							{
								action = jsonSequence.get(i).getAsJsonObject().get("Value").getAsString();
								if (checkExistence(jsonSequence.get(i).getAsJsonObject()))
								{
									sequenceDetailListModel.addElement(new ActionListElement(action, ActionType.Message, true));
								}
								else
								{
									sequenceDetailListModel.addElement(new ActionListElement(action, ActionType.Message, false));
								}
							}					
						}
					
						currentDisplayedAction.Set(ActionType.Sequence, selectedValue);
					}
					else
					{
						Logger.Warning("Selected sequence " + selectedValue + " is wrongly formatted and does not contain a \"Sequence\" object.");
						btnUseThis.setEnabled(false);
					}

					textArea_Json.setCaretPosition(0);

				}
				else
				{
					sequenceDetail.clearSelection();
					sequenceDetailListModel.removeAllElements();
					btnPickThisSequence.setEnabled(false);
				}
			}
		}
	}
	
	private class SequenceDetailsSelectionListener implements ListSelectionListener 
	{
		@Override
		public void valueChanged(ListSelectionEvent e) 
		{
			if (!e.getValueIsAdjusting())
			{
				if (sequenceDetail.getSelectedIndex() != -1)
				{
					messagesList.clearSelection();
					closePopUp();

					String selectedValue = sequenceDetail.getSelectedValue().toString();
					
					if (selectedValue.endsWith(actionMalformedSuffix))
					{
						selectedValue = selectedValue.substring(0, selectedValue.lastIndexOf(actionMalformedSuffix));
					}
					
					if (selectedValue.startsWith(waitActionStart))
					{
						textArea_Json.setText("");
					}
					else
					{

						File messageFile = new File(messagesFolder + File.separator + selectedValue + ".json");

						FileReader f = null;
						try 
						{
							f = new FileReader(messageFile);
							textArea_Json.read(f, null);

							currentDisplayedAction.Set(ActionType.Message, selectedValue);
							setRightPanelUI(UIState.Message);

						} catch (IOException ioe) {
							textArea_Json.setText("");
							Logger.Info("Trying to open an inexistant message file from detail pane.");
							UIManager.getLookAndFeel().provideErrorFeedback(textArea_Json);
						} finally {
							if (f != null) try { f.close(); } catch (Exception ex) { ex.printStackTrace(); }
						}
					}

					textArea_Json.setCaretPosition(0);

				}
			}
		}
	}
	
	private class FilterChangedListener implements CaretListener
	{

		@Override
		public void caretUpdate(CaretEvent e) {
			if (filter != textField_MessageFilter.getText().toLowerCase())
			{
				btnPickThisMessage.setEnabled(false);
				btnPickThisSequence.setEnabled(false);
				filterLists();		

				select(currentDisplayedAction);
			}	
		}

	}
	
	private class ChckbxFilterItemListener implements ItemListener{
		
		@Override  
		public void itemStateChanged(ItemEvent e) {
			filterInsideSequences = chckbx_searchInSeqDetails.isSelected();
			
			if (!filter.isEmpty())
			{
				filterLists();
			}
		}
	}
	
	/** EDITOR PANEL **/
	
	private class JsonEdited implements KeyListener
	{

		@Override
		public void keyTyped(KeyEvent e) {
			
			closePopUp();
			
			if (currentDisplayedAction.Type == ActionType.Message)
			{
				currentDisplayedAction.MakeCustom();
				setRightPanelUI(UIState.CustomMessage);			
			}
			else if (currentDisplayedAction.Type == ActionType.Sequence)
			{
				currentDisplayedAction.MakeCustom();
				setRightPanelUI(UIState.CustomSequence);			
			}
			
			btnPickThisMessage.setEnabled(false);
			btnPickThisSequence.setEnabled(false);
			btnDelete.setEnabled(false);
		}

		@Override
		public void keyReleased(KeyEvent e) {}

		@Override
		public void keyPressed(KeyEvent e) {}
		
	}
	
	/** BUTTONS **/
	
	private class SetAction implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {
			
			currentAction.CopyOf(currentDisplayedAction);
			saveToDiskAsCurrentAction(currentAction);
			//restartWebServer();
			resetApp(true);
			
		}
	}
	
	private class SettingsClicked implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!settingsOn)
			{
				textArea_Json.removeKeyListener(jsonEdited);

				splitPaneDivLocation = splitPane_Main.getDividerLocation();
				panel_Main_LeftPane.setVisible(false);
				panel_textAreaButtons.setVisible(false);
				progressBar_Server.setVisible(false);
				
				readSettingsFile();
				
				settingsOn = true;
				
				infoPopUp("Click on same icon will automatically save the modifications to the settings.", null, false);			
			}
			else
			{
				closePopUp();
				
				JsonParser parser = new JsonParser();
				try
				{
					parser.parse(textArea_Json.getText());
				}
				catch (JsonSyntaxException jSE)
				{
					infoPopUp("The json is malformed and cannot be saved as such:<br>"+jSE.getMessage(), null, true);
					return;
				}
				
				saveSettingsFile();
				
				panel_Main_LeftPane.setVisible(true);
				splitPane_Main.setDividerLocation(splitPaneDivLocation);
				panel_textAreaButtons.setVisible(true);
				progressBar_Server.setVisible(true);
				
				resetApp(false);
				
				loadAction(currentDisplayedAction);
				
				textArea_Json.addKeyListener(jsonEdited);
				
				settingsOn = false;

			}
		}
	}

	
	/** POPUPS **/
	
	private class PopupClicked implements MouseListener, ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {
			closePopUp();
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			closePopUp();
		}

		@Override
		public void mousePressed(MouseEvent e) { }

		@Override
		public void mouseReleased(MouseEvent e) { }

		@Override
		public void mouseEntered(MouseEvent e) { }

		@Override
		public void mouseExited(MouseEvent e) { }

		
	}
	
	private class NameTextFieldChangedListener implements CaretListener
	{

		@Override
		public void caretUpdate(CaretEvent e) {
			
			String checkName = ((JTextField)e.getSource()).getText();
			int i = 0;
			if (currentDisplayedAction.Type == ActionType.Message || currentDisplayedAction.Type == ActionType.CustomMessage)
			{
				for (i = 0; i < messagesListModel.getSize(); i++)
				{
					if (messagesListModel.getElementAt(i).toString().equalsIgnoreCase(checkName))
					{
						((JTextField)e.getSource()).setBackground(warningColor);
						buttonSaveAsNew.setEnabled(false);
						return;
					}
				}
			}
			else if (currentDisplayedAction.Type == ActionType.Sequence || currentDisplayedAction.Type == ActionType.CustomSequence)
			{
				for (i = 0; i < sequencesListModel.getSize(); i++)
				{
					if (sequencesListModel.getElementAt(i).toString().equalsIgnoreCase(checkName))
					{
						((JTextField)e.getSource()).setBackground(warningColor);
						buttonSaveAsNew.setEnabled(false);
						return;
					}
				}
			}
			((JTextField)e.getSource()).setBackground(textFieldBG);
			buttonSaveAsNew.setEnabled(true);

		}
		
	}
	
	/** SAVE EVENTS **/
	
	private class SaveAction implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {
			JsonParser parser = new JsonParser();
			try
			{
				parser.parse(textArea_Json.getText());
			}
			catch (JsonSyntaxException jSE)
			{
				infoPopUp("The json is malformed and cannot be saved as such:<br>"+jSE.getMessage(), null, true);
				return;
			}
			
			saveActionPopUp(currentDisplayedAction);	
		}	
	}
	
	private class SaveOverwrite implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {

			closePopUp();
			
			String saveTo;
			ActionType type = currentDisplayedAction.Type;
			ActionType newType = type;
			String name = currentDisplayedAction.Name;
			
			switch (type)
			{
			case Message:
				saveTo = messagesFolder + "\\" + name + ".json";
				break;
			case Sequence:
				saveTo = sequencesFolder + "\\" + name + ".json";
				break;
			case CustomMessage:
				name = currentDisplayedAction.OriginalName;
				saveTo = messagesFolder + "\\" + currentDisplayedAction.OriginalName + ".json";
				newType = ActionType.Message;
				break;
			case CustomSequence:
				name = currentDisplayedAction.OriginalName;
				saveTo = sequencesFolder + "\\" + currentDisplayedAction.OriginalName + ".json";
				newType = ActionType.Sequence;
				break;
			
			default:
				saveTo = "";
				break;
			}
			
			Logger.Log("Save file (overwrite): "+ saveTo);
			
			BufferedWriter file;
			try {
				file = new BufferedWriter(new FileWriter(saveTo));
				file.write(textArea_Json.getText());
				file.close();
			} catch (IOException ex) { ex.printStackTrace(); }
			
			loadAction(new Action(newType, name));
						
		}
	}
	
	private class SaveNew implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {

			closePopUp();
			
			String saveTo = "";
			ActionType type = currentDisplayedAction.Type;
			ActionType newType = type;
			String name = newName.getText();
			
			if (name != null && name != "")
			{
				switch (type)
				{
				case Message:
					saveTo = messagesFolder + "\\" + name + ".json";
					break;
				case Sequence:
					saveTo = sequencesFolder + "\\" + name + ".json";
					break;
				case CustomMessage:
					saveTo = messagesFolder + "\\" + name + ".json";
					newType = ActionType.Message;
					break;
				case CustomSequence:
					saveTo = sequencesFolder + "\\" + name + ".json";
					newType = ActionType.Sequence;
					break;
			
				default:
					saveTo = "";
					break;
				}
			
				Logger.Info("Save file: "+ saveTo);
			}
			
			File actionFile = new File(saveTo);
			if (actionFile.exists())
			{
				infoPopUp("Action already exists. Choose another name", new JButton[] {new JButton("OK")}, true);
			}
			else
			{
				BufferedWriter file;
				try {
					file = new BufferedWriter(new FileWriter(saveTo));
					file.write(textArea_Json.getText());
					file.close();
				} catch (IOException ex) { ex.printStackTrace(); }				
				
				loadAction(new Action(newType, name));
			}
		}
	}
	
	/** NEW ACTION EVENTS **/
	
	private class NewAction implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {
			newActionPopUp();
		}
	}
	
	private class NewMessageClicked implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {
			
			closePopUp();
			String newMessage = "{\n" 
					+ "\t\"InputElements\": [{\n"
					+ "\t\t\"Text\": \"\",\n"
					+ "\t\t\"Id\": \"\",\n"
					+ "\t\t\"Type\": \"\"\n"
					+ "\t}],\n"
					+ "\t\"OutputElements\": [{\n"
					+ "\t\t\"Text\": \"\",\n"
					+ "\t\t\"Id\": \"\",\n"
					+ "\t\t\"Type\": \"\"\n"
					+ "\t}],\n"
					+ "\t\"Id\": \"\",\n"
					+ "\t\"Language\": \"ita\",\n"
					+ "\t\"Type\": \"\",\n"
					+ "\t\"Number\":  " + new Random().nextInt(200)
					+ "\n}";
			
			textArea_Json.setText(newMessage);
			if (currentDisplayedAction.Type == ActionType.Message)
			{
				currentDisplayedAction.Set(ActionType.CustomMessage, "", currentDisplayedAction.Name);
			}
			else if (currentDisplayedAction.Type != ActionType.CustomMessage)
			{
				currentDisplayedAction.Set(ActionType.CustomMessage, "", "");
				messagesList.clearSelection();
				sequencesList.clearSelection();
			}
			setRightPanelUI(UIState.CustomMessage);	
			
		}
	}
	
	private class NewSequenceClicked implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {
			
			closePopUp();
			
			String newSequence = "{\n" 
					+ "\t\"Sequence\": [\n"
					+ "\t{\n"
					+ "\t\t\"Type\": \"message\",\n"
					+ "\t\t\"Value\": \"\"\n"
					+ "\t},\n"
					+ "\t{\n"
					+ "\t\t\"Type\": \"wait\",\n"
					+ "\t\t\"Value\": \"2000\"\n"
					+ "\t}]\n"
					+ "}";
			
			textArea_Json.setText(newSequence);
			if (currentDisplayedAction.Type == ActionType.Sequence)
			{
				currentDisplayedAction.Set(ActionType.CustomSequence, "", currentDisplayedAction.Name);
			}
			else if (currentDisplayedAction.Type != ActionType.CustomSequence)
			{
				currentDisplayedAction.Set(ActionType.CustomSequence, "", "");
				messagesList.clearSelection();
				sequencesList.clearSelection();
			}
			setRightPanelUI(UIState.CustomSequence);	

		}
	}
	
	/** DELETE EVENTS **/
	
	private class DeleteAction implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {
			JButton[] buttons = new JButton[2];
			buttons[0] = new JButton("Yes");
			buttons[0].addActionListener(new ConfirmDelete());
			buttons[1] = new JButton("No");
			buttons[1].addActionListener(new PopupClicked());
			
			String popupMessage = "";
			if (currentDisplayedAction.Name == currentAction.Name && currentDisplayedAction.Type == currentAction.Type)
				popupMessage = "This action is in use currently. Delete anyways?";
			else 
				popupMessage = "Are you sure you want to delete " + currentDisplayedAction.Name + "?";
			
			infoPopUp(popupMessage, buttons, true);	
		}
	}
	
	private class ConfirmDelete implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {
			ActionType type = currentDisplayedAction.Type;
			String name = currentDisplayedAction.Name;
			File toDelete = new File("");;
			textArea_Json.setText("");
			
			Boolean isCurrent = false;
			if (currentDisplayedAction.Name == currentAction.Name && currentDisplayedAction.Type == currentAction.Type)
				isCurrent = true;
			
			switch (type)
			{
			case Message:
				toDelete = new File(messagesFolder + File.separator + name + ".json");
				break;
			case Sequence:
				toDelete = new File(sequencesFolder + File.separator + name + ".json");
				break;
			case CustomMessage:
			case CustomSequence:
				textArea_Json.setText("");
				break;
			}
			

			if (toDelete.exists())
			{
				System.gc();
				Logger.Info("Delete file " + toDelete.getAbsolutePath());
				if (!toDelete.delete())
				{
					Logger.Info("Cannot delete file. Java's fault. Reason unknown.");
					infoPopUp("Java stuff happens: deletion failed. Try again later.", null, true);
				}
			}
			
			resetApp(false);
			currentDisplayedAction.Set(null, "", "");
			loadAction(currentDisplayedAction);
			if (isCurrent)
			{
				stopWebServer();
				currentAction.Set(null, "", "");
				saveToDiskAsCurrentAction(null);
			}
			closePopUp();
		}
		
	}
	
	/** PROGRESS BAR **/
	
	private class ProgressBarMenu_RestartServer implements MouseListener
	{

		@Override
		public void mouseClicked(MouseEvent e) {}	

		@Override
		public void mousePressed(MouseEvent e) {		
			if (currentAction.Type != null)
				restartWebServer();
			else
				infoPopUp("No action in use. Choose action to start server.", null, true);
		}

		@Override
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}
		
	}

	private class ProgressBarMenu_ShowCurrentAction implements MouseListener
	{

		@Override
		public void mouseClicked(MouseEvent e) {	}	

		@Override
		public void mousePressed(MouseEvent e) {
			if (currentAction.Type == ActionType.Message)
			{
				int i = messagesListModel.indexOf(currentAction.Name);
				messagesList.ensureIndexIsVisible(i - messagesList.getVisibleRowCount());
				messagesList.ensureIndexIsVisible(i + messagesList.getVisibleRowCount());
				messagesList.ensureIndexIsVisible(i);
			}
			else if (currentAction.Type == ActionType.Sequence)
			{
				int i = sequencesListModel.indexOf(currentAction.Name);
				sequencesList.ensureIndexIsVisible(i - sequencesList.getVisibleRowCount());
				sequencesList.ensureIndexIsVisible(i + sequencesList.getVisibleRowCount());
				sequencesList.ensureIndexIsVisible(i);
			}
			else
			{
				infoPopUp("No action currently in use.", null, true);
			}
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}
		
	}

	
	/************************************
	 * ENUMERATIONS AND STRUCTS
	 *************************************/
	public enum UIState
	{
		Message,
		Sequence,
		CustomMessage,
		CustomSequence,
		MessageFromDetailOfSequence
	}
	
	/************************************
	 * RIGHT-CLICK MENU (Auto-generated)
	 *************************************/
	
	private static void addPopup(Component component, final JPopupMenu popup) {
		component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			private void showMenu(MouseEvent e) {
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}
	
	/************************************
	 * JList cell renderer
	 *************************************/
	@SuppressWarnings("serial")
	private class SortedListOfActionsCellRenderer extends DefaultListCellRenderer
	{

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			ActionListElement element = ((SortedListOfActions)list.getModel()).getActionListElementAt(index);

			if (element != null)
			{
				if (!element.IsCorrect)
				{
					setForeground(errorColor);
					setOpaque(true); // otherwise, it's transparent
				}
				if (element.Name.equalsIgnoreCase(currentAction.Name) && element.Type == currentAction.Type)
				{
					setBackground(currentColor);
					setOpaque(true); // otherwise, it's transparent
				}
			}

			return this;  // DefaultListCellRenderer derived from JLabel, DefaultListCellRenderer.getListCellRendererComponent returns this as well.
		}
	}
	
	@SuppressWarnings("serial")
	private class SequenceDetailCellRenderer extends DefaultListCellRenderer
	{

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			ActionListElement element = (ActionListElement)((DefaultListModel)list.getModel()).getElementAt(index);

			if (element != null)
			{
				setText(element.Name);
				if (!element.IsCorrect)
				{
					setForeground(errorColor);
					setOpaque(true); // otherwise, it's transparent
				}
			}

			return this;  // DefaultListCellRenderer derived from JLabel, DefaultListCellRenderer.getListCellRendererComponent returns this as well.
		}
	}
	
}
