package org.limewire.core.api.download;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.i18n.I18nMarker;
import org.limewire.io.Address;

/**
 * The interface for a single download.  Observed by all downloads: gnutella, bittorrent, or other.
 */
public interface DownloadItem extends PropertiableFile {	

    public static final String DOWNLOAD_ITEM = "limewire.download.glueItem";

    /**
     * Amalgamates the various specific error messages that a downloader could encounter
     *  into a general error applicable to any type of download and user under understandable.
     */
	public static enum ErrorState {
	    DISK_PROBLEM(I18nMarker.marktr("There is a disk problem")),
	    CORRUPT_FILE(I18nMarker.marktr("The file is corrupted")),
	    FILE_NOT_SHARABLE(I18nMarker.marktr("This file is not shareable")),
	    UNABLE_TO_CONNECT(I18nMarker.marktr("Trouble connecting to people")),
	    
	    /**
         * The DownloadItem is not in an error state.
         */
	    NONE(I18nMarker.marktr("No problems."));
	    
	    private final String message;

        private ErrorState(String message) {
            this.message = message;
        }
        
        /**
         * Returns error state message marked for translation.
         */
        public String getMessage() {
            return message;
        }
    };
    
    /**
     * Used to signify a "remainder" estimate that can not be calculated for 
     *  whatever reason.
     */
    public final static long UNKNOWN_TIME = Long.MAX_VALUE;

    /**
     * Adds a listener for state changes affecting getState().
     */
	public void addPropertyChangeListener(PropertyChangeListener listener);
	
	/**
     * Removes an existing listener.
     */
	public void removePropertyChangeListener(PropertyChangeListener listener);
	
	/**
	 * @return the current state of the download.
	 */
	public DownloadState getState();

	public String getTitle();

	/**
	 * 
	 * @return the percentage between 0 and 100.
	 */
	public int getPercentComplete();

	/**
	 * @return size in bytes
	 */
	public long getCurrentSize();
	
	/**
     * @return size in bytes
     */
	public long getTotalSize();

    /**
     * @return seconds remaining until the download will complete or <code>UNKNOWN</code> if unknown
     */
	public long getRemainingDownloadTime();
	
    /**
     * @return seconds remaining until the state will change or <code>UNKNOWN</code> if unknown
     */
    public long getRemainingTimeInState();

    /**
     * Initiates the cancel of this download item.
     */
	public void cancel();

	/**
	 * Pauses the download item if possible.
	 */
	public void pause();

	/**
	 * Resumes downloading of a paused item.
	 */
	public void resume();

	public int getDownloadSourceCount();

	public List<Address> getSources();
	
	public Category getCategory();

    /**
     * @return speed in kb/s or 0 if speed could not be measured
     */
	public float getDownloadSpeed();

	/**
     * Returns the position of the download on the uploader, relevant only if
     * the downloader is remote queued.
     */
    public int getRemoteQueuePosition();
  
    public ErrorState getErrorState();
    
    public int getLocalQueuePriority();
    
    /**
     * @return if this downloader can be launched or previewed.
     */
    public boolean isLaunchable();
    
    /**
     * Returns the File that is being downloaded to.
     * This call never blocks, but may return a file
     * that is locked and cannot be used by other programs.
     */
    File getDownloadingFile();
    
    /**
     * Returns a file suitable for launching.
     * This call may block while the file is created.
     */
    File getLaunchableFile();
    
    /**
     * Sets the destination path and file name for the download.
     */
    void setSaveFile(File saveFile, boolean overwrite) throws SaveLocationException;

    /** Returns true if {@link #resume()} will search for more sources. */
    boolean isSearchAgainEnabled(); 
}