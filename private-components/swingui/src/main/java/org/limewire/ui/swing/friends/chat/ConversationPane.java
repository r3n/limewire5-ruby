package org.limewire.ui.swing.friends.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLEditorKit;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.FileOfferFeature;
import org.limewire.core.api.friend.feature.features.FileOfferer;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.CopyAction;
import org.limewire.ui.swing.action.CopyAllAction;
import org.limewire.ui.swing.action.PopupUtil;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.RuntimeTopicEventSubscriber;
import org.limewire.ui.swing.friends.chat.Message.Type;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.painter.GenericBarPainter;
import org.limewire.ui.swing.util.DNDUtils;
import org.limewire.ui.swing.util.GuiUtils;
import static org.limewire.ui.swing.util.I18n.tr;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.util.FileUtils;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

import net.miginfocom.swing.MigLayout;

/**
 *
 */
public class ConversationPane extends JPanel implements Displayable, Conversation {
    private static final int PADDING = 5;
    private static final Log LOG = LogFactory.getLog(ConversationPane.class);
    private static final Color DEFAULT_BACKGROUND = new Color(224, 224, 224);
    private static final Color BACKGROUND_COLOR = Color.WHITE;

    private final List<Message> messages = new ArrayList<Message>();
    private final Map<String, MessageFileOffer> idToMessageWithFileOffer =
            new ConcurrentHashMap<String, MessageFileOffer>();

    private final JEditorPane editor;
    private final String conversationName;
    private final String friendId;
    private final String loggedInID;
    private final MessageWriter writer;
    private final ChatFriend chatFriend;
    private final ShareListManager shareListManager;
    private final IconManager iconManager;
    private final LibraryNavigator libraryNavigator;
    private HyperlinkButton downloadlink;
    private HyperlinkButton sharelink;
    private ResizingInputPanel inputPanel;
    private ChatState currentChatState;

    private ListenerSupport<FriendEvent> friendSupport;
    private ListenerSupport<FeatureEvent> featureSupport;
    private EventListener<FeatureEvent> featureListener;
    private EventListener<FriendEvent> friendListener;
    
    @Resource(key="ChatConversation.toolbarTopColor") private Color toolbarTopColor;
    @Resource(key="ChatConversation.toolbarBottomColor") private Color toolbarBottomColor;
    @Resource(key="ChatConversation.toolbarBorderColor") private Color toolbarBorderColor;
    @Resource(key="ChatConversation.linkFont") private Font linkFont;

    private final JScrollPane conversationScroll;
    private final JPanel chatWrapper;
    
