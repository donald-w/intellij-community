package com.intellij.debugger.ui.content.newUI;

import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

class GridCell {

  private Grid myContainer;

  private List<Content> myContents = new ArrayList<Content>();
  private JBTabs myTabs;
  private Grid.Placeholder myPlaceholder;
  private PlaceInGrid myPlaceInGrid;
  private ContentManager myContentManager;

  private Map<Content, TabInfo> myContent2Tab = new HashMap<Content, TabInfo>();

  public GridCell(ContentManager contentManager, Project project, Grid container, Grid.Placeholder placeholder, boolean horizontalToolbars, PlaceInGrid placeInGrid) {
    myContentManager = contentManager;
    myContainer = container;
    myPlaceInGrid = placeInGrid;
    myPlaceholder = placeholder;
    myTabs = new JBTabs(project, container.myActionManager, container);
    myTabs.setUiDecorator(new JBTabs.UiDecorator() {
      public JBTabs.UiDecoration getDecoration() {
        return new JBTabs.UiDecoration(null, new Insets(0, -1, 0, -1));
      }
    });
    myTabs.setSideComponentVertical(!horizontalToolbars);
    myTabs.setStealthTabMode(true);

  }

  public PlaceInGrid getPlaceInGrid() {
    return myPlaceInGrid;
  }

  void add(Content content) {
    if (myContents.contains(content)) return;
    myContents.add(content);

    revalidateCell();
  }

  void remove(Content content) {
    if (!myContents.contains(content)) return;
    myContents.remove(content);

    revalidateCell();
  }

  private void revalidateCell() {

    if (myContents.size() == 0) {
      myPlaceholder.removeAll();
    } else {
      if (myPlaceholder.isNull()) {
        myPlaceholder.setContent(myTabs);
      }

      myTabs.removeAllTabs();
      for (Content each : myContents) {
        myTabs.addTab(createTabInfoFor(each));
      }
    }

    restoreProportion();

    myTabs.revalidate();
    myTabs.repaint();
  }

  void setHideTabs(boolean hide) {
    myTabs.setHideTabs(hide);
  }

  private TabInfo createTabInfoFor(Content content) {
    final JComponent c = content.getComponent();

    NewDebuggerContentUI.removeScrollBorder(c);

    final TabInfo tabInfo = new TabInfo(c)
      .setIcon(content.getIcon())
      .setText(content.getDisplayName())
      .setActions(content.getActions(), content.getPlace())
      .setObject(content)
      .setPreferredFocusableComponent(content.getPreferredFocusableComponent());

    myContent2Tab.put(content, tabInfo);

    return tabInfo;
  }

  private TabInfo getTabInfoFor(Content content) {
    return myContent2Tab.get(content);
  }

  public void setToolbarHorizontal(final boolean horizontal) {
    myTabs.setSideComponentVertical(!horizontal);
  }

  public void restoreProportion() {
    myContainer.restoreProportion(myPlaceInGrid);
  }

  public void updateSelection(final boolean isShowing) {
    for (Content each : myContents) {
      final TabInfo eachTab = getTabInfoFor(each);
      if (myTabs.getSelectedInfo() == eachTab && isShowing) {
        myContentManager.addSelectedContent(each);
      } else {
        myContentManager.removeFromSelection(each);
      }
    }
  }
}
