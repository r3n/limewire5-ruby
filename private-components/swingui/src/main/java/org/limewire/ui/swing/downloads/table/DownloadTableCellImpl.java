package org.limewire.ui.swing.downloads.table;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeProgressBar;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

import com.google.inject.assistedinject.AssistedInject;

public class DownloadTableCellImpl extends JXPanel implements DownloadTableCell {

    private final CategoryIconManager categoryIconManager;
    private final ProgressBarDecorator progressBarDecorator;
    
    private CardLayout statusViewLayout;
    private final static String FULL_LAYOUT = "Full download display";
    private JPanel fullPanel;
    private static String MIN_LAYOUT = "Condensed download display";
    private JPanel minPanel;

    private DownloadButtonPanel minButtonPanel;
    private JLabel minIconLabel;
    private LabelContainer minTitleLabel;
    private JLabel minStatusLabel;
    private JXHyperlink minLinkButton;
    
    private DownloadButtonPanel fullButtonPanel;
    private JLabel fullIconLabel;
    private LabelContainer fullTitleLabel;
    private JLabel fullStatusLabel;
    private LimeProgressBar fullProgressBar;
    private JLabel fullTimeLabel;
    
    private JLabel removeLinkSpacer;
    private HyperlinkButton cancelLink;
    private HyperlinkButton launchButton;
   
    @Resource private Icon warningIcon;
    @Resource private int progressBarWidth;
    @Resource private Color titleLabelColour;
    @Resource private Color statusLabelColour;
    @Resource private Color warningLabelColour;
    @Resource private Color errorLabelColour;
    @Resource private Color finishedLabelColour;
    @Resource private Color linkColour;
    @Resource private Font statusFontPlainMin;
    @Resource private Font statusFontPlainFull;
    @Resource private Font titleFont;
    @Resource private Color borderPaint;
        
    private ActionListener editorListener = null;
    
    @AssistedInject
    public DownloadTableCellImpl(CategoryIconManager categoryIconManager,
            ProgressBarDecorator progressBarDecorator) {
        
        GuiUtils.assignResources(this);

        this.categoryIconManager = categoryIconManager;
        this.progressBarDecorator = progressBarDecorator;
        
        initComponents();
    }
    
    public void setEditorListener(ActionListener editorListener) {
        this.editorListener = editorListener;
        this.minButtonPanel.setActionListener(editorListener);
        this.fullButtonPanel.setActionListener(editorListener);
        this.minLinkButton.addActionListener(editorListener);
        this.cancelLink.addActionListener(editorListener);
        this.launchButton.addActionListener(editorListener);
    }
    
    public void update(DownloadItem item) {
        updateComponent(item.getState(), item);
    }