    @AssistedInject
    public ConversationPane(@Assisted MessageWriter writer, final @Assisted ChatFriend chatFriend, @Assisted String loggedInID,
                            ShareListManager libraryManager, IconManager iconManager, LibraryNavigator libraryNavigator,
                            IconLibrary iconLibrary, ChatHyperlinkListenerFactory chatHyperlinkListenerFactory,
                            @Named("backgroundExecutor")ScheduledExecutorService schedExecService) {
        this.writer = writer;
        this.chatFriend = chatFriend;
        this.conversationName = chatFriend.getName();
        this.friendId = chatFriend.getID();
        this.loggedInID = loggedInID;
        this.shareListManager = libraryManager;
        this.iconManager = iconManager;
        this.libraryNavigator = libraryNavigator;
        
        GuiUtils.assignResources(this);

        setLayout(new BorderLayout());
        
        editor = new JEditorPane();
        editor.setEditable(false);
        editor.setContentType("text/html");
        editor.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        HTMLEditorKit editorKit = (HTMLEditorKit) editor.getEditorKit();
        editorKit.setAutoFormSubmission(false);

        conversationScroll = new JScrollPane(editor, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        conversationScroll.setOpaque(false);
        conversationScroll.setBorder(BorderFactory.createEmptyBorder());
        
        
        final JButton closeConversation = new IconButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new CloseChatEvent(chatFriend).publish();
            }
        });
        closeConversation.setIcon(iconLibrary.getEndChat());
        
        chatWrapper = new JPanel();
        chatWrapper.setLayout(new OverlayLayout(chatWrapper));
        
        JPanel closePanel = new JPanel();
        closePanel.setLayout(null);
        closePanel.setOpaque(false);
        final Rectangle closeBounds = new Rectangle(268,5,6,6);
        final Rectangle closeBoundsSlider = new Rectangle(250,5,6,6);
        closeConversation.setBounds(closeBounds);
        closePanel.add(closeConversation);
        
        JPanel conversationPanel = new JPanel(new BorderLayout());
        conversationPanel.setOpaque(false);
        conversationPanel.add(conversationScroll, BorderLayout.CENTER);
        
        chatWrapper.add(closePanel);
        chatWrapper.add(conversationPanel);
        
        conversationScroll.getVerticalScrollBar().addComponentListener(new ComponentListener() {
            @Override
            public void componentHidden(ComponentEvent e) {
                closeConversation.setBounds(closeBounds);
            }
            @Override
            public void componentMoved(ComponentEvent e) {
            }
            @Override
            public void componentResized(ComponentEvent e) {
            }
            @Override
            public void componentShown(ComponentEvent e) {
                closeConversation.setBounds(closeBoundsSlider);
            }
        });
        
        AdjustmentListener adjustmentListener = new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                chatWrapper.repaint();
            }
        };
        
        conversationScroll.getVerticalScrollBar().addAdjustmentListener(adjustmentListener);
        conversationScroll.getHorizontalScrollBar().addAdjustmentListener(adjustmentListener);
        
        add(chatWrapper, BorderLayout.CENTER);

        PopupUtil.addPopupMenus(editor, new CopyAction(editor), new CopyAllAction());

        FriendShareDropTarget friendShare = new FriendShareDropTarget(editor, libraryManager.getOrCreateFriendShareList(chatFriend.getUser()));
        editor.setDropTarget(friendShare.getDropTarget());

        add(footerPanel(writer, chatFriend, schedExecService), BorderLayout.SOUTH);

        setBackground(DEFAULT_BACKGROUND);

        editor.addHyperlinkListener(chatHyperlinkListenerFactory.create(this));
        EventAnnotationProcessor.subscribe(this);
    }

    
    @Inject
    public void register(@Named("available")ListenerSupport<FriendEvent> friendSupport,
                         ListenerSupport<FeatureEvent> featureSupport) {
        this.friendSupport = friendSupport;
        this.featureSupport = featureSupport;

        featureListener = new EventListener<FeatureEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FeatureEvent event) {
                if (event.getSource().getFriend().getId().equals(friendId)) {
                    handleFeatureUpdate(event);
                }
            }
        };

        friendListener = new EventListener<FriendEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendEvent event) {
                if (event.getData().getId().equals(friendId)) {
                    handleFriendEvent(event);
                }
            }
        };
        friendSupport.addListener(friendListener);
        featureSupport.addListener(featureListener);
    }

    @RuntimeTopicEventSubscriber(methodName="getMessageReceivedTopicName")
    public void handleConversationMessage(String topic, MessageReceivedEvent event) {
        Message message = event.getMessage();
        LOG.debugf("Message: from {0} text: {1} topic: {2}", message.getSenderName(), message.toString(), topic);
        messages.add(message);
        Type type = message.getType();

        if (type != Type.Sent) {
            currentChatState = ChatState.active;
        }

        if (message instanceof MessageFileOffer) {
            MessageFileOffer msgWithFileOffer = (MessageFileOffer)message;
            String fileOfferID = msgWithFileOffer.getFileOffer().getId();
            idToMessageWithFileOffer.put(fileOfferID, msgWithFileOffer);
        }

        displayMessages();
    }

    @RuntimeTopicEventSubscriber(methodName="getChatStateTopicName")
    public void handleChatStateUpdate(String topic, ChatStateEvent event) {
        LOG.debugf("Chat state update for {0} to {1}", event.getFriend().getName(), event.getState());
        if (currentChatState != event.getState()) {
            currentChatState = event.getState();
            displayMessages();
        }
    }

    private void handleFriendEvent(FriendEvent event) {
        switch(event.getType()) {
        case ADDED:
            currentChatState = ChatState.active;
            displayMessages(false);
            inputPanel.getInputComponent().setEnabled(true);
            break;
        case REMOVED:
            displayMessages(true);
            inputPanel.getInputComponent().setEnabled(false);
            break;
        }
    }

    private void handleFeatureUpdate(FeatureEvent featureEvent) {
        FeatureEvent.Type featureEventType = featureEvent.getType();
        Feature feature = featureEvent.getData();

        if (feature.getID().equals(LimewireFeature.ID)) {
            if (featureEventType == FeatureEvent.Type.ADDED) {
                downloadlink.setEnabled(true);
            } else if (featureEventType == FeatureEvent.Type.REMOVED) {
                downloadlink.setEnabled(false);
            }
        }
    }

    public void setChatStateGone() {
        try {
            writer.setChatState(ChatState.gone);
        } catch (XMPPException e) {
            LOG.error("Could not set chat state while closing the conversation", e);
        }
    }

    public void destroy() {
        EventAnnotationProcessor.unsubscribe(this);
        featureSupport.removeListener(featureListener);
        friendSupport.removeListener(friendListener);
        inputPanel.destroy();
    }

    public String getMessageReceivedTopicName() {
        return MessageReceivedEvent.buildTopic(friendId);
    }

    public String getChatStateTopicName() {
        return ChatStateEvent.buildTopic(friendId);
    }

    public void displayMessages() {
        displayMessages(false);
    }

    public ChatFriend getChatFriend() {
        return chatFriend;
    }

    public Map<String, MessageFileOffer> getFileOfferMessages() {
        return Collections.unmodifiableMap(new HashMap<String, MessageFileOffer>(idToMessageWithFileOffer));
    }

    private void displayMessages(boolean friendSignedOff) {
        String chatDoc = ChatDocumentBuilder.buildChatText(messages, currentChatState, conversationName, friendSignedOff);
        LOG.debugf("Chat doc: {0}", chatDoc);
        final JScrollBar verticalScrollBar = conversationScroll.getVerticalScrollBar();
        final int scrollValue = verticalScrollBar.getValue();
        editor.setText(chatDoc);
        
        //LWC-2262: If the scroll bar was moved above the bottom of the scrollpane, reset the value of
        //the bar to where it was before the text was updated.  This needs to be issued to the end of the
        //queue because the actual repainting/resizing of the scrollbar happens later in a 
        //task added to the EDT by the plaf listener of the editor's document.
        //A better fix for this behavior may be possible
        if (verticalScrollBar.getMaximum() > (scrollValue + verticalScrollBar.getVisibleAmount() + PADDING)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    verticalScrollBar.setValue(scrollValue);
                }
            });
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    verticalScrollBar.setValue(verticalScrollBar.getMaximum());
                }
            });
        }
        
        decorateFileOfferButtons();
        
        chatWrapper.repaint();
    }

    private void decorateFileOfferButtons() {
        //This is a hack to set the file mime-type icon for file offer buttons that may appear in the conversation
        recursiveButtonSearch(editor);
    }

    private void recursiveButtonSearch(Container container) {
        for(Component component : container.getComponents()) {
            if (component instanceof Container) {
                recursiveButtonSearch((Container)component);
            }
            if (component instanceof JButton) {
                JButton button = (JButton)component;
                String buttonText = button.getText();

                // Using end of button text to determine whether button shouild be disabled
                // then disable it.  This is because JEditorPane does not disable buttons
                // disabled in the form html
                if (buttonText.endsWith(":disabled")) {
                    buttonText = buttonText.substring(0, buttonText.lastIndexOf(":disabled"));
                    button.setText(buttonText);
                    button.setEnabled(false);
                }

                String extension = FileUtils.getFileExtension(buttonText);
                if (!extension.isEmpty()) {
                    Icon icon = iconManager.getIconForExtension(extension);
                    button.setIcon(icon);
                }

            }
        }
    }

    private JPanel footerPanel(MessageWriter writer, ChatFriend chatFriend,
                               ScheduledExecutorService schedExecService) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        
        downloadlink = new HyperlinkButton(new DownloadFromFriendLibraryAction());
        downloadlink.setFont(linkFont);

        sharelink = new HyperlinkButton(new ShareAction());
        sharelink.setFont(linkFont);
        
        
        JXPanel toolbar = new JXPanel(new MigLayout("insets 0 0 0 5, gap 10, alignx right, aligny 50%"));
        ResizeUtils.forceHeight(toolbar, 22);
        
        toolbar.setBackgroundPainter(new GenericBarPainter<JXPanel>(
                new GradientPaint(0, 0, toolbarTopColor, 0, 1, toolbarBottomColor),
                toolbarBorderColor, PainterUtils.TRASPARENT,
                toolbarBorderColor, PainterUtils.TRASPARENT));
        
        toolbar.add(downloadlink);
        toolbar.add(sharelink);

        inputPanel = new ResizingInputPanel(writer, schedExecService);
        inputPanel.setBorder(BorderFactory.createEmptyBorder());
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);

        JTextComponent inputComponent = inputPanel.getInputComponent();
        FriendShareDropTarget friendShare = new FriendShareDropTarget(inputComponent, shareListManager.getOrCreateFriendShareList(chatFriend.getUser()));
        inputComponent.setDropTarget(friendShare.getDropTarget());

        return panel;
    }

    @Override
    public void handleDisplay() {
        invalidate();
        repaint();
        inputPanel.handleDisplay();
    }

    private class DownloadFromFriendLibraryAction extends AbstractAction {
        public DownloadFromFriendLibraryAction() {
            super("<html><u>" + tr("Browse Files") + "</u></html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            libraryNavigator.selectFriendLibrary(chatFriend.getUser());
        }
    }

    private class ShareAction extends AbstractAction {
        public ShareAction() {
            super("<html><u>" + tr("What I'm Sharing") + "</u></html>");
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            libraryNavigator.selectFriendShareList(chatFriend.getUser());
        }
    }

    public void offerFolder(ListeningFuture<List<ListeningFuture<LocalFileItem>>> future) {
        // TODO: Change this to show event immediately & update as status changes.
        future.addFutureListener(new EventListener<FutureEvent<List<ListeningFuture<LocalFileItem>>>>() {
            @Override
            public void handleEvent(FutureEvent<List<ListeningFuture<LocalFileItem>>> event) {
                if(event.getResult() != null) {
                    for(ListeningFuture<LocalFileItem> future : event.getResult()) {
                        offerFile(future);
                    }
                }
            }
        });
    }

    public void offerFile(ListeningFuture<LocalFileItem> future) {
        // TODO: Change this to show event immediately & update as status changes.
        future.addFutureListener(new EventListener<FutureEvent<LocalFileItem>>() {
            @SwingEDTEvent
            @Override
            public void handleEvent(FutureEvent<LocalFileItem> event) {
               if(event.getResult() != null) {
                   FileMetaData metadata = event.getResult().toMetadata();
                   boolean sentFileOffer = false;

                   // if active presence exists, send file offer to it,
                   // otherwise broadcast to every presence with FileOfferFeature.ID feature
                   User chatUser = chatFriend.getUser();
                   FriendPresence activePresence = chatUser.getActivePresence();
                   if ((activePresence != null) && activePresence.hasFeatures(FileOfferFeature.ID)) {
                        sentFileOffer = performFileOffer(metadata, activePresence);
                   } else {
                       for (FriendPresence presence : chatUser.getFriendPresences().values()) {
                            sentFileOffer |= performFileOffer(metadata, presence);
                       }
                   }

                   MessageFileOffer fileOfferMessage =
                           new MessageFileOfferImpl(loggedInID, friendId, Message.Type.Sent, metadata, null);

                   if (sentFileOffer) {
                        new MessageReceivedEvent(fileOfferMessage).publish();
                   } else {
                       // TODO: Devise how to handle file offer sending failures, using tooltip perhaps?
                   }
               }
            }

            private boolean performFileOffer(FileMetaData metadata, FriendPresence presence) {
                Feature fileOfferFeature = presence.getFeature(FileOfferFeature.ID);
                boolean fileOfferSent = false;
                if (fileOfferFeature != null) {
                    try {
                        writer.setChatState(ChatState.active);
                        FileOfferer fileOfferer = ((FileOfferFeature) fileOfferFeature).getFeature();
                        fileOfferer.offerFile(metadata);
                        fileOfferSent = true;
                    } catch (XMPPException e) {
                        LOG.debug("File offer failed", e);
                    }
                }
                return fileOfferSent;
            }
        });
    }
    
    private class FriendShareDropTarget implements DropTargetListener {
        private final DropTarget dropTarget;
        private LocalFileList fileList;
        
        public FriendShareDropTarget(JComponent component, LocalFileList fileList) {
            dropTarget = new DropTarget(component, DnDConstants.ACTION_COPY, this, true, null);
            this.fileList = fileList;
        }
        
        public void setModel(LocalFileList fileList) {
            this.fileList = fileList;
        }
        
        public DropTarget getDropTarget() {
            return dropTarget;
        }

        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
            checkLimewireConnected(dtde);
        }
        
        @Override
        public void dragExit(DropTargetEvent dte) {
        }
        
        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            checkLimewireConnected(dtde);
        }

        private void checkLimewireConnected(DropTargetDragEvent dtde) {
            if (!chatFriend.isSignedInToLimewire()) {
                dtde.rejectDrag();
            }
        }
        
        @Override
        public void drop(DropTargetDropEvent dtde) {
            if ((dtde.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0) {
                // Accept the drop and get the transfer data
                dtde.acceptDrop(dtde.getDropAction());
                Transferable transferable = dtde.getTransferable();

                try {
                    final LocalFileList currentModel = fileList;
                    final File[] droppedFiles = DNDUtils.getFiles(transferable); 
              
                    for(File file : droppedFiles) {
                        if(file != null) {
                            if(file.isDirectory()) {
                                acceptedFolder(currentModel.addFolder(file));
                            } else {
                                acceptedFile(currentModel.addFile(file));
                            }
                        }
                    }
                    
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    dtde.dropComplete(false);
                }
              } else {
                    dtde.rejectDrop();
              }
        }
        
        @Override
        public void dropActionChanged(DropTargetDragEvent dtde) {
        }
        
        protected void acceptedFile(ListeningFuture<LocalFileItem> future) {
            offerFile(future);
        }
        
        protected void acceptedFolder(ListeningFuture<List<ListeningFuture<LocalFileItem>>> future) {
            offerFolder(future);
        }
    }
}
