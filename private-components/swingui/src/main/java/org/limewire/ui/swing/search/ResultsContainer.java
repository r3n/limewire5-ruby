package org.limewire.ui.swing.search;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Action;
import javax.swing.Scrollable;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.resultpanel.BaseResultPanel;
import org.limewire.ui.swing.search.resultpanel.BaseResultPanelFactory;
import org.limewire.ui.swing.settings.SwingUiSettings;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * ResultsContainer is a display panel that contains the search results tables 
 * for all media categories.
 * 
 * @see org.limewire.ui.swing.search.SearchResultsPanel
 */
public class ResultsContainer extends JXPanel {

    /** Results panel containing display tables. */
    private final BaseResultPanel baseResultPanel;

    /** Data model containing search results. */
    private final SearchResultsModel searchResultsModel;
    
    /** Current view type; either LIST or TABLE. */
    private SearchViewType mode = SearchViewType.forId(SwingUiSettings.SEARCH_VIEW_TYPE_ID.getValue());

    /**
     * Constructs a ResultsContainer with the specified search results data
     * model and factories.
     * @see org.limewire.ui.swing.search.ResultsContainerFactory
     */
    @AssistedInject
    ResultsContainer(
        @Assisted SearchResultsModel searchResultsModel,
        BaseResultPanelFactory baseFactory) {
        
        this.searchResultsModel = searchResultsModel;
        
        // Create result panel.
        baseResultPanel = baseFactory.create(searchResultsModel);
        
        setLayout(new BorderLayout());
        
        // Add result panel to the container.
        add(baseResultPanel, BorderLayout.CENTER);
    }

    /**
     * Installs a listener on the list of search results to update the result
     * count for the specified search category and tab action.  The result
     * count is displayed in parentheses next to the category name.
     */
    public void synchronizeResultCount(SearchCategory key, final Action action) {
        // Adds itself as a listener to the list & keeps the action in sync.
        new SourceCountMaintainer(searchResultsModel.getCategorySearchResults(key), action);
    }
    
    /**
     * Changes whether the list view or table view is displayed.
     * @param mode LIST or TABLE
     */
    public void setViewType(SearchViewType mode) {
        this.mode = mode;
        if (baseResultPanel != null) {
            baseResultPanel.setViewType(mode);
        }
    }
    
    /**
     * Displays the search results tables for the specified search category.
     */
    public void showCategory(SearchCategory category) {
        baseResultPanel.showCategory(category);
        baseResultPanel.setViewType(mode);
    }

    /**
     * Returns the header component for the category results currently 
     * displayed.  The method returns null if no header is displayed.
     */
    public Component getScrollPaneHeader() {
        return baseResultPanel.getScrollPaneHeader();
    }

    /**
     * Returns the results view component currently being displayed. 
     */
    public Scrollable getScrollable() {
        return baseResultPanel.getScrollable();
    }
}