    private void initComponents() {
        
        this.setBackgroundPainter(this.createCellPainter());        
        
        statusViewLayout = new CardLayout();
        this.setLayout(statusViewLayout);
        
        fullPanel = new JPanel(new GridBagLayout());
        minPanel  = new JPanel(new GridBagLayout());
        
        fullPanel.setOpaque(false);
        minPanel.setOpaque(false);

        Border blankBorder = BorderFactory.createEmptyBorder(0,0,0,0);
        fullPanel.setBorder(blankBorder);
        minPanel.setBorder(blankBorder);
        this.setBorder(blankBorder);
        
        this.add(fullPanel, FULL_LAYOUT);
        this.add( minPanel, MIN_LAYOUT);
        this.statusViewLayout.show(this, FULL_LAYOUT);
        
        
        minIconLabel = new JLabel();
        
        minTitleLabel = new LabelContainer();

        minStatusLabel = new JLabel();
        minStatusLabel.setFont(statusFontPlainMin);
        minStatusLabel.setForeground(statusLabelColour);

        minButtonPanel = new DownloadButtonPanel(editorListener);
        minButtonPanel.setOpaque(false);

        minLinkButton = new JXHyperlink();
        minLinkButton.addActionListener(editorListener);
        minLinkButton.setForeground(linkColour);
        minLinkButton.setClickedColor(linkColour);
        minLinkButton.setFont(statusFontPlainMin);
                                
        fullIconLabel = new JLabel();

        fullTitleLabel = new LabelContainer();

        fullStatusLabel = new JLabel();
        fullStatusLabel.setFont(statusFontPlainFull);
        fullStatusLabel.setForeground(statusLabelColour);
        
        fullProgressBar = new LimeProgressBar(0, 100);
        progressBarDecorator.decoratePlain(fullProgressBar);        
        Dimension size = new Dimension(progressBarWidth, 16);
        fullProgressBar.setMaximumSize(size);
        fullProgressBar.setMinimumSize(size);
        fullProgressBar.setPreferredSize(size);
        
        fullTimeLabel = new JLabel();
        fullTimeLabel.setFont(statusFontPlainFull);
        
        fullButtonPanel = new DownloadButtonPanel(editorListener);
        fullButtonPanel.setOpaque(false);        

        cancelLink = new HyperlinkButton();
        cancelLink.setText(I18n.tr("Remove"));
        cancelLink.setFont(statusFontPlainMin);
        cancelLink.setActionCommand(DownloadActionHandler.CANCEL_COMMAND);
        //FontUtils.bold(cancelLink);
        FontUtils.underline(cancelLink); 
        
        launchButton = new HyperlinkButton();
        launchButton.setText(I18n.tr("Launch"));
        launchButton.setFont(statusFontPlainMin);
        launchButton.setActionCommand(DownloadActionHandler.LAUNCH_COMMAND);
        //FontUtils.bold(launchButton);
        FontUtils.underline(launchButton); 
        
        removeLinkSpacer = new JLabel("- ");
        removeLinkSpacer.setMinimumSize(new Dimension(0,cancelLink.getPreferredSize().height));
        removeLinkSpacer.setFont(statusFontPlainMin);
        removeLinkSpacer.setForeground(errorLabelColour);
        
        createFullView();
        createMinView();
    }
    
    private void createMinView() {
        //this is a bit hacky but easier than messing with the gridbag
        JPanel removePanel = new JPanel();
        removePanel.setOpaque(false);
        removePanel.add(removeLinkSpacer);
        removePanel.add(cancelLink);
        removePanel.add(launchButton);
        
        GridBagConstraints gbc = new GridBagConstraints();

        Insets insets = new Insets(0,10,0,0);
        
        gbc.insets = insets;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridheight = 5;
        minPanel.add(minIconLabel, gbc);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        minPanel.add(minTitleLabel, gbc);
        
        gbc.insets = new Insets(5,4,0,0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 5;
        minPanel.add(minButtonPanel, gbc);  
        
        gbc.insets = insets;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 0;
        minPanel.add(minStatusLabel, gbc);
       
        gbc.insets = new Insets(0,0,0,0);
        gbc.gridx++;
        minPanel.add(minLinkButton, gbc);

        gbc.gridx++;
        minPanel.add(removePanel, gbc);
        
        //puts the pause button in the right place for the connecting state
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 3;
        gbc.gridheight = 0;
        minPanel.add(Box.createHorizontalStrut(progressBarWidth-16), gbc);
            
    }
    
    private void createFullView() {
        GridBagConstraints gbc = new GridBagConstraints();

        Insets insets = new Insets(0,10,0,0);
        
        gbc.insets = insets;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        fullPanel.add(fullIconLabel, gbc);
        
        gbc.insets = new Insets(0,5,0,0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        fullPanel.add(fullTitleLabel, gbc);
        
        gbc.insets = new Insets(2,10,0,0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 3;
        fullPanel.add(fullProgressBar, gbc);
        
        gbc.insets = new Insets(5,4,0,0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx += gbc.gridwidth;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 3;
        fullPanel.add(fullButtonPanel, gbc);  
        
        gbc.insets = insets;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        fullPanel.add(fullStatusLabel, gbc);
        
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        fullPanel.add(fullTimeLabel, gbc);    
    }
    
    private void updateMin(DownloadState state, DownloadItem item) {
        
        minTitleLabel.setText(item.getTitle());
        
        switch (state) {
        
        
        case ERROR :

            minIconLabel.setIcon(warningIcon);
            minStatusLabel.setForeground(errorLabelColour);
            minStatusLabel.setFont(statusFontPlainMin);
            
            break;
        
        case STALLED :
            
            //minIconLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));
            minIconLabel.setIcon(warningIcon);
            minStatusLabel.setForeground(warningLabelColour);
            minStatusLabel.setFont(statusFontPlainMin);
            
            break;
            
        case FINISHING :
        case DONE :
            
            minIconLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));
            minStatusLabel.setForeground(finishedLabelColour);
            minStatusLabel.setFont(statusFontPlainMin);
            
            break;
            
        default :
            minIconLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));     
            minStatusLabel.setForeground(statusLabelColour);
            minStatusLabel.setFont(statusFontPlainMin);
            
        }
        
        minStatusLabel.setText(getMessage(state, item));        
        
        updateButtonsMin(item);      
    }
    
    
    
    private void updateFull(DownloadState state, DownloadItem item) {
        
        fullIconLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));
        fullTitleLabel.setText(item.getTitle());
        fullTimeLabel.setForeground(statusLabelColour);
        fullTimeLabel.setFont(statusFontPlainFull);
        
        fullProgressBar.setValue(item.getPercentComplete());

        fullProgressBar.setEnabled(item.getState() != DownloadState.PAUSED);
        
        fullStatusLabel.setText(getMessage(state, item));
        
        if (item.getRemainingDownloadTime() > Long.MAX_VALUE-1000) {
            fullTimeLabel.setVisible(false);
        }
        else {
            fullTimeLabel.setText(I18n.tr("{0} left", CommonUtils.seconds2time(item.getRemainingDownloadTime())));
            fullTimeLabel.setVisible(item.getState() == DownloadState.DOWNLOADING);
        }
                 
        updateButtonsFull(item);      
    }
    

    private void updateButtonsMin(DownloadItem item) {
        DownloadState state = item.getState();
        minButtonPanel.updateButtons(state);
        
        switch (state) {        
            case ERROR :
                minLinkButton.setVisible(true);
                minLinkButton.setActionCommand(DownloadActionHandler.LINK_COMMAND);
                //underline hidden  & color changed till link is active
                minLinkButton.setText(I18n.tr(item.getErrorState().getMessage()));
               //  minLinkButton.setText("<html><u>" + I18n.tr(item.getErrorState().getMessage()) + "</u></html>");
                // TODO remove color and rollover settings once error link is active
                minLinkButton.setRolloverEnabled(false);
                minLinkButton.setForeground(errorLabelColour);
                minLinkButton.setClickedColor(errorLabelColour);
                break;
                
            case STALLED :
                minLinkButton.setVisible(true);
                minLinkButton.setActionCommand(DownloadActionHandler.TRY_AGAIN_COMMAND);
                if(item.isSearchAgainEnabled()) {
                    minLinkButton.setText("<html><u>" + I18n.tr("Search Again") + "</u></html>");
                } else {
                    minLinkButton.setText("<html><u>" + I18n.tr("Try Again") + "</u></html>");
                }
                // TODO remove color and rollover settings once error link is active
                minLinkButton.setForeground(linkColour);
                minLinkButton.setClickedColor(linkColour);
                minLinkButton.setRolloverEnabled(true);
                break;
                
            default:
                minLinkButton.setVisible(false);
        }

        launchButton.setVisible(item.isLaunchable() && item.getState() == DownloadState.DONE);

        cancelLink.setVisible(item.getState() == DownloadState.ERROR);
        removeLinkSpacer.setVisible(cancelLink.isVisible());
    }
    
    private void updateButtonsFull(DownloadItem item) {
        DownloadState state = item.getState();
        
        fullButtonPanel.updateButtons(state);
    }

    private void updateComponent(DownloadState state, DownloadItem item){
        if(item == null) { // can be null because of accessibility calls.
            return;
        }
        
        switch(state) {
            case DOWNLOADING:
            case PAUSED:
                statusViewLayout.show(this, FULL_LAYOUT);
                updateFull(state, item);
                break;
            default:
                statusViewLayout.show(this, MIN_LAYOUT);
                updateMin(state, item);
        }
    }
    
    private Painter<JXPanel> createCellPainter() {
        AbstractPainter<JXPanel> painter = new AbstractPainter<JXPanel>() {

            @Override
            protected void doPaint(Graphics2D g, JXPanel object, int width, int height) {
                g.setPaint(borderPaint);
                g.drawLine(0, height-1, width-0, height-1);
            }
        } ;
        
        painter.setCacheable(true);
        painter.setAntialiasing(false);
        //painter.setFilters(PainterUtils.createSoftenFilter(0.025f));
                        
        return painter;
    }
    
    private String getMessage(DownloadState state, DownloadItem item) {
        switch (state) {
        case RESUMING:
            return I18n.tr("Resuming at {0}%",
                    item.getPercentComplete());
        case CANCELLED:
            return I18n.tr("Cancelled");
        case FINISHING:
            return I18n.tr("Finishing download...");
        case DONE:
            return I18n.tr("Done - ");
        case CONNECTING:
            return I18n.tr("Connecting...");
        case DOWNLOADING:
            // {0}: current size
            // {1}: total size
            // {2}: download speed
            // {3}: number of people
            return I18n.trn("Downloading {0} of {1} ({2}) from {3} person",
                    "Downloading {0} of {1} ({2}) from {3} people",
                    item.getDownloadSourceCount(),
                    GuiUtils.toUnitbytes(item.getCurrentSize()), 
                    GuiUtils.toUnitbytes(item.getTotalSize()),
                    GuiUtils.rate2speed(item.getDownloadSpeed()), 
                    item.getDownloadSourceCount());
        case TRYING_AGAIN:
            return getTryAgainMessage(item.getRemainingTimeInState());
        case STALLED:
            return I18n.tr("Stalled - {0} of {1} ({2}%). - ", 
                    GuiUtils.toUnitbytes(item.getCurrentSize()),
                    GuiUtils.toUnitbytes(item.getTotalSize()),
                    item.getPercentComplete()
                    );
        case ERROR:         
            return I18n.tr("Unable to download: ");
        case PAUSED:
            // {0}: current size, {1} total size, {2} percent complete
            return I18n.tr("Paused - {0} of {1} ({2}%)", 
                    GuiUtils.toUnitbytes(item.getCurrentSize()), GuiUtils.toUnitbytes(item.getTotalSize()),
                    item.getPercentComplete());
        case LOCAL_QUEUED:
            return getQueueTimeMessage(item.getRemainingTimeInState());
        case REMOTE_QUEUED:
            if(item.getRemoteQueuePosition() == -1 || item.getRemoteQueuePosition() == Integer.MAX_VALUE){
                return getQueueTimeMessage(item.getRemainingTimeInState());
            }
            return I18n.trn("Waiting - Next in line",
                    "Waiting - {0} in line",
                    item.getRemoteQueuePosition(), item.getRemoteQueuePosition());
        default:
            return null;
        }
        
    }
    
    private String getTryAgainMessage(long tryingAgainTime) {
        if(tryingAgainTime == DownloadItem.UNKNOWN_TIME){
            return I18n.tr("Searching for people with this file...");                
        } else {
            return I18n.tr("Searching for people with this file... ({0} left)", CommonUtils.seconds2time(tryingAgainTime));
        }
    }
    
    private String getQueueTimeMessage(long queueTime){
        if(queueTime == DownloadItem.UNKNOWN_TIME){
            return I18n.tr("Waiting - remaining time unknown");                
        } else {
            return I18n.tr("Waiting - Starting in {0}", CommonUtils.seconds2time(queueTime));
        }
    }

    @Override
    public Component getComponent() {
        return this;
    }

    /**
     * Class to make trimming the title length inside the current GridBagLayout possible 
     */
    private class LabelContainer extends JPanel {
        private final JLabel label = new JLabel();
        
        public LabelContainer() {
            this.setLayout(new BorderLayout());
            this.setOpaque(false);
            this.setBorder(BorderFactory.createEmptyBorder());
            
            this.label.setFont(titleFont);
            this.label.setForeground(titleLabelColour);
            this.label.setMaximumSize(new Dimension(progressBarWidth-30, 20));
            this.label.setPreferredSize(new Dimension(progressBarWidth-30, 20));
            
            this.add(this.label, BorderLayout.WEST);
        }
        
        public void setText(String text) {
            this.label.setText(text);
        }
    }
}
